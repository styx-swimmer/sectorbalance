"""
Main pipeline entrypoint for Sector Rebalancing Platform.
Executes daily workflow:
  1. Fetch latest 11 SPDR Sector prices and SPY benchmark
  2. Compute 3-state HMM market regime
  3. Load existing portfolio share positions from Firestore
  4. Execute Shannon's Demon drift analysis and generate rebalance trade orders
  5. Compute historical performance and Rebalancing Premium
  6. Sync updates to Google Cloud Firestore
"""
import sys
import os
import argparse
from datetime import datetime

# Add current dir to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from data_fetcher import MarketDataFetcher
from hmm_regime import GaussianHMMDetector
from rebalance_engine import RebalanceEngine
from performance_tracker import PerformanceTracker
from firestore_sync import FirestoreSync


def run_daily_pipeline(
    risk_profile: str = "SHOCK_ABSORBER",
    capital: float = 100000.0,
    reset_to_equal_weight: bool = False,
) -> dict:
    now_str = datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S UTC")
    print(f"[{now_str}] Starting Sector Daily Pipeline...")

    # 1. Fetch Market Data
    fetcher = MarketDataFetcher()
    print("-> Fetching latest sector prices...")
    prices_data = fetcher.fetch_latest_prices()
    prices_map = {k: v["price"] for k, v in prices_data.items()}

    # 2. HMM Market Regime Detection
    print("-> Running 3-state HMM Market Regime Detector...")
    returns, vols = fetcher.fetch_historical_series(lookback_days=252)
    detector = GaussianHMMDetector()
    regime_result = detector.fit_and_predict(returns, vols)
    print(f"   Daily Regime: [{regime_result['title']}] (Confidence: {regime_result['confidence']}%)")

    # 3. Load Existing Portfolio Holdings from Firestore
    sync_client = FirestoreSync()
    existing_state = sync_client.get_portfolio_state()
    existing_holdings = None
    if existing_state and not reset_to_equal_weight:
        existing_holdings = existing_state.get("holdings")
        if existing_holdings:
            print("   Loaded persistent share holdings from Firestore.")

    # 4. Rebalance Engine Drift Analysis
    print(f"-> Calculating Shannon's Demon 11-sector drift ({risk_profile})...")
    engine = RebalanceEngine(risk_profile_key=risk_profile)
    portfolio_state = engine.analyze_portfolio(
        prices=prices_map,
        holdings=existing_holdings,
        total_capital=capital,
        reset_to_equal_weight=reset_to_equal_weight,
    )
    print(f"   Status: [{portfolio_state['status']}] (Max Drift: {portfolio_state['max_drift_pct']}%)")
    if portfolio_state["rebalance_needed"]:
        print(f"   Rebalance Triggered! Breached sectors: {portfolio_state['breached_sectors']}")

    # 5. Performance & Rebalancing Premium
    print("-> Computing historical Rebalancing Premium...")
    perf_tracker = PerformanceTracker()
    performance_data = perf_tracker.generate_performance_curves(days=180)
    print(f"   Shannon's Demon Alpha: +{performance_data['rebalance_alpha_bps']} bps (+{performance_data['rebalance_alpha_pct']}%)")

    # 6. Sync to Firestore
    print("-> Syncing to Google Cloud Firestore...")
    sync_client.sync_all(
        regime_data=regime_result,
        sectors_data=prices_data,
        portfolio_state=portfolio_state,
        performance_data=performance_data,
    )

    print("Pipeline completed successfully!")
    return {
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "market_regime": regime_result,
        "sectors_current": prices_data,
        "portfolio_state": portfolio_state,
        "performance_history": performance_data,
    }


def cloud_function_entrypoint(request):
    """
    HTTP handler for Google Cloud Functions / Cloud Run invocation.
    Triggered daily via Google Cloud Scheduler.
    """
    try:
        req_json = request.get_json(silent=True) or {}
        risk_profile = req_json.get("risk_profile", "SHOCK_ABSORBER")
        capital = float(req_json.get("capital", 100000.0))
        reset_rebalance = bool(req_json.get("reset", False))
        result = run_daily_pipeline(
            risk_profile=risk_profile,
            capital=capital,
            reset_to_equal_weight=reset_rebalance,
        )
        return ({"status": "success", "result": result}, 200, {"Content-Type": "application/json"})
    except Exception as e:
        return ({"status": "error", "message": str(e)}, 500, {"Content-Type": "application/json"})


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Sector Rebalance Daily Pipeline")
    parser.add_argument("--cli", action="store_true", help="Run once as CLI batch job and exit")
    parser.add_argument("--profile", default="SHOCK_ABSORBER", choices=["SHOCK_ABSORBER", "WEALTH_COMPOUNDER"])
    parser.add_argument("--capital", type=float, default=100000.0)
    parser.add_argument("--reset", action="store_true", help="Reset portfolio to equal weight")
    args = parser.parse_args()

    if args.cli:
        run_daily_pipeline(risk_profile=args.profile, capital=args.capital, reset_to_equal_weight=args.reset)
    else:
        if "PORT" in os.environ:
            print("PORT detected in environment. Starting HTTP web server...")
            from server import start_server
            start_server()
        else:
            run_daily_pipeline(risk_profile=args.profile, capital=args.capital, reset_to_equal_weight=args.reset)
