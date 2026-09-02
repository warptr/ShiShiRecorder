package com.warptr.ShiShiRecorder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class InternalAudioTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        val preferences = getSharedPreferences(RecordingCatalog.PREFERENCES, Context.MODE_PRIVATE)
        if (preferences.getBoolean(RecordingCatalog.KEY_ACTIVE, false) ||
            preferences.getBoolean(RecordingCatalog.KEY_PREPARED, false)) {
            startService(Intent(this, InternalAudioRecordingService::class.java).setAction(InternalAudioRecordingService.ACTION_STOP))
        } else {
            val launchIntent = Intent(this, SimpleMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(SimpleMainActivity.EXTRA_TILE_START, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivityAndCollapseCompat(launchIntent)
            }
        }
        refreshTile()
    }

    private fun refreshTile() {
        qsTile?.apply {
            label = "内部录音"
            val preferences = getSharedPreferences(RecordingCatalog.PREFERENCES, Context.MODE_PRIVATE)
            state = if (preferences.getBoolean(RecordingCatalog.KEY_ACTIVE, false) ||
                preferences.getBoolean(RecordingCatalog.KEY_PREPARED, false)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun startActivityAndCollapseCompat(intent: Intent) {
        startActivityAndCollapse(intent)
    }
}
