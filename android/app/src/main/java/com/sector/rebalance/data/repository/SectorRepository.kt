package com.sector.rebalance.data.repository

import android.util.Log
import com.sector.rebalance.data.model.HistoricalPoint
import com.sector.rebalance.data.model.MarketRegime
import com.sector.rebalance.data.model.PortfolioState
import com.sector.rebalance.data.model.PerformanceData
import com.sector.rebalance.data.model.Sector
import com.sector.rebalance.data.model.StrategyMetric
import com.sector.rebalance.data.model.TradeOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

const val DEFAULT_CLOUD_RUN_URL = "https://sector-rebalance-service-616340915754.us-central1.run.app"

interface SectorRepository {
    val marketRegime: Flow<MarketRegime>
    val portfolioState: Flow<PortfolioState>
    val performanceData: Flow<PerformanceData>
    val isLiveSync: Flow<Boolean>
    val lastSyncTime: Flow<String>
    val syncSource: Flow<String>

    suspend fun refreshData()
    suspend fun updateRiskProfile(profileKey: String)
    suspend fun fetchFromCloudRunUrl(baseUrl: String = DEFAULT_CLOUD_RUN_URL): Result<String>
}

class FirestoreSectorRepository : SectorRepository {

    private val _marketRegime = MutableStateFlow(createInitialMarketRegime())
    override val marketRegime: Flow<MarketRegime> = _marketRegime.asStateFlow()

    private val _portfolioState = MutableStateFlow(createInitialPortfolioState(5.0))
    override val portfolioState: Flow<PortfolioState> = _portfolioState.asStateFlow()

    private val _performanceData = MutableStateFlow(createInitialPerformanceData())
    override val performanceData: Flow<PerformanceData> = _performanceData.asStateFlow()

    // Default directly to LIVE mode with the hardcoded Cloud Run URL
    private val _isLiveSync = MutableStateFlow(true)
    override val isLiveSync: Flow<Boolean> = _isLiveSync.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Connecting to Cloud Run...")
    override val lastSyncTime: Flow<String> = _lastSyncTime.asStateFlow()

    private val _syncSource = MutableStateFlow("Cloud Run Live Backend")
    override val syncSource: Flow<String> = _syncSource.asStateFlow()

    init {
        // 1. Immediately fetch latest calculated data from the hardcoded Cloud Run endpoint on startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i("SectorRepository", "Connecting to live Cloud Run endpoint: $DEFAULT_CLOUD_RUN_URL")
                val result = fetchFromCloudRunUrl(DEFAULT_CLOUD_RUN_URL)
                if (result.isSuccess) {
                    Log.i("SectorRepository", "Live calculations loaded successfully from Cloud Run!")
                } else {
                    Log.w("SectorRepository", "Initial Cloud Run fetch notice: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w("SectorRepository", "Startup sync exception: ${e.message}")
            }
        }

        // 2. Also register Firestore real-time listener if Firebase is available
        tryInitFirestore()
    }

    private fun tryInitFirestore() {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            Log.d("SectorRepository", "Connecting to Firebase Firestore instance...")

            // Listen to Market Regime
            firestore.collection("market_regime").document("daily_state")
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val data = snapshot.data ?: return@addSnapshotListener

                    val updateTime = data["updated_at"] as? String ?: ""
                    _isLiveSync.value = true
                    _syncSource.value = "Google Cloud Firestore"
                    if (updateTime.isNotEmpty()) {
                        _lastSyncTime.value = formatTimestamp(updateTime)
                    }

                    _marketRegime.value = MarketRegime(
                        regimeKey = data["regime_key"] as? String ?: "LOW_VOL_GRIND_UP",
                        title = data["title"] as? String ?: "Low Volatility / Slow Grind Up",
                        badgeColorHex = data["badge_color"] as? String ?: "#10B981",
                        recommendation = data["recommendation"] as? String ?: "100% Invested in Rebalancing Engine",
                        exposurePct = (data["exposure_pct"] as? Long)?.toInt() ?: 100,
                        description = data["description"] as? String ?: "",
                        confidence = (data["confidence"] as? Double) ?: 82.0,
                        probLowVol = ((data["probabilities"] as? Map<*, *>)?.get("LOW_VOL_GRIND_UP") as? Number)?.toDouble() ?: 82.0,
                        probChoppy = ((data["probabilities"] as? Map<*, *>)?.get("CHOPPY_SIDEWAYS") as? Number)?.toDouble() ?: 13.0,
                        probHighVol = ((data["probabilities"] as? Map<*, *>)?.get("HIGH_VOL_CRASH") as? Number)?.toDouble() ?: 5.0,
                        updatedAt = updateTime
                    )
                }

            // Listen to Portfolio State
            firestore.collection("portfolio_state").document("current")
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val data = snapshot.data ?: return@addSnapshotListener

                    val updateTime = data["updated_at"] as? String ?: ""
                    _isLiveSync.value = true
                    _syncSource.value = "Google Cloud Firestore"
                    if (updateTime.isNotEmpty()) {
                        _lastSyncTime.value = formatTimestamp(updateTime)
                    }

                    val rawSectors = data["sectors"] as? List<Map<String, Any>> ?: emptyList()
                    val sectorsList = rawSectors.map { s ->
                        Sector(
                            ticker = s["ticker"] as? String ?: "",
                            name = s["name"] as? String ?: "",
                            price = (s["price"] as? Number)?.toDouble() ?: 0.0,
                            shares = (s["shares"] as? Number)?.toDouble() ?: 0.0,
                            value = (s["value"] as? Number)?.toDouble() ?: 0.0,
                            currentWeight = (s["current_weight"] as? Number)?.toDouble() ?: 0.0,
                            targetWeight = (s["target_weight"] as? Number)?.toDouble() ?: 9.09,
                            driftPct = (s["drift_pct"] as? Number)?.toDouble() ?: 0.0,
                            absDriftPct = (s["abs_drift_pct"] as? Number)?.toDouble() ?: 0.0,
                            status = s["status"] as? String ?: "BALANCED",
                            isBreached = s["is_breached"] as? Boolean ?: false
                        )
                    }

                    val rawOrders = data["orders"] as? List<Map<String, Any>> ?: emptyList()
                    val ordersList = rawOrders.map { o ->
                        TradeOrder(
                            ticker = o["ticker"] as? String ?: "",
                            name = o["name"] as? String ?: "",
                            action = o["action"] as? String ?: "HOLD",
                            currentShares = (o["current_shares"] as? Number)?.toDouble() ?: 0.0,
                            targetShares = (o["target_shares"] as? Number)?.toDouble() ?: 0.0,
                            shareChange = (o["share_change"] as? Number)?.toDouble() ?: 0.0,
                            currentValue = (o["current_value"] as? Number)?.toDouble() ?: 0.0,
                            targetValue = (o["target_value"] as? Number)?.toDouble() ?: 0.0,
                            dollarAmount = (o["dollar_amount"] as? Number)?.toDouble() ?: 0.0,
                            isTriggerSource = o["is_trigger_source"] as? Boolean ?: false
                        )
                    }

                    _portfolioState.value = PortfolioState(
                        portfolioValue = (data["portfolio_value"] as? Number)?.toDouble() ?: 100000.0,
                        targetPerSector = (data["target_per_sector"] as? Number)?.toDouble() ?: 9.09,
                        riskProfile = data["risk_profile"] as? String ?: "Shock Absorber",
                        driftThresholdPct = (data["drift_threshold_pct"] as? Number)?.toDouble() ?: 5.0,
                        maxDriftPct = (data["max_drift_pct"] as? Number)?.toDouble() ?: 0.0,
                        status = data["status"] as? String ?: "BALANCED",
                        rebalanceNeeded = data["rebalance_needed"] as? Boolean ?: false,
                        sectors = sectorsList,
                        orders = ordersList,
                        turnoverDollars = (data["turnover_dollars"] as? Number)?.toDouble() ?: 0.0,
                        turnoverPct = (data["turnover_pct"] as? Number)?.toDouble() ?: 0.0,
                        educationalRationale = data["educational_rationale"] as? String ?: "",
                        updatedAt = updateTime
                    )
                }

        } catch (e: Exception) {
            Log.d("SectorRepository", "Firestore listening in background; primary live sync via Cloud Run.")
        }
    }

    override suspend fun refreshData() {
        fetchFromCloudRunUrl(DEFAULT_CLOUD_RUN_URL)
    }

    override suspend fun updateRiskProfile(profileKey: String) {
        val threshold = if (profileKey == "WEALTH_COMPOUNDER") 10.0 else 5.0
        // Locally update tolerance threshold on the current sectors
        val curr = _portfolioState.value
        val breached = curr.sectors.filter { it.absDriftPct >= threshold }.map { it.ticker }
        _portfolioState.value = curr.copy(
            riskProfile = if (threshold <= 5.0) "Shock Absorber" else "Wealth Compounder",
            driftThresholdPct = threshold,
            rebalanceNeeded = breached.isNotEmpty(),
            status = if (breached.isNotEmpty()) "REBALANCE_TRIGGERED" else "BALANCED",
            breachedSectors = breached
        )
    }

    override suspend fun fetchFromCloudRunUrl(baseUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim()
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            if (!cleanUrl.endsWith("/status") && !cleanUrl.endsWith("/run")) {
                cleanUrl = cleanUrl.trimEnd('/') + "/status"
            }

            Log.d("SectorRepository", "Connecting to live Cloud Run URL: $cleanUrl")
            val url = URL(cleanUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                return@withContext Result.failure(Exception("HTTP Error $responseCode from Cloud Run endpoint"))
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val jsonString = reader.readText()
            reader.close()

            parseAndApplyLiveJson(jsonString)
            _isLiveSync.value = true
            _syncSource.value = "Cloud Run Live Backend"

            Result.success("Connected to Cloud Run! Live calculation data loaded.")
        } catch (e: Exception) {
            Log.e("SectorRepository", "Cloud Run fetch error", e)
            Result.failure(e)
        }
    }

    private fun parseAndApplyLiveJson(jsonStr: String) {
        val root = JSONObject(jsonStr)
        val dataObj = if (root.has("data")) root.getJSONObject("data") else root
        val timestamp = dataObj.optString("timestamp", root.optString("triggered_at", ""))

        if (timestamp.isNotEmpty()) {
            _lastSyncTime.value = formatTimestamp(timestamp)
        }

        // Market Regime
        if (dataObj.has("market_regime")) {
            val regObj = dataObj.getJSONObject("market_regime")
            val probsObj = regObj.optJSONObject("probabilities")
            _marketRegime.value = MarketRegime(
                regimeKey = regObj.optString("regime_key", "LOW_VOL_GRIND_UP"),
                title = regObj.optString("title", "Low Volatility / Slow Grind Up"),
                badgeColorHex = regObj.optString("badge_color", "#10B981"),
                recommendation = regObj.optString("recommendation", "100% Invested in Rebalancing Engine"),
                exposurePct = regObj.optInt("exposure_pct", 100),
                description = regObj.optString("description", ""),
                confidence = regObj.optDouble("confidence", 82.0),
                probLowVol = probsObj?.optDouble("LOW_VOL_GRIND_UP", 82.0) ?: 82.0,
                probChoppy = probsObj?.optDouble("CHOPPY_SIDEWAYS", 13.0) ?: 13.0,
                probHighVol = probsObj?.optDouble("HIGH_VOL_CRASH", 5.0) ?: 5.0,
                updatedAt = timestamp
            )
        }

        // Portfolio State
        if (dataObj.has("portfolio_state")) {
            val portObj = dataObj.getJSONObject("portfolio_state")
            val sectorsArr = portObj.optJSONArray("sectors") ?: JSONArray()
            val parsedSectors = mutableListOf<Sector>()
            for (i in 0 until sectorsArr.length()) {
                val s = sectorsArr.getJSONObject(i)
                parsedSectors.add(
                    Sector(
                        ticker = s.optString("ticker", ""),
                        name = s.optString("name", ""),
                        price = s.optDouble("price", 0.0),
                        shares = s.optDouble("shares", 0.0),
                        value = s.optDouble("value", 0.0),
                        currentWeight = s.optDouble("current_weight", 0.0),
                        targetWeight = s.optDouble("target_weight", 9.09),
                        driftPct = s.optDouble("drift_pct", 0.0),
                        absDriftPct = s.optDouble("abs_drift_pct", 0.0),
                        status = s.optString("status", "BALANCED"),
                        isBreached = s.optBoolean("is_breached", false)
                    )
                )
            }

            val ordersArr = portObj.optJSONArray("orders") ?: JSONArray()
            val parsedOrders = mutableListOf<TradeOrder>()
            for (i in 0 until ordersArr.length()) {
                val o = ordersArr.getJSONObject(i)
                parsedOrders.add(
                    TradeOrder(
                        ticker = o.optString("ticker", ""),
                        name = o.optString("name", ""),
                        action = o.optString("action", "HOLD"),
                        currentShares = o.optDouble("current_shares", 0.0),
                        targetShares = o.optDouble("target_shares", 0.0),
                        shareChange = o.optDouble("share_change", 0.0),
                        currentValue = o.optDouble("current_value", 0.0),
                        targetValue = o.optDouble("target_value", 0.0),
                        dollarAmount = o.optDouble("dollar_amount", 0.0),
                        isTriggerSource = o.optBoolean("is_trigger_source", false)
                    )
                )
            }

            _portfolioState.value = PortfolioState(
                portfolioValue = portObj.optDouble("portfolio_value", 100000.0),
                targetPerSector = portObj.optDouble("target_per_sector", 9.09),
                riskProfile = portObj.optString("risk_profile", "Shock Absorber"),
                driftThresholdPct = portObj.optDouble("drift_threshold_pct", 5.0),
                maxDriftPct = portObj.optDouble("max_drift_pct", 0.0),
                status = portObj.optString("status", "BALANCED"),
                rebalanceNeeded = portObj.optBoolean("rebalance_needed", false),
                sectors = parsedSectors,
                orders = parsedOrders,
                turnoverDollars = portObj.optDouble("turnover_dollars", 0.0),
                turnoverPct = portObj.optDouble("turnover_pct", 0.0),
                estimatedFeeDollars = portObj.optDouble("estimated_fee_dollars", 0.0),
                educationalRationale = portObj.optString("educational_rationale", ""),
                updatedAt = timestamp
            )
        }

        // Performance History
        if (dataObj.has("performance_history")) {
            val perfObj = dataObj.getJSONObject("performance_history")
            val alphaBps = perfObj.optDouble("rebalance_alpha_bps", 258.0)
            val alphaPct = perfObj.optDouble("rebalance_alpha_pct", 2.58)
            val historyArr = perfObj.optJSONArray("history") ?: JSONArray()
            val points = mutableListOf<HistoricalPoint>()
            for (i in 0 until historyArr.length()) {
                val pt = historyArr.getJSONObject(i)
                points.add(
                    HistoricalPoint(
                        date = pt.optString("date", ""),
                        rebalance5pct = pt.optDouble("rebalance_5pct", 100.0),
                        rebalance10pct = pt.optDouble("rebalance_10pct", 100.0),
                        unrebalancedDrift = pt.optDouble("unrebalanced_drift", 100.0),
                        spyBenchmark = pt.optDouble("spy_benchmark", 100.0),
                        premiumVsDriftBps = pt.optDouble("premium_vs_drift_bps", 0.0)
                    )
                )
            }

            _performanceData.value = PerformanceData(
                periodDays = perfObj.optInt("period_days", 180),
                rebalanceAlphaBps = alphaBps,
                rebalanceAlphaPct = alphaPct,
                strategies = _performanceData.value.strategies,
                history = if (points.isNotEmpty()) points else _performanceData.value.history,
                updatedAt = timestamp
            )
        }
    }

    private fun formatTimestamp(isoStr: String): String {
        return try {
            val clean = isoStr.replace("Z", "").replace("T", " ")
            if (clean.contains(".")) clean.substringBefore(".") + " UTC" else "$clean UTC"
        } catch (e: Exception) {
            isoStr
        }
    }

    companion object {
        fun createInitialMarketRegime(): MarketRegime {
            return MarketRegime(
                regimeKey = "LOW_VOL_GRIND_UP",
                title = "Low Volatility / Slow Grind Up",
                badgeColorHex = "#10B981",
                recommendation = "100% Invested in Rebalancing Engine",
                exposurePct = 100,
                description = "Bullish, steady compounding environment. Volatility is low and momentum is stable.",
                confidence = 82.0,
                probLowVol = 82.0,
                probChoppy = 13.0,
                probHighVol = 5.0,
                updatedAt = "Connecting to Cloud Run..."
            )
        }

        fun createInitialPortfolioState(driftThreshold: Double): PortfolioState {
            val sectors = listOf(
                Sector("XLB", "Materials", 91.46, 99.40, 9091.12, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLE", "Energy", 88.99, 102.16, 9091.22, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLF", "Financials", 46.30, 196.35, 9091.00, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLI", "Industrials", 129.04, 70.45, 9090.87, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLK", "Technology", 230.59, 39.42, 9089.86, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLP", "Consumer Staples", 80.62, 112.76, 9090.71, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLU", "Utilities", 75.04, 121.15, 9091.10, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLV", "Health Care", 147.69, 61.55, 9090.32, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLY", "Consumer Discretionary", 192.91, 47.13, 9091.85, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLC", "Communication Services", 89.43, 101.65, 9090.56, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
                Sector("XLRE", "Real Estate", 41.60, 218.53, 9090.85, 9.09, 9.09, 0.0, 0.0, "BALANCED", false),
            )

            return PortfolioState(
                portfolioValue = 100000.0,
                targetPerSector = 9.09,
                riskProfile = if (driftThreshold <= 5.0) "Shock Absorber" else "Wealth Compounder",
                driftThresholdPct = driftThreshold,
                maxDriftPct = 0.0,
                status = "BALANCED",
                rebalanceNeeded = false,
                breachedSectors = emptyList(),
                sectors = sectors,
                orders = emptyList(),
                turnoverDollars = 0.0,
                turnoverPct = 0.0,
                estimatedFeeDollars = 0.0,
                educationalRationale = "Portfolio is within disciplined tolerance. Max drift is 0.0%. No action required.",
                updatedAt = "Connecting to Cloud Run..."
            )
        }

        fun createInitialPerformanceData(): PerformanceData {
            val strategies = mapOf(
                "rebalance_10pct" to StrategyMetric("Wealth Compounder (10% Band)", 13.84, -17.40, 113.84),
                "rebalance_5pct" to StrategyMetric("Shock Absorber (5% Band)", 12.92, -16.28, 112.92),
                "unrebalanced_drift" to StrategyMetric("Un-rebalanced Buy & Hold", 11.26, -21.10, 111.26),
                "spy_benchmark" to StrategyMetric("SPY (S&P 500 ETF)", 12.40, -23.93, 112.40)
            )

            val points = (0..20).map { i ->
                val ratio = i / 20.0
                HistoricalPoint(
                    date = "Week $i",
                    rebalance5pct = 100.0 + ratio * 12.92,
                    rebalance10pct = 100.0 + ratio * 13.84,
                    unrebalancedDrift = 100.0 + ratio * 11.26,
                    spyBenchmark = 100.0 + ratio * 12.40,
                    premiumVsDriftBps = ratio * 258.0
                )
            }

            return PerformanceData(
                periodDays = 180,
                rebalanceAlphaBps = 258.0,
                rebalanceAlphaPct = 2.58,
                strategies = strategies,
                history = points
            )
        }
    }
}
