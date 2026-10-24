package com.sector.rebalance.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val isLive by viewModel.isLiveSync.collectAsState()
    val syncTime by viewModel.lastSyncTime.collectAsState()
    val syncSource by viewModel.syncSource.collectAsState()
    val isShock = portfolio.driftThresholdPct <= 5.0

    var cloudRunUrlInput by remember { mutableStateOf(com.sector.rebalance.data.repository.DEFAULT_CLOUD_RUN_URL) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

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
                text = "Settings & Data Sync",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure drift tolerance bands and verify backend data source.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 2. Data Connection Status Card
        item {
            Text(
                text = "CURRENT DATA SOURCE & CONNECTION",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isLive) RegimeGreen.copy(alpha = 0.4f) else RegimeYellow.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isLive) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isLive) RegimeGreen else RegimeYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isLive) "LIVE CALCULATED DATA" else "DEMO / SAMPLE DATA",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isLive) RegimeGreen else RegimeYellow,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = syncSource,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Last Calculation Time", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(text = syncTime, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Direct Cloud Run Sync
        item {
            Text(
                text = "CLOUD RUN DIRECT SYNC",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fetch live calculations directly from your Cloud Run service URL:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cloudRunUrlInput,
                        onValueChange = { cloudRunUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://sector-rebalance-service-...run.app", color = TextSecondary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (cloudRunUrlInput.isNotBlank()) {
                                isSyncing = true
                                syncStatusMessage = "Connecting to Cloud Run..."
                                viewModel.syncFromCloudRun(cloudRunUrlInput) { success, msg ->
                                    isSyncing = false
                                    syncStatusMessage = msg
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing && cloudRunUrlInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = BackgroundDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSyncing) "Fetching..." else "Fetch Live Data From Cloud Run",
                            color = BackgroundDark,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (syncStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncStatusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLive) RegimeGreen else RegimeYellow
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 4. Risk Profile Selection
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

        // 5. Asset Universe
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
