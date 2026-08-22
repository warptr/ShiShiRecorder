package com.yepgoryo.CaptureCap

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.AttributeSet
import androidx.preference.ListPreference

class CodecList(context: Context, attributeSet: AttributeSet?): ListPreference(context, attributeSet) {
    private val codecsList: ArrayList<String> = ArrayList()
    private val prefName: String = key
    private val appSettings = GlobalProperties(context)

    private fun getAllCodecs() {
        codecsList.clear()
        val type: String = if (this.prefName == appSettings.getStringPropertyName(GlobalProperties.PropertiesString.AUDIO_CODEC_VALUE)) {
            val audioFormatValue = appSettings!!.getStringProperty(GlobalProperties.PropertiesString.AUDIO_FORMAT_VALUE, context.getResources().getString(R.string.audio_format_option_auto_value))
            if (audioFormatValue == context.getResources().getString(R.string.audio_format_option_auto_value)) {
                MediaFormat.MIMETYPE_AUDIO_AAC
            } else {
                audioFormatValue
            }
        } else {
            val videoFormatValue = appSettings!!.getStringProperty(GlobalProperties.PropertiesString.FORMAT_VALUE, context.getResources().getString(R.string.format_option_auto_value))
            if (videoFormatValue == context.getResources().getString(R.string.format_option_auto_value)) {
                MediaFormat.MIMETYPE_VIDEO_AVC
            } else {
                videoFormatValue
            }
        }
        for (mediaCodecInfo in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) {
            if (mediaCodecInfo.isEncoder) {
                for (codecType in mediaCodecInfo.getSupportedTypes()) {
                    if (!this.codecsList.contains(mediaCodecInfo.name) && codecType.equals(type, true)) {
                        this.codecsList.add(mediaCodecInfo.name)
                    }
                }
            }
        }
    }

    init {
        getAllCodecs()
        val entries: Array<String> = arrayOf(context.getResources().getString(R.string.codec_option_auto)) + codecsList
        val values: Array<String> = arrayOf(context.getResources().getString(R.string.codec_option_auto_value)) + codecsList
        setEntries(entries)
        entryValues = values
        setDefaultValue(context.getResources().getString(R.string.audio_codec_option_auto_value))
    }

    override fun onClick() {
        super.onClick()
        getAllCodecs()
        val entries: Array<String> = arrayOf(context.getResources().getString(R.string.codec_option_auto)) + codecsList
        val values: Array<String> = arrayOf(context.getResources().getString(R.string.codec_option_auto_value)) + codecsList
        setEntries(entries)
        entryValues = values
        setDefaultValue(context.getResources().getString(R.string.audio_codec_option_auto_value))
    }
}
