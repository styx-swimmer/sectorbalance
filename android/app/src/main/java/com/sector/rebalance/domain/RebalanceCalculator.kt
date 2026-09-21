package com.sector.rebalance.domain

import com.sector.rebalance.data.model.Sector
import com.sector.rebalance.data.model.TradeOrder

object RebalanceCalculator {
    const val NUM_SECTORS = 11
    const val TARGET_WEIGHT = 100.0 / NUM_SECTORS // ~9.0909%

    /**
     * Evaluates drift for 11 sectors against specified threshold band.
     */
    fun checkBreaches(sectors: List<Sector>, thresholdPct: Double): List<String> {
        return sectors.filter { it.absDriftPct >= thresholdPct }.map { it.ticker }
    }

    /**
     * Calculates dollar amount and share count needed to return to equal weight.
     */
    fun calculateOrderForSector(
        sector: Sector,
        totalPortfolioValue: Double
    ): TradeOrder {
        val targetValue = totalPortfolioValue * (TARGET_WEIGHT / 100.0)
        val dollarDiff = targetValue - sector.value
        val action = when {
            dollarDiff > 10.0 -> "BUY"
            dollarDiff < -10.0 -> "SELL"
            else -> "HOLD"
        }
        val targetShares = if (sector.price > 0) targetValue / sector.price else 0.0
        val shareChange = if (sector.price > 0) Math.abs(dollarDiff) / sector.price else 0.0

        return TradeOrder(
            ticker = sector.ticker,
            name = sector.name,
            action = action,
            currentShares = sector.shares,
            targetShares = targetShares,
            shareChange = shareChange,
            currentValue = sector.value,
            targetValue = targetValue,
            dollarAmount = Math.abs(dollarDiff),
            isTriggerSource = sector.isBreached
        )
    }
}
