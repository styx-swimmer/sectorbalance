package com.sector.rebalance.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sector.rebalance.data.model.StrategyMetric
import com.sector.rebalance.data.repository.SectorRepository
import com.sector.rebalance.ui.theme.*

@Composable
fun AnalyticsScreen(
    repository: SectorRepository,
    modifier: Modifier = Modifier
) {
    val performance by repository.performanceData.collectAsState(
        initial = com.sector.rebalance.data.repository.FirestoreSectorRepository.createSamplePerformanceData()
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // 1. Header
        item {
            Text(
                text = "Performance & Alpha",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Shannon's Demon Rebalancing Premium vs Benchmarks.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Big Hero Metric: Rebalancing Premium
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryTeal.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SHANNON'S DEMON ALPHA",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryTeal,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "+${String.format("%.0f", performance.rebalanceAlphaBps)} bps",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = PrimaryTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoGraph,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pure volatility harvesting premium (+${String.format("%.2f", performance.rebalanceAlphaPct)}%) captured systematically over un-rebalanced drift without predicting market direction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Strategy Comparison Section
        item {
            Text(
                text = "STRATEGY COMPARISON",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 4. Comparison Cards
        performance.strategies.values.forEach { strategy ->
            item {
                StrategyComparisonCard(strategy = strategy)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 5. Research Findings Card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                        Text(
                            text = "EMPIRICAL RESEARCH FINDINGS (1998–2026)",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    BulletPoint("1998–2008 Crashes: Rebalancing beat SPY (+2.05% vs -1.62%) by buying deep crash dips and trimming bubbles.")
                    Spacer(modifier = Modifier.height(6.dp))
                    BulletPoint("2020–2026 Inflation/Shock: Cut max drawdown nearly in half (-16.28% vs SPY -23.93%).")
                    Spacer(modifier = Modifier.height(6.dp))
                    BulletPoint("Calendar Seasonality: Disproven. Static 5-10% bands beat heuristic calendar adjustments.")
                }
            }
        }
    }
}

@Composable
fun StrategyComparisonCard(strategy: StrategyMetric) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = strategy.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Max Drawdown: ${String.format("%.2f", strategy.maxDrawdownPct)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (strategy.maxDrawdownPct > -20.0) RegimeGreen else RegimeRed
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${String.format("%.2f", strategy.totalReturnPct)}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = RegimeGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${String.format("%.2f", strategy.finalValue)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "• ", style = MaterialTheme.typography.bodyMedium, color = PrimaryTeal)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
