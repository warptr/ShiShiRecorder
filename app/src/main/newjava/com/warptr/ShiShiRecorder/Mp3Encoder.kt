package com.warptr.ShiShiRecorder

/** Thin, stateful wrapper around the bundled LGPL LAME encoder. */
class Mp3Encoder(
    sampleRate: Int = SAMPLE_RATE,
    channels: Int = CHANNELS,
    bitRateKbps: Int,
) : AutoCloseable {
    private var handle = nativeCreate(sampleRate, channels, bitRateKbps)

    fun encode(pcm: ShortArray, sampleCount: Int): ByteArray {
        check(handle != 0L) { "MP3 编码器已关闭" }
        if (sampleCount == 0) return ByteArray(0)
        return nativeEncode(handle, pcm, 0, sampleCount, CHANNELS)
    }

    fun flush(): ByteArray {
        if (handle == 0L) return ByteArray(0)
        return nativeFlush(handle)
    }

    override fun close() {
        if (handle != 0L) {
            nativeClose(handle)
            handle = 0L
        }
    }

    private external fun nativeCreate(sampleRate: Int, channels: Int, bitRateKbps: Int): Long
    private external fun nativeEncode(handle: Long, pcm: ShortArray, offset: Int, sampleCount: Int, channels: Int): ByteArray
    private external fun nativeFlush(handle: Long): ByteArray
    private external fun nativeClose(handle: Long)

    companion object {
        const val SAMPLE_RATE = 48_000
        const val CHANNELS = 2

        init {
            System.loadLibrary("mp3-jni")
        }
    }
}
