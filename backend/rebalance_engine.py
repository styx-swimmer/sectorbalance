"""
Rebalancing Engine: Shannon's Demon Volatility Harvesting for 11 SPDR Sectors.
Calculates portfolio drift, evaluates 5% and 10% tolerance bands, and generates
rebalance trade orders with execution friction modeling.
"""
from typing import Dict, List, Any, Optional
from config import (
    SECTOR_TICKERS,
    SECTOR_NAMES,
    TARGET_WEIGHT,
    NUM_SECTORS,
    RISK_PROFILES,
    ONE_WAY_FEE_BPS,
)


class RebalanceEngine:
    def __init__(self, risk_profile_key: str = "SHOCK_ABSORBER"):
        self.risk_profile_key = risk_profile_key
        self.risk_profile = RISK_PROFILES.get(
            risk_profile_key, RISK_PROFILES["SHOCK_ABSORBER"]
        )
        self.target_weight = TARGET_WEIGHT
        self.drift_threshold = self.risk_profile["drift_threshold"]

    def analyze_portfolio(
        self,
        prices: Dict[str, float],
        holdings: Optional[Dict[str, float]] = None,
        total_capital: float = 100000.0,
    ) -> Dict[str, Any]:
        """
        Analyzes the current 11-sector portfolio:
          - If holdings (shares) provided, computes current value & weights.
          - If holdings is None, assumes base portfolio scaled to total_capital.
          - Calculates drift per sector vs target weight (9.0909%).
          - Identifies breaches of drift bands (5% or 10%).
          - Generates rebalance orders to restore equal weighting.
        """
        # 1. Determine position values
        position_values = {}
        shares = {}

        if holdings:
            for ticker in SECTOR_TICKERS:
                price = prices.get(ticker, 100.0)
                sh = holdings.get(ticker, 0.0)
                shares[ticker] = sh
                position_values[ticker] = sh * price
            total_value = sum(position_values.values())
            if total_value <= 0:
                total_value = total_capital
        else:
            # Default simulated allocation if no live custom holdings passed
            total_value = total_capital
            # Distribute with slight organic drift if none provided
            for ticker in SECTOR_TICKERS:
                price = prices.get(ticker, 100.0)
                target_val = total_value * self.target_weight
                shares[ticker] = round(target_val / price, 2)
                position_values[ticker] = shares[ticker] * price
            total_value = sum(position_values.values())

        # 2. Calculate current weights and drift
        sectors_analysis = []
        max_abs_drift = 0.0
        breached_sectors = []

        for ticker in SECTOR_TICKERS:
            price = prices.get(ticker, 100.0)
            val = position_values.get(ticker, 0.0)
            weight = (val / total_value) if total_value > 0 else 0.0
            drift = weight - self.target_weight  # positive = overweight, negative = underweight
            abs_drift = abs(drift)

            if abs_drift > max_abs_drift:
                max_abs_drift = abs_drift

            is_breached = abs_drift >= self.drift_threshold
            if is_breached:
                breached_sectors.append(ticker)

            status = "BALANCED"
            if is_breached:
                status = "REBALANCE_TRIGGERED"
            elif abs_drift >= (self.drift_threshold - 0.015):
                status = "WATCH"

            sectors_analysis.append(
                {
                    "ticker": ticker,
                    "name": SECTOR_NAMES.get(ticker, ticker),
                    "price": round(price, 2),
                    "shares": round(shares.get(ticker, 0.0), 2),
                    "value": round(val, 2),
                    "current_weight": round(weight * 100, 2),
                    "target_weight": round(self.target_weight * 100, 2),
                    "drift_pct": round(drift * 100, 2),
                    "abs_drift_pct": round(abs_drift * 100, 2),
                    "status": status,
                    "is_breached": is_breached,
                }
            )

        # 3. Overall Portfolio Status
        rebalance_needed = len(breached_sectors) > 0
        overall_status = "REBALANCE_TRIGGERED" if rebalance_needed else ("WATCH" if max_abs_drift >= (self.drift_threshold - 0.015) else "BALANCED")

        # 4. Generate Rebalance Orders
        orders = []
        total_turnover_dollars = 0.0

        for item in sectors_analysis:
            ticker = item["ticker"]
            price = item["price"]
            curr_val = item["value"]
            curr_shares = item["shares"]
            target_val = total_value * self.target_weight
            dollar_diff = target_val - curr_val  # positive = BUY, negative = SELL
            shares_diff = (dollar_diff / price) if price > 0 else 0.0

            action = "HOLD"
            if rebalance_needed and abs(dollar_diff) > 10.0:
                action = "BUY" if dollar_diff > 0 else "SELL"
                total_turnover_dollars += abs(dollar_diff)

            orders.append(
                {
                    "ticker": ticker,
                    "name": item["name"],
                    "action": action,
                    "current_shares": curr_shares,
                    "target_shares": round(target_val / price, 2) if price > 0 else 0.0,
                    "share_change": round(abs(shares_diff), 2),
                    "current_value": curr_val,
                    "target_value": round(target_val, 2),
                    "dollar_amount": round(abs(dollar_diff), 2),
                    "is_trigger_source": ticker in breached_sectors,
                }
            )

        # Estimated one-way trading fee / turnover friction
        estimated_friction = total_turnover_dollars * ONE_WAY_FEE_BPS

        # 5. Educational Rationale
        rationale = self._generate_rationale(
            rebalance_needed, breached_sectors, max_abs_drift, sectors_analysis
        )

        return {
            "portfolio_value": round(total_value, 2),
            "target_per_sector": round(self.target_weight * 100, 2),
            "risk_profile": self.risk_profile["name"],
            "drift_threshold_pct": round(self.drift_threshold * 100, 1),
            "max_drift_pct": round(max_abs_drift * 100, 2),
            "status": overall_status,
            "rebalance_needed": rebalance_needed,
            "breached_sectors": breached_sectors,
            "sectors": sectors_analysis,
            "orders": orders,
            "turnover_dollars": round(total_turnover_dollars, 2),
            "turnover_pct": round((total_turnover_dollars / total_value * 100), 2) if total_value > 0 else 0.0,
            "estimated_fee_dollars": round(estimated_friction, 2),
            "educational_rationale": rationale,
        }

    def _generate_rationale(
        self,
        rebalance_needed: bool,
        breached_sectors: List[str],
        max_drift: float,
        sectors: List[Dict[str, Any]],
    ) -> str:
        if not rebalance_needed:
            return (
                f"Portfolio is within disciplined tolerance. Max drift is currently "
                f"{round(max_drift * 100, 2)}% (below the {round(self.drift_threshold * 100, 1)}% band). "
                f"No action required; letting compounders run while avoiding unnecessary turnover fees."
            )

        overweight = [s for s in sectors if s["ticker"] in breached_sectors and s["drift_pct"] > 0]
        underweight = [s for s in sectors if s["ticker"] in breached_sectors and s["drift_pct"] < 0]

        top_winner = max(sectors, key=lambda s: s["drift_pct"])
        top_laggard = min(sectors, key=lambda s: s["drift_pct"])

        win_msg = f"{top_winner['ticker']} ({top_winner['name']}) has expanded to {top_winner['current_weight']}% (+{top_winner['drift_pct']}% drift)"
        lag_msg = f"{top_laggard['ticker']} ({top_laggard['name']}) has contracted to {top_laggard['current_weight']}% ({top_laggard['drift_pct']}% drift)"

        return (
            f"Shannon's Demon Rebalance Triggered! {win_msg}, breaching the {round(self.drift_threshold * 100, 1)}% threshold. "
            f"Rebalancing systematically locks in gains from outperforming sectors to acquire undervalued laggards (like {lag_msg}) "
            f"without predicting market direction, capturing the structural volatility premium."
        )
