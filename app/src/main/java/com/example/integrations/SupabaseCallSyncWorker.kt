package com.example.integrations

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.OceanSurferApp
import com.example.data.AppRepository
import com.example.data.local.AppDatabase
import java.util.concurrent.TimeUnit

class SupabaseCallSyncWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val db = (applicationContext as? OceanSurferApp)?.database ?: AppDatabase.getDatabase(applicationContext)
    val repo = AppRepository(db)
    val config = IntegrationPrefsStore.loadSupabaseConfig(applicationContext)
    if (!config.autoSyncCalls) return Result.success()

    var pendingFailure = false
    val unsynced = repo.getUnsyncedCalls()
    for (call in unsynced) {
      val uploadResult = SupabaseClient.syncCallRecord(config, call)
      if (uploadResult.isSuccess) {
        repo.markSupabaseSynced(call.id)
      } else {
        pendingFailure = true
      }
    }

    val pullResult = SupabaseClient.fetchCalls(config)
    val remoteCalls = pullResult.getOrNull()
    if (remoteCalls != null) {
      repo.reconcileRemoteCalls(remoteCalls)
    }

    return if (pendingFailure && runAttemptCount < 5) Result.retry() else Result.success()
  }

  companion object {
    private const val IMMEDIATE_WORK_NAME = "ocean_surfer_supabase_immediate_sync"
    private const val PERIODIC_WORK_NAME = "ocean_surfer_supabase_periodic_sync"

    fun enqueueImmediateSync(context: Context) {
      try {
        val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()
        val request = OneTimeWorkRequestBuilder<SupabaseCallSyncWorker>()
          .setConstraints(constraints)
          .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
          .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
          IMMEDIATE_WORK_NAME,
          ExistingWorkPolicy.REPLACE,
          request
        )
      } catch (_: Exception) {
        // Safe no-op when WorkManager is uninitialized in isolated unit tests
      }
    }

    fun schedulePeriodicSync(context: Context) {
      try {
        val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()
        val request = PeriodicWorkRequestBuilder<SupabaseCallSyncWorker>(15, TimeUnit.MINUTES)
          .setConstraints(constraints)
          .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
          PERIODIC_WORK_NAME,
          ExistingPeriodicWorkPolicy.KEEP,
          request
        )
      } catch (_: Exception) {
        // Safe no-op in isolated unit test environments
      }
    }
  }
}
