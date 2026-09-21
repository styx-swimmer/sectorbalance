package com.sector.rebalance.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Tune
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
import com.sector.rebalance.domain.RiskProfile
import com.sector.rebalance.ui.dashboard.DashboardViewModel
import com.sector.rebalance.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val portfolio by viewModel.portfolioState.collectAsState()
    val isShock = portfolio.driftThresholdPct <= 5.0

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
                text = "Settings & Profiles",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure drift tolerance bands and engine parameters.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 2. Risk Profile Selection
        item {
            Text(
                text = "RISK PROFILE & DRIFT TOLERANCE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            ProfileSelectionCard(
                profile = RiskProfile.SHOCK_ABSORBER,
                isSelected = isShock,
                onSelect = { viewModel.setRiskProfile("SHOCK_ABSORBER") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileSelectionCard(
                profile = RiskProfile.WEALTH_COMPOUNDER,
                isSelected = !isShock,
                onSelect = { viewModel.setRiskProfile("WEALTH_COMPOUNDER") }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. Asset Universe
        item {
            Text(
                text = "PORTFOLIO UNIVERSE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "11 SPDR Sector ETFs (Equal Weight)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Target allocation: 9.0909% per sector", style = MaterialTheme.typography.bodyMedium, color = PrimaryTeal)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "XLB, XLE, XLF, XLI, XLK, XLP, XLU, XLV, XLY, XLC, XLRE + SPY Benchmark",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. Cloud & Data Backend Info
        item {
            Text(
                text = "CLOUD & DATA INFRASTRUCTURE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = RegimeGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Google Cloud Firestore Sync Active", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Data Feed: yfinance & Massive.com API", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(text = "Regime Model: 3-State Gaussian Hidden Markov Model", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(text = "Turnover Model: 2 bps one-way execution friction", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ProfileSelectionCard(
    profile: RiskProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryTeal else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = profile.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = PrimaryTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = profile.tagline,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryTeal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = profile.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}
