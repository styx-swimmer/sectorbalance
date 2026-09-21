package com.sector.rebalance.ui.action

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sector.rebalance.data.model.TradeOrder
import com.sector.rebalance.ui.dashboard.DashboardViewModel
import com.sector.rebalance.ui.theme.*

@Composable
fun RebalanceActionScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val portfolio by viewModel.portfolioState.collectAsState()
    val activeOrders = portfolio.orders.filter { it.action != "HOLD" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // 1. Header Title
        item {
            Text(
                text = "Rebalance Assistant",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Shannon's Demon execution orders to restore equal weighting.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Educational Rationale Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "STRATEGY RATIONALE",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = portfolio.educationalRationale,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Turnover & Friction Metrics
        if (portfolio.rebalanceNeeded) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceVariantDark,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Turnover", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = "$${String.format("%,.0f", portfolio.turnoverDollars)} (${String.format("%.1f", portfolio.turnoverPct)}%)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(text = "Est. Fee (2 bps)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = "$${String.format("%.2f", portfolio.estimatedFeeDollars)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(text = "Orders", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = "${activeOrders.size} Trades",
                                style = MaterialTheme.typography.bodyLarge,
                                color = PrimaryTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 4. Trade Orders List
        if (activeOrders.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = RegimeGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Rebalancing Needed",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All 11 sectors are within your ${String.format("%.1f", portfolio.driftThresholdPct)}% tolerance band. Relax and let your capital compound.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "RECOMMENDED TRADES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(activeOrders) { order ->
                OrderRowItem(order = order)
            }
        }
    }
}

@Composable
fun OrderRowItem(order: TradeOrder) {
    val isSell = order.action == "SELL"
    val actionColor = if (isSell) RegimeRed else RegimeGreen
    val actionIcon = if (isSell) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = actionColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = actionIcon, contentDescription = null, tint = actionColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = order.action,
                            style = MaterialTheme.typography.labelSmall,
                            color = actionColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = order.ticker,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${order.name} • ${String.format("%.2f", order.shareChange)} shares",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%,.2f", order.dollarAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (order.isTriggerSource) "Trigger Source" else "Equalize",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (order.isTriggerSource) RegimeRed else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
