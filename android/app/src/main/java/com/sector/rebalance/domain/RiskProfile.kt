package com.sector.rebalance.domain

enum class RiskProfile(
    val key: String,
    val title: String,
    val driftThresholdPct: Double,
    val tagline: String,
    val description: String
) {
    SHOCK_ABSORBER(
        key = "SHOCK_ABSORBER",
        title = "Shock Absorber",
        driftThresholdPct = 5.0,
        tagline = "5% Drift Tolerance",
        description = "Harvests cross-sectional volatility frequently and cuts drawdowns in half during market shocks."
    ),
    WEALTH_COMPOUNDER(
        key = "WEALTH_COMPOUNDER",
        title = "Wealth Compounder",
        driftThresholdPct = 10.0,
        tagline = "10% Drift Tolerance",
        description = "Lets winners run longer and minimizes turnover friction for maximum long-term capital compounding."
    );

    companion object {
        fun fromKey(key: String): RiskProfile {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: SHOCK_ABSORBER
        }
    }
}
