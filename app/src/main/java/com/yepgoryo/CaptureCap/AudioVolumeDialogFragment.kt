package com.yepgoryo.CaptureCap

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat

class AudioVolumeDialogFragment : PreferenceDialogFragmentCompat() {
    companion object {
        fun newInstance(str: String): AudioVolumeDialogFragment {
            val audioVolumeDialogFragment = AudioVolumeDialogFragment()
            val bundle = Bundle(1)
            bundle.putString("key", str)
            audioVolumeDialogFragment.setArguments(bundle)
            return audioVolumeDialogFragment
        }
    }

    private var appSettings: GlobalProperties? = null
    private var keyName: String = ""
    private var audioVolumeScale: Int = 100
    private var micVolumeScale: Int = 100

    fun setKeyName(str: String) {
        this.keyName = str
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        this.appSettings = GlobalProperties(requireContext())
    }

    public override fun onBindDialogView(view: View) {
        this.audioVolumeScale = this.appSettings!!.getIntProperty(GlobalProperties.PropertiesInt.AUDIO_VOLUME, 100)
        this.micVolumeScale = this.appSettings!!.getIntProperty(GlobalProperties.PropertiesInt.MICROPHONE_VOLUME, 100)

        val audioVolume: TextView = view.findViewById(R.id.audio_volume_value)
        val audioSeekBar: SeekBar = view.findViewById(R.id.audio_volume_seek)
        audioSeekBar.progress = this.audioVolumeScale
        audioVolume.text = this.audioVolumeScale.toString()
        audioSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                this@AudioVolumeDialogFragment.audioVolumeScale = progress
                audioVolume.text = this@AudioVolumeDialogFragment.audioVolumeScale.toString()
            }
        })

        val micVolume: TextView = view.findViewById(R.id.microphone_volume_value)
        val micSeekBar: SeekBar = view.findViewById(R.id.microphone_volume_seek)
        micSeekBar.progress = this.micVolumeScale
        micVolume.text = this.micVolumeScale.toString()
        micSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                this@AudioVolumeDialogFragment.micVolumeScale = progress
                micVolume.text = this@AudioVolumeDialogFragment.micVolumeScale.toString()
            }
        })
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            this.appSettings?.setIntProperty(GlobalProperties.PropertiesInt.AUDIO_VOLUME, this.audioVolumeScale)
            this.appSettings?.setIntProperty(GlobalProperties.PropertiesInt.MICROPHONE_VOLUME, this.micVolumeScale)
        }
    }
}
