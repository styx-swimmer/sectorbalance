import unittest
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from hmm_regime import GaussianHMMDetector
from config import REGIME_LOW_VOL, REGIME_CHOPPY, REGIME_HIGH_VOL


class TestHMMDetector(unittest.TestCase):
    def setUp(self):
        self.detector = GaussianHMMDetector()

    def test_regime_classification_keys(self):
        returns = [0.0005] * 50
        vols = [0.12] * 50
        result = self.detector.fit_and_predict(returns, vols)

        self.assertIn("regime_key", result)
        self.assertIn("title", result)
        self.assertIn("badge_color", result)
        self.assertIn("recommendation", result)
        self.assertIn("probabilities", result)

        probs = result["probabilities"]
        self.assertIn(REGIME_LOW_VOL, probs)
        self.assertIn(REGIME_CHOPPY, probs)
        self.assertIn(REGIME_HIGH_VOL, probs)

    def test_high_volatility_classification(self):
        # Severe crash returns and high volatility
        returns = [-0.03] * 50
        vols = [0.45] * 50
        result = self.detector.fit_and_predict(returns, vols)

        self.assertEqual(result["regime_key"], REGIME_HIGH_VOL)
        self.assertEqual(result["title"], "High Volatility / Crash")
        self.assertEqual(result["exposure_pct"], 50)


if __name__ == "__main__":
    unittest.main()
