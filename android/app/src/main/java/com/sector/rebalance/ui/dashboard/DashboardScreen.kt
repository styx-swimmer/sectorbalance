package com.sector.rebalance.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sector.rebalance.ui.components.MarketRegimeBanner
import com.sector.rebalance.ui.components.SectorRowItem
import com.sector.rebalance.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToRebalance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val regime by viewModel.marketRegime.collectAsState()
    val portfolio by viewModel.portfolioState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // 1. Top Market Regime Banner (Hidden Markov Model)
        item {
            MarketRegimeBanner(regime = regime)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Portfolio Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PORTFOLIO VALUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$${String.format("%,.2f", portfolio.portfolioValue)}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Rebalance Status Badge
                        val (statusBg, statusFg, statusText, statusIcon) = if (portfolio.rebalanceNeeded) {
                            Quadruple(RegimeRed.copy(alpha = 0.2f), RegimeRed, "REBALANCE TRIGGERED", Icons.Default.Warning)
                        } else {
                            Quadruple(RegimeGreen.copy(alpha = 0.2f), RegimeGreen, "BALANCED", Icons.Default.CheckCircle)
                        }

                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusFg,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = statusText,
                                    color = statusFg,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Row: Profile, Max Drift, Tolerance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Profile", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(text = portfolio.riskProfile, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text(text = "Max Drift", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = "${String.format("%.2f", portfolio.maxDriftPct)}%",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (portfolio.rebalanceNeeded) RegimeRed else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(text = "Tolerance Band", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(text = "±${String.format("%.1f", portfolio.driftThresholdPct)}%", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (portfolio.rebalanceNeeded) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToRebalance,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Autorenew, contentDescription = null, tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "View Rebalance Orders (${portfolio.orders.filter { it.action != "HOLD" }.size})", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Section Title: 11 SPDR Sectors
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "11 SPDR SECTORS (EQUAL-WEIGHT 9.09%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${portfolio.sectors.size} ETFs",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 4. Sector Items List
        items(portfolio.sectors) { sector ->
            SectorRowItem(sector = sector)
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
