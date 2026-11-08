"""
Configuration for Sector Rebalancing Platform & HMM Regime Engine.
11 SPDR Sector ETFs + SPY Benchmark.
"""
from typing import Dict, List

# Default GCP Project ID for Firestore
DEFAULT_PROJECT_ID = "sectorbalance"

# 11 SPDR Sector ETFs
SECTOR_TICKERS: List[str] = [
    "XLB",   # Materials
    "XLE",   # Energy
    "XLF",   # Financials
    "XLI",   # Industrials
    "XLK",   # Technology
    "XLP",   # Consumer Staples
    "XLU",   # Utilities
    "XLV",   # Health Care
    "XLY",   # Consumer Discretionary
    "XLC",   # Communication Services
    "XLRE",  # Real Estate
]

BENCHMARK_TICKER: str = "SPY"

# Sector metadata mapping
SECTOR_NAMES: Dict[str, str] = {
    "XLB": "Materials",
    "XLE": "Energy",
    "XLF": "Financials",
    "XLI": "Industrials",
    "XLK": "Technology",
    "XLP": "Consumer Staples",
    "XLU": "Utilities",
    "XLV": "Health Care",
    "XLY": "Consumer Discretionary",
    "XLC": "Communication Services",
    "XLRE": "Real Estate",
}

# Equal-Weight Target per sector: 1 / 11 = 9.0909%
NUM_SECTORS: int = len(SECTOR_TICKERS)
TARGET_WEIGHT: float = 1.0 / NUM_SECTORS  # ~0.090909

# Inception baseline configuration (established September 21, 2026)
# Used to initialize holdings if no prior history is in Firestore
INCEPTION_DATE = "2026-09-21"
INCEPTION_CAPITAL = 100000.0
INCEPTION_HOLDINGS: Dict[str, float] = {
    "XLB": 99.40,    # Materials (~$9,091 at $91.46)
    "XLE": 102.16,   # Energy (~$9,091 at $88.99)
    "XLF": 196.35,   # Financials (~$9,091 at $46.30)
    "XLI": 70.45,    # Industrials (~$9,091 at $129.04)
    "XLK": 39.42,    # Technology (~$9,091 at $230.59)
    "XLP": 112.76,   # Consumer Staples (~$9,091 at $80.62)
    "XLU": 121.15,   # Utilities (~$9,091 at $75.04)
    "XLV": 61.55,    # Health Care (~$9,091 at $147.69)
    "XLY": 47.13,    # Consumer Discretionary (~$9,091 at $192.91)
    "XLC": 101.65,   # Communication Services (~$9,091 at $89.43)
    "XLRE": 218.53,  # Real Estate (~$9,091 at $41.60)
}

# Risk Profiles & Drift Tolerances
# Drift defined as absolute tolerance around the target weight
# e.g., 5% tolerance means [target - 5%, target + 5%]
RISK_PROFILES = {
    "SHOCK_ABSORBER": {
        "name": "Shock Absorber",
        "description": "5% Drift Band. Tighter risk control, frequent volatility harvesting, deep drawdown shield.",
        "drift_threshold": 0.05,  # 5% absolute drift tolerance
        "rebalance_band_min": max(0.0, TARGET_WEIGHT - 0.05),
        "rebalance_band_max": TARGET_WEIGHT + 0.05,
    },
    "WEALTH_COMPOUNDER": {
        "name": "Wealth Compounder",
        "description": "10% Drift Band. Lets winners run, minimizes turnover friction, maximizes long-term compounding.",
        "drift_threshold": 0.10,  # 10% absolute drift tolerance
        "rebalance_band_min": max(0.0, TARGET_WEIGHT - 0.10),
        "rebalance_band_max": TARGET_WEIGHT + 0.10,
    },
}

# 3-State Hidden Markov Model Regimes
REGIME_LOW_VOL = "LOW_VOL_GRIND_UP"
REGIME_CHOPPY = "CHOPPY_SIDEWAYS"
REGIME_HIGH_VOL = "HIGH_VOL_CRASH"

REGIME_DETAILS = {
    REGIME_LOW_VOL: {
        "title": "Low Volatility / Slow Grind Up",
        "badge_color": "#10B981",  # Emerald Green
        "icon": "trending_up",
        "recommendation": "100% Invested in Rebalancing Engine",
        "exposure_pct": 100,
        "description": "Bullish, steady compounding environment. Volatility is low and momentum is stable.",
    },
    REGIME_CHOPPY: {
        "title": "Choppy / Sideways",
        "badge_color": "#F59E0B",  # Amber Yellow
        "icon": "swap_horiz",
        "recommendation": "100% Invested - Prime for Shannon's Demon Volatility Harvesting",
        "exposure_pct": 100,
        "description": "Mean-reverting environment. Ideal conditions for cross-sectional rebalancing to capture premium.",
    },
    REGIME_HIGH_VOL: {
        "title": "High Volatility / Crash",
        "badge_color": "#EF4444",  # Crimson Red
        "icon": "warning",
        "recommendation": "High Volatility Alert: Consider Defensive 50% Equity / 50% Cash scaling",
        "exposure_pct": 50,
        "description": "Elevated market stress and turbulent shocks. Exercise defensive discipline.",
    },
}

# Assumed turnover friction (one-way 2 bps = 0.0002)
ONE_WAY_FEE_BPS = 0.0002

# Firestore Collections
FIRESTORE_COLLECTIONS = {
    "MARKET_REGIME": "market_regime",
    "SECTORS_CURRENT": "sectors_current",
    "PORTFOLIO_STATE": "portfolio_state",
    "PERFORMANCE_HISTORY": "performance_history",
}
