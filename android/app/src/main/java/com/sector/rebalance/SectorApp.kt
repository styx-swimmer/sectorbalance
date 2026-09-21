package com.sector.rebalance

import android.app.Application
import com.sector.rebalance.data.repository.FirestoreSectorRepository
import com.sector.rebalance.data.repository.SectorRepository

class SectorApp : Application() {
    lateinit var repository: SectorRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FirestoreSectorRepository()
    }
}
