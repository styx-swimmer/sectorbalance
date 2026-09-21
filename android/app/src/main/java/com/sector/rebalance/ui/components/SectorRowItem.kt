package com.sector.rebalance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sector.rebalance.data.model.Sector
import com.sector.rebalance.ui.theme.*

@Composable
fun SectorRowItem(
    sector: Sector,
    modifier: Modifier = Modifier
) {
    val driftSign = if (sector.driftPct > 0) "+" else ""
    val driftColor = when {
        sector.isBreached -> RegimeRed
        sector.absDriftPct >= 3.5 -> RegimeYellow
        else -> TextSecondary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ticker + Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = getSectorColor(sector.ticker).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = sector.ticker,
                                style = MaterialTheme.typography.labelSmall,
                                color = getSectorColor(sector.ticker),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = sector.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$${String.format("%.2f", sector.price)} • ${String.format("%.1f", sector.shares)} sh",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Weight & Drift
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.2f", sector.currentWeight)}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Target ${String.format("%.2f", sector.targetWeight)}%  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "($driftSign${String.format("%.2f", sector.driftPct)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = driftColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual Allocation Bar vs Target
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                // Target Marker (at 9.09% out of 25% scale)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .offset(x = (100 * (9.09f / 25.0f)).dp)
                        .background(TextSecondary)
                )

                // Current Weight Bar
                val progress = (sector.currentWeight / 25.0).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (sector.isBreached) RegimeRed else getSectorColor(sector.ticker))
                )
            }
        }
    }
}

fun getSectorColor(ticker: String): Color {
    return when (ticker) {
        "XLK" -> ColorTech
        "XLV" -> ColorHealth
        "XLF" -> ColorFin
        "XLY" -> ColorConsDisc
        "XLC" -> ColorComm
        "XLI" -> ColorIndust
        "XLP" -> ColorStaples
        "XLE" -> ColorEnergy
        "XLU" -> ColorUtils
        "XLRE" -> ColorRealEst
        "XLB" -> ColorMaterials
        else -> PrimaryTeal
    }
}
