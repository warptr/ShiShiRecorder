package com.yepgoryo.CaptureCap

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.AttributeSet
import androidx.preference.ListPreference

class FormatList(context: Context, attributeSet: AttributeSet?): ListPreference(context, attributeSet) {
    private val formatList: ArrayList<String> = ArrayList()
    private val formatListValues: ArrayList<String> = ArrayList()
    private val prefName: String = key
    private val appSettings = GlobalProperties(context)
    private val allowedFormats = listOf(
        MediaFormat.MIMETYPE_VIDEO_AVC,
        MediaFormat.MIMETYPE_VIDEO_HEVC,

        MediaFormat.MIMETYPE_AUDIO_AAC,
    )

    private fun getAllFormats() {
        val type: String = if (this.prefName == appSettings.getStringPropertyName(GlobalProperties.PropertiesString.AUDIO_FORMAT_VALUE)) {"audio"} else {"video"}
        for (mediaCodecInfo in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) {
            if (mediaCodecInfo.isEncoder) {
                for (codecType in mediaCodecInfo.getSupportedTypes()) {
                    if (!this.formatListValues.contains(codecType) && codecType.startsWith(type) && allowedFormats.contains(codecType)) {
                        var codecTypeName = codecType
                        when (codecTypeName) {
                            MediaFormat.MIMETYPE_VIDEO_AVC -> {
                                codecTypeName = "H.264"
                            }
                            MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                                codecTypeName = "H.265"
                            }
                            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                                codecTypeName = "AAC"
                            }
                        }
                        this.formatList.add(codecTypeName)
                        this.formatListValues.add(codecType)
                    }
                }
            }
        }
    }

    init {
        getAllFormats()
        val entries: Array<String> = arrayOf(context.getResources().getString(R.string.format_option_auto)) + formatList
        val values: Array<String> = arrayOf(context.getResources().getString(R.string.format_option_auto_value)) + formatListValues
        setEntries(entries)
        entryValues = values
        setDefaultValue(context.getResources().getString(R.string.audio_format_option_auto_value))
    }
}
