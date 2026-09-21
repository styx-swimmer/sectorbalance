package com.sector.rebalance.data.model

data class MarketRegime(
    val regimeKey: String = "LOW_VOL_GRIND_UP",
    val title: String = "Low Volatility / Slow Grind Up",
    val badgeColorHex: String = "#10B981",
    val recommendation: String = "100% Invested in Rebalancing Engine",
    val exposurePct: Int = 100,
    val description: String = "Bullish, steady compounding environment.",
    val confidence: Double = 85.0,
    val probLowVol: Double = 85.0,
    val probChoppy: Double = 10.0,
    val probHighVol: Double = 5.0,
    val updatedAt: String = ""
)
