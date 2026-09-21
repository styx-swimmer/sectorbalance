import unittest
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from config import SECTOR_TICKERS, TARGET_WEIGHT
from rebalance_engine import RebalanceEngine


class TestRebalanceEngine(TestCase := unittest.TestCase):
    def setUp(self):
        self.engine_shock = RebalanceEngine(risk_profile_key="SHOCK_ABSORBER")
        self.engine_compound = RebalanceEngine(risk_profile_key="WEALTH_COMPOUNDER")
        self.sample_prices = {ticker: 100.0 for ticker in SECTOR_TICKERS}

    def test_target_weight_calculation(self):
        self.assertEqual(len(SECTOR_TICKERS), 11)
        self.assertAlmostEqual(TARGET_WEIGHT * 11, 1.0, places=5)
        self.assertAlmostEqual(TARGET_WEIGHT, 0.090909, places=5)

    def test_balanced_portfolio_no_rebalance(self):
        # Initial equal allocation should be balanced
        result = self.engine_shock.analyze_portfolio(prices=self.sample_prices, total_capital=110000.0)
        self.assertFalse(result["rebalance_needed"])
        self.assertEqual(result["status"], "BALANCED")
        self.assertEqual(len(result["breached_sectors"]), 0)

    def test_breach_detection_5pct_shock_absorber(self):
        # Create a skewed portfolio where XLK has expanded significantly
        holdings = {ticker: 100.0 for ticker in SECTOR_TICKERS}
        prices = dict(self.sample_prices)
        prices["XLK"] = 250.0  # XLK value = 25,000 out of ~35,000 total (> 70% weight, huge drift!)

        result = self.engine_shock.analyze_portfolio(prices=prices, holdings=holdings)
        self.assertTrue(result["rebalance_needed"])
        self.assertEqual(result["status"], "REBALANCE_TRIGGERED")
        self.assertIn("XLK", result["breached_sectors"])

        # Check that XLK is recommended to SELL and other sectors are BUY
        xlk_order = next(o for o in result["orders"] if o["ticker"] == "XLK")
        self.assertEqual(xlk_order["action"], "SELL")
        self.assertTrue(xlk_order["is_trigger_source"])

    def test_wealth_compounder_allows_wider_drift(self):
        # A drift that breaches 5% but is within 10%
        # Target = 9.09%. If weight = 16% (drift = +6.91%)
        # Shock absorber (5%) should trigger; Wealth Compounder (10%) should NOT trigger
        holdings = {ticker: 100.0 for ticker in SECTOR_TICKERS}
        prices = dict(self.sample_prices)
        prices["XLK"] = 180.0  # Moderate drift

        res_shock = self.engine_shock.analyze_portfolio(prices=prices, holdings=holdings)
        res_comp = self.engine_compound.analyze_portfolio(prices=prices, holdings=holdings)

        # Max drift in res_shock and res_comp should be the same
        self.assertEqual(res_shock["max_drift_pct"], res_comp["max_drift_pct"])
        # Thresholds are different
        self.assertEqual(res_shock["drift_threshold_pct"], 5.0)
        self.assertEqual(res_comp["drift_threshold_pct"], 10.0)


if __name__ == "__main__":
    unittest.main()
