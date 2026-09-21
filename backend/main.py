"""
Main pipeline entrypoint for Sector Rebalancing Platform.
Executes daily workflow:
  1. Fetch latest 11 SPDR Sector prices and SPY benchmark
  2. Compute 3-state HMM market regime
  3. Execute Shannon's Demon drift analysis and generate rebalance trade orders
  4. Compute historical performance and Rebalancing Premium
  5. Sync updates to Google Cloud Firestore
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


def run_daily_pipeline(risk_profile: str = "SHOCK_ABSORBER", capital: float = 100000.0) -> dict:
    print(f"[{datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S UTC')}] Starting Sector Daily Pipeline...")

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

    # 3. Rebalance Engine
    print(f"-> Calculating Shannon's Demon 11-sector drift ({risk_profile})...")
    engine = RebalanceEngine(risk_profile_key=risk_profile)
    portfolio_state = engine.analyze_portfolio(prices=prices_map, total_capital=capital)
    print(f"   Status: [{portfolio_state['status']}] (Max Drift: {portfolio_state['max_drift_pct']}%)")
    if portfolio_state["rebalance_needed"]:
        print(f"   Rebalance Triggered! Breached sectors: {portfolio_state['breached_sectors']}")

    # 4. Performance & Rebalancing Premium
    print("-> Computing historical Rebalancing Premium...")
    perf_tracker = PerformanceTracker()
    performance_data = perf_tracker.generate_performance_curves(days=180)
    print(f"   Shannon's Demon Alpha: +{performance_data['rebalance_alpha_bps']} bps (+{performance_data['rebalance_alpha_pct']}%)")

    # 5. Sync to Firestore
    print("-> Syncing to Google Cloud Firestore...")
    sync_client = FirestoreSync()
    sync_client.sync_all(
        regime_data=regime_result,
        sectors_data=prices_data,
        portfolio_state=portfolio_state,
        performance_data=performance_data,
    )

    print("Pipeline completed successfully!")
    return {
        "regime": regime_result,
        "portfolio_state": portfolio_state,
        "performance": performance_data,
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
        result = run_daily_pipeline(risk_profile=risk_profile, capital=capital)
        return ({"status": "success", "result": result}, 200, {"Content-Type": "application/json"})
    except Exception as e:
        return ({"status": "error", "message": str(e)}, 500, {"Content-Type": "application/json"})


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Sector Rebalance Daily Pipeline")
    parser.add_argument("--profile", default="SHOCK_ABSORBER", choices=["SHOCK_ABSORBER", "WEALTH_COMPOUNDER"])
    parser.add_argument("--capital", type=float, default=100000.0)
    args = parser.parse_args()

    run_daily_pipeline(risk_profile=args.profile, capital=args.capital)
