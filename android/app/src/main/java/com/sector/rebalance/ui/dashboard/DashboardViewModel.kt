package com.sector.rebalance.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sector.rebalance.data.model.MarketRegime
import com.sector.rebalance.data.model.PortfolioState
import com.sector.rebalance.data.repository.SectorRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: SectorRepository
) : ViewModel() {

    val marketRegime: StateFlow<MarketRegime> = repository.marketRegime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MarketRegime())

    val portfolioState: StateFlow<PortfolioState> = repository.portfolioState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioState())

    val isLiveSync: StateFlow<Boolean> = repository.isLiveSync
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastSyncTime: StateFlow<String> = repository.lastSyncTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Demo Data (Not Connected)")

    val syncSource: StateFlow<String> = repository.syncSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Offline Sample Data")

    fun refresh() {
        viewModelScope.launch {
            repository.refreshData()
        }
    }

    fun setRiskProfile(profileKey: String) {
        viewModelScope.launch {
            repository.updateRiskProfile(profileKey)
        }
    }

    fun syncFromCloudRun(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.fetchFromCloudRunUrl(url)
            if (result.isSuccess) {
                onResult(true, result.getOrNull() ?: "Sync successful!")
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Sync failed")
            }
        }
    }
}
