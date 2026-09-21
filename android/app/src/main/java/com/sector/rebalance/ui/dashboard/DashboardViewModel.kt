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
}
