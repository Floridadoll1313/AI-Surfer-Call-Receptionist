package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase

class OceanSurferApp : Application() {

  lateinit var database: AppDatabase
    private set

  override fun onCreate() {
    super.onCreate()
    instance = this
    database = AppDatabase.getDatabase(this)
    createNotificationChannels()
    com.example.integrations.SupabaseCallSyncWorker.schedulePeriodicSync(this)
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_CALL_ALERTS,
        "Ocean Tide Call & Escalation Alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Urgent alerts for unreviewed incoming customer calls from Ocean Tide Drop"
        enableVibration(true)
        enableLights(true)
      }

      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  companion object {
    const val CHANNEL_CALL_ALERTS = "ocean_tide_call_alerts"
    lateinit var instance: OceanSurferApp
      private set
  }
}
