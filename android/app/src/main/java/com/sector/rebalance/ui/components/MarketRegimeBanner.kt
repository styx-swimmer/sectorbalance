package com.sector.rebalance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sector.rebalance.data.model.MarketRegime
import com.sector.rebalance.ui.theme.RegimeGreen
import com.sector.rebalance.ui.theme.RegimeRed
import com.sector.rebalance.ui.theme.RegimeYellow
import com.sector.rebalance.ui.theme.SurfaceDark
import com.sector.rebalance.ui.theme.TextPrimary
import com.sector.rebalance.ui.theme.TextSecondary

@Composable
fun MarketRegimeBanner(
    regime: MarketRegime,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val (badgeColor, statusIcon, statusLabel) = when (regime.regimeKey) {
        "HIGH_VOL_CRASH" -> Triple(RegimeRed, Icons.Default.Warning, "High Volatility / Crash")
        "CHOPPY_SIDEWAYS" -> Triple(RegimeYellow, Icons.Default.Shield, "Choppy / Sideways")
        else -> Triple(RegimeGreen, Icons.Default.TrendingUp, "Low Volatility / Slow Grind Up")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Regime Tag & Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Text(
                        text = "DAILY MARKET REGIME (HMM)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "${regime.confidence.toInt()}% Confidence",
                            color = badgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Title Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = regime.recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = badgeColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Expanded Breakdown
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = regime.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Probability Bars
                    Text(
                        text = "Hidden Markov State Distribution:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    RegimeProbabilityRow("Low Vol Grind Up", regime.probLowVol, RegimeGreen)
                    RegimeProbabilityRow("Choppy / Sideways", regime.probChoppy, RegimeYellow)
                    RegimeProbabilityRow("High Vol / Crash", regime.probHighVol, RegimeRed)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Target Exposure: ${regime.exposurePct}% Invested ${if (regime.exposurePct < 100) "/ ${100 - regime.exposurePct}% Cash Shield" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RegimeProbabilityRow(label: String, pct: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1.2f))
        LinearProgressIndicator(
            progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(2f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
        Text(
            text = "${pct.toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier
                .weight(0.5f)
                .padding(start = 8.dp),
            fontWeight = FontWeight.Medium
        )
    }
}
