package com.warptr.CaptureCapMP3

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class InternalAudioTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        val preferences = getSharedPreferences(RecordingCatalog.PREFERENCES, Context.MODE_PRIVATE)
        if (preferences.getBoolean(RecordingCatalog.KEY_ACTIVE, false)) {
            startService(Intent(this, InternalAudioRecordingService::class.java).setAction(InternalAudioRecordingService.ACTION_STOP))
        } else {
            startActivityAndCollapse(Intent(this, SimpleMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(SimpleMainActivity.EXTRA_TILE_START, true)
            })
        }
        refreshTile()
    }

    private fun refreshTile() {
        qsTile?.apply {
            label = "内部录音"
            state = if (getSharedPreferences(RecordingCatalog.PREFERENCES, Context.MODE_PRIVATE)
                    .getBoolean(RecordingCatalog.KEY_ACTIVE, false)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
