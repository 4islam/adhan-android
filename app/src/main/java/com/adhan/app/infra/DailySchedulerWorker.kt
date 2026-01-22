package com.adhan.app.infra

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailySchedulerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerScheduler: com.adhan.app.domain.PrayerScheduler,
    private val logRepository: com.adhan.app.domain.LogRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            logRepository.log("DailySchedulerWorker: Starting daily maintenance.")
            prayerScheduler.scheduleAlarmsForNext24Hours()
            logRepository.log("DailySchedulerWorker: Maintenance complete.")
            Result.success()
        } catch (e: Exception) {
            logRepository.log("DailySchedulerWorker: Maintenance failed ${e.message}", true)
            Result.retry()
        }
    }
}
