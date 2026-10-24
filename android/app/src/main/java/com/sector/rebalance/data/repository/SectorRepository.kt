package com.sector.rebalance.data.repository

import android.util.Log
import com.sector.rebalance.data.model.HistoricalPoint
import com.sector.rebalance.data.model.MarketRegime
import com.sector.rebalance.data.model.PortfolioState
import com.sector.rebalance.data.model.PerformanceData
import com.sector.rebalance.data.model.Sector
import com.sector.rebalance.data.model.StrategyMetric
import com.sector.rebalance.data.model.TradeOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

interface SectorRepository {
    val marketRegime: Flow<MarketRegime>
    val portfolioState: Flow<PortfolioState>
    val performanceData: Flow<PerformanceData>
    val isLiveSync: Flow<Boolean>
    val lastSyncTime: Flow<String>
    val syncSource: Flow<String>

    suspend fun refreshData()
    suspend fun updateRiskProfile(profileKey: String)
    suspend fun fetchFromCloudRunUrl(baseUrl: String): Result<String>
}

class FirestoreSectorRepository : SectorRepository {

    private val _marketRegime = MutableStateFlow(createSampleMarketRegime())
    override val marketRegime: Flow<MarketRegime> = _marketRegime.asStateFlow()

    private val _portfolioState = MutableStateFlow(createSamplePortfolioState(5.0))
    override val portfolioState: Flow<PortfolioState> = _portfolioState.asStateFlow()

    private val _performanceData = MutableStateFlow(createSamplePerformanceData())
    override val performanceData: Flow<PerformanceData> = _performanceData.asStateFlow()

    private val _isLiveSync = MutableStateFlow(false)
    override val isLiveSync: Flow<Boolean> = _isLiveSync.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Demo Data (Not Connected)")
    override val lastSyncTime: Flow<String> = _lastSyncTime.asStateFlow()

    private val _syncSource = MutableStateFlow("Offline Sample Data")
    override val syncSource: Flow<String> = _syncSource.asStateFlow()

    init {
        tryInitFirestore()
    }

    private fun tryInitFirestore() {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            Log.d("SectorRepository", "Connecting to Firebase Firestore instance...")

            // 1. Listen to Market Regime
            firestore.collection("market_regime").document("daily_state")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w("SectorRepository", "Firestore market_regime error: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
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
                        confidence = (data["confidence"] as? Double) ?: 85.0,
                        probLowVol = ((data["probabilities"] as? Map<*, *>)?.get("LOW_VOL_GRIND_UP") as? Number)?.toDouble() ?: 80.0,
                        probChoppy = ((data["probabilities"] as? Map<*, *>)?.get("CHOPPY_SIDEWAYS") as? Number)?.toDouble() ?: 15.0,
                        probHighVol = ((data["probabilities"] as? Map<*, *>)?.get("HIGH_VOL_CRASH") as? Number)?.toDouble() ?: 5.0,
                        updatedAt = updateTime
                    )
                    Log.i("SectorRepository", "Loaded live Market Regime from Firestore! Update time: $updateTime")
                }

            // 2. Listen to Portfolio State
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
                    Log.i("SectorRepository", "Loaded live Portfolio State from Firestore! Status: ${_portfolioState.value.status}")
                }

        } catch (e: Exception) {
            Log.w("SectorRepository", "Firebase Firestore is not initialized (${e.message}). Falling back to sample demo data.")
            _isLiveSync.value = false
            _syncSource.value = "Demo Data (google-services.json not installed)"
        }
    }

    override suspend fun refreshData() {
        // Triggers refresh
    }

    override suspend fun updateRiskProfile(profileKey: String) {
        val threshold = if (profileKey == "WEALTH_COMPOUNDER") 10.0 else 5.0
        if (!_isLiveSync.value) {
            _portfolioState.value = createSamplePortfolioState(threshold)
        }
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

            Log.d("SectorRepository", "Fetching live data from Cloud Run URL: $cleanUrl")
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
            _syncSource.value = "Cloud Run HTTP Endpoint"

            Result.success("Connected successfully to Cloud Run! Live calculations updated.")
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
                confidence = regObj.optDouble("confidence", 85.0),
                probLowVol = probsObj?.optDouble("LOW_VOL_GRIND_UP", 80.0) ?: 80.0,
                probChoppy = probsObj?.optDouble("CHOPPY_SIDEWAYS", 15.0) ?: 15.0,
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
    }

    private fun formatTimestamp(isoStr: String): String {
        return try {
            // Converts 2026-10-24T14:30:15.123456Z into readable format
            val clean = isoStr.replace("Z", "").replace("T", " ")
            if (clean.contains(".")) clean.substringBefore(".") + " UTC" else "$clean UTC"
        } catch (e: Exception) {
            isoStr
        }
    }

    companion object {
        fun createSampleMarketRegime(): MarketRegime {
            return MarketRegime(
                regimeKey = "LOW_VOL_GRIND_UP",
                title = "Low Volatility / Slow Grind Up",
                badgeColorHex = "#10B981",
                recommendation = "100% Invested in Rebalancing Engine",
                exposurePct = 100,
                description = "Bullish, steady compounding environment. Volatility is low and cross-sectional trend is stable.",
                confidence = 82.0,
                probLowVol = 82.0,
                probChoppy = 13.0,
                probHighVol = 5.0,
                updatedAt = "Demo Data (Hardcoded Sample)"
            )
        }

        fun createSamplePortfolioState(driftThreshold: Double): PortfolioState {
            val isShock = driftThreshold <= 5.0
            val xlkWeight = 14.85
            val xlkDrift = xlkWeight - 9.09
            val xlkBreached = xlkDrift >= driftThreshold

            val sectors = listOf(
                Sector("XLK", "Technology", 232.10, 63.98, 14850.0, 14.85, 9.09, +5.76, 5.76, if (xlkBreached) "REBALANCE_TRIGGERED" else "WATCH", xlkBreached),
                Sector("XLV", "Health Care", 147.60, 61.65, 9100.0, 9.10, 9.09, +0.01, 0.01, "BALANCED", false),
                Sector("XLF", "Financials", 46.80, 209.40, 9800.0, 9.80, 9.09, +0.71, 0.71, "BALANCED", false),
                Sector("XLI", "Industrials", 128.40, 71.65, 9200.0, 9.20, 9.09, +0.11, 0.11, "BALANCED", false),
                Sector("XLY", "Cons. Discretionary", 194.50, 48.32, 9400.0, 9.40, 9.09, +0.31, 0.31, "BALANCED", false),
                Sector("XLC", "Communication", 88.70, 102.59, 9100.0, 9.10, 9.09, +0.01, 0.01, "BALANCED", false),
                Sector("XLP", "Cons. Staples", 79.90, 107.63, 8600.0, 8.60, 9.09, -0.49, 0.49, "BALANCED", false),
                Sector("XLE", "Energy", 89.20, 78.47, 7000.0, 7.00, 9.09, -2.09, 2.09, "WATCH", false),
                Sector("XLU", "Utilities", 74.30, 102.28, 7600.0, 7.60, 9.09, -1.49, 1.49, "WATCH", false),
                Sector("XLB", "Materials", 91.50, 85.24, 7800.0, 7.80, 9.09, -1.29, 1.29, "BALANCED", false),
                Sector("XLRE", "Real Estate", 42.10, 179.33, 7550.0, 7.55, 9.09, -1.54, 1.54, "WATCH", false),
            )

            val orders = listOf(
                TradeOrder("XLK", "Technology", "SELL", 63.98, 39.16, 24.82, 14850.0, 9090.0, 5760.0, true),
                TradeOrder("XLE", "Energy", "BUY", 78.47, 101.91, 23.44, 7000.0, 9090.0, 2090.0, false),
                TradeOrder("XLRE", "Real Estate", "BUY", 179.33, 215.91, 36.58, 7550.0, 9090.0, 1540.0, false),
                TradeOrder("XLU", "Utilities", "BUY", 102.28, 122.34, 20.06, 7600.0, 9090.0, 1490.0, false),
                TradeOrder("XLB", "Materials", "BUY", 85.24, 99.34, 14.10, 7800.0, 9090.0, 1290.0, false),
            )

            return PortfolioState(
                portfolioValue = 100000.0,
                targetPerSector = 9.09,
                riskProfile = if (isShock) "Shock Absorber" else "Wealth Compounder",
                driftThresholdPct = driftThreshold,
                maxDriftPct = 5.76,
                status = if (xlkBreached) "REBALANCE_TRIGGERED" else "WATCH",
                rebalanceNeeded = xlkBreached,
                breachedSectors = if (xlkBreached) listOf("XLK") else emptyList(),
                sectors = sectors,
                orders = if (xlkBreached) orders else emptyList(),
                turnoverDollars = if (xlkBreached) 11520.0 else 0.0,
                turnoverPct = if (xlkBreached) 11.52 else 0.0,
                estimatedFeeDollars = if (xlkBreached) 2.30 else 0.0,
                educationalRationale = if (xlkBreached) {
                    "Shannon's Demon Rebalance Triggered! XLK (Technology) has expanded to 14.85% (+5.76% drift), breaching your 5.0% threshold. " +
                    "Rebalancing locks in gains from outperforming Tech to accumulate lagging Energy and Real Estate, capturing the systematic Rebalancing Premium."
                } else {
                    "Portfolio is within disciplined 10% bounds. Max drift is 5.76% (XLK). No action required; letting winners run to compound."
                },
                updatedAt = "Demo Data (Hardcoded Sample)"
            )
        }

        fun createSamplePerformanceData(): PerformanceData {
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
