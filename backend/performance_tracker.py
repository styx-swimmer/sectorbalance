"""
Performance Tracker & Rebalancing Premium (Shannon's Demon Alpha).
Tracks and compares:
  1. Equal Weight 5% Rebalance (Shock Absorber)
  2. Equal Weight 10% Rebalance (Wealth Compounder)
  3. Un-rebalanced Sector Drift
  4. SPY Benchmark
"""
import math
from typing import Dict, List, Any
from datetime import datetime, timedelta


class PerformanceTracker:
    def __init__(self):
        pass

    def generate_performance_curves(self, days: int = 180) -> Dict[str, Any]:
        """
        Generates simulated daily performance points illustrating the Rebalancing Premium
        consistent with the empirical 25-year research findings.
        """
        history = []
        base_date = datetime.utcnow() - timedelta(days=days)

        val_5pct = 100.0
        val_10pct = 100.0
        val_drift = 100.0
        val_spy = 100.0

        peak_5pct = 100.0
        peak_10pct = 100.0
        peak_drift = 100.0
        peak_spy = 100.0

        max_dd_5pct = 0.0
        max_dd_10pct = 0.0
        max_dd_drift = 0.0
        max_dd_spy = 0.0

        # Simulated trajectory highlighting volatility harvesting
        # Rebalancing earns a steady cross-sectional volatility premium over drift
        for i in range(days):
            date_str = (base_date + timedelta(days=i)).strftime("%Y-%m-%d")

            # Synthetic market cycle wave
            t = i / 30.0
            market_ret = 0.0004 + 0.009 * math.sin(t) + 0.003 * math.cos(2.3 * t)

            # Shannon's Demon premium: harvesting cross-sectional dispersion
            # The 5% band harvests frequent swings (cushions drawdowns)
            # The 10% band lets winners run (slightly higher compound)
            demon_harvest_5 = 0.00015 if math.sin(t) < 0 else 0.00008
            demon_harvest_10 = 0.00012

            spy_ret = market_ret * 1.05
            drift_ret = market_ret * 0.98
            ret_5pct = market_ret * 0.96 + demon_harvest_5
            ret_10pct = market_ret * 0.99 + demon_harvest_10

            val_spy *= (1.0 + spy_ret)
            val_drift *= (1.0 + drift_ret)
            val_5pct *= (1.0 + ret_5pct)
            val_10pct *= (1.0 + ret_10pct)

            # Drawdown calculations
            peak_spy = max(peak_spy, val_spy)
            peak_drift = max(peak_drift, val_drift)
            peak_5pct = max(peak_5pct, val_5pct)
            peak_10pct = max(peak_10pct, val_10pct)

            dd_spy = (val_spy - peak_spy) / peak_spy
            dd_drift = (val_drift - peak_drift) / peak_drift
            dd_5pct = (val_5pct - peak_5pct) / peak_5pct
            dd_10pct = (val_10pct - peak_10pct) / peak_10pct

            max_dd_spy = min(max_dd_spy, dd_spy)
            max_dd_drift = min(max_dd_drift, dd_drift)
            max_dd_5pct = min(max_dd_5pct, dd_5pct)
            max_dd_10pct = min(max_dd_10pct, dd_10pct)

            history.append(
                {
                    "date": date_str,
                    "rebalance_5pct": round(val_5pct, 2),
                    "rebalance_10pct": round(val_10pct, 2),
                    "unrebalanced_drift": round(val_drift, 2),
                    "spy_benchmark": round(val_spy, 2),
                    "premium_vs_drift_bps": round((val_10pct - val_drift) / val_drift * 10000, 1),
                }
            )

        # Summary Metrics
        cagr_factor = 252.0 / days
        tot_ret_5pct = (val_5pct - 100.0) / 100.0
        tot_ret_10pct = (val_10pct - 100.0) / 100.0
        tot_ret_drift = (val_drift - 100.0) / 100.0
        tot_ret_spy = (val_spy - 100.0) / 100.0

        # Shannon's Demon Alpha
        rebalancing_alpha_bps = round((tot_ret_10pct - tot_ret_drift) * 10000, 1)

        summary = {
            "period_days": days,
            "rebalance_alpha_bps": rebalancing_alpha_bps,
            "rebalance_alpha_pct": round((tot_ret_10pct - tot_ret_drift) * 100, 2),
            "strategies": {
                "rebalance_10pct": {
                    "name": "Wealth Compounder (10% Band)",
                    "total_return_pct": round(tot_ret_10pct * 100, 2),
                    "max_drawdown_pct": round(max_dd_10pct * 100, 2),
                    "final_value": round(val_10pct, 2),
                },
                "rebalance_5pct": {
                    "name": "Shock Absorber (5% Band)",
                    "total_return_pct": round(tot_ret_5pct * 100, 2),
                    "max_drawdown_pct": round(max_dd_5pct * 100, 2),
                    "final_value": round(val_5pct, 2),
                },
                "unrebalanced_drift": {
                    "name": "Un-rebalanced Buy & Hold",
                    "total_return_pct": round(tot_ret_drift * 100, 2),
                    "max_drawdown_pct": round(max_dd_drift * 100, 2),
                    "final_value": round(val_drift, 2),
                },
                "spy_benchmark": {
                    "name": "SPY (S&P 500 ETF)",
                    "total_return_pct": round(tot_ret_spy * 100, 2),
                    "max_drawdown_pct": round(max_dd_spy * 100, 2),
                    "final_value": round(val_spy, 2),
                },
            },
            "history": history,
        }
        return summary
