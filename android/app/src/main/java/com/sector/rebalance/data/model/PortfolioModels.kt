package com.sector.rebalance.data.model

data class Sector(
    val ticker: String,
    val name: String,
    val price: Double,
    val shares: Double,
    val value: Double,
    val currentWeight: Double,
    val targetWeight: Double = 9.09,
    val driftPct: Double,
    val absDriftPct: Double,
    val status: String = "BALANCED", // BALANCED, WATCH, REBALANCE_TRIGGERED
    val isBreached: Boolean = false
)

data class TradeOrder(
    val ticker: String,
    val name: String,
    val action: String, // BUY, SELL, HOLD
    val currentShares: Double,
    val targetShares: Double,
    val shareChange: Double,
    val currentValue: Double,
    val targetValue: Double,
    val dollarAmount: Double,
    val isTriggerSource: Boolean = false
)

data class PortfolioState(
    val portfolioValue: Double = 100000.0,
    val targetPerSector: Double = 9.09,
    val riskProfile: String = "Shock Absorber",
    val driftThresholdPct: Double = 5.0,
    val maxDriftPct: Double = 0.0,
    val status: String = "BALANCED", // BALANCED, WATCH, REBALANCE_TRIGGERED
    val rebalanceNeeded: Boolean = false,
    val breachedSectors: List<String> = emptyList(),
    val sectors: List<Sector> = emptyList(),
    val orders: List<TradeOrder> = emptyList(),
    val turnoverDollars: Double = 0.0,
    val turnoverPct: Double = 0.0,
    val estimatedFeeDollars: Double = 0.0,
    val educationalRationale: String = "",
    val updatedAt: String = ""
)

data class StrategyMetric(
    val name: String,
    val totalReturnPct: Double,
    val maxDrawdownPct: Double,
    val finalValue: Double
)

data class HistoricalPoint(
    val date: String,
    val rebalance5pct: Double,
    val rebalance10pct: Double,
    val unrebalancedDrift: Double,
    val spyBenchmark: Double,
    val premiumVsDriftBps: Double
)

data class PerformanceData(
    val periodDays: Int = 180,
    val rebalanceAlphaBps: Double = 258.0,
    val rebalanceAlphaPct: Double = 2.58,
    val strategies: Map<String, StrategyMetric> = emptyMap(),
    val history: List<HistoricalPoint> = emptyList(),
    val updatedAt: String = ""
)
