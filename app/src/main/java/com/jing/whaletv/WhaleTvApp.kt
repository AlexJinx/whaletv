package com.jing.whaletv

import android.app.Application
import com.jing.whaletv.core.AppContainer
import com.jing.whaletv.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WhaleTvApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            val settings = container.settingsRepository.settings.first()
            if (settings.autoRefresh) {
                SyncScheduler.schedulePeriodic(
                    context = this@WhaleTvApp,
                    intervalHours = settings.refreshIntervalHours,
                )
            }
            SyncScheduler.enqueueImmediate(this@WhaleTvApp)
        }
    }
}
