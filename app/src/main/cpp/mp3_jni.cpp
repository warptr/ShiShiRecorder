#include <jni.h>
#include <android/log.h>
#include <memory>
#include <vector>

extern "C" {
#include "lame.h"
}

namespace {
constexpr const char* TAG = "ShiShiRecorderMp3";

void throwIllegalState(JNIEnv* env, const char* message) {
    jclass type = env->FindClass("java/lang/IllegalStateException");
    if (type != nullptr) {
        env->ThrowNew(type, message);
    }
}

lame_t fromHandle(jlong handle) {
    return reinterpret_cast<lame_t>(handle);
}

jbyteArray encodeResult(JNIEnv* env, int written, const std::vector<unsigned char>& output) {
    if (written < 0) {
        throwIllegalState(env, "MP3 编码失败");
        return nullptr;
    }
    jbyteArray result = env->NewByteArray(written);
    if (result != nullptr && written > 0) {
        env->SetByteArrayRegion(result, 0, written, reinterpret_cast<const jbyte*>(output.data()));
    }
    return result;
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_warptr_ShiShiRecorder_Mp3Encoder_nativeCreate(
        JNIEnv* env,
        jobject,
        jint sampleRate,
        jint channels,
        jint bitRateKbps) {
    if (sampleRate != 48000 || (channels != 1 && channels != 2)) {
        throwIllegalState(env, "不支持的 MP3 音频参数");
        return 0;
    }
    if (bitRateKbps != 128 && bitRateKbps != 192 && bitRateKbps != 256 && bitRateKbps != 320) {
        throwIllegalState(env, "不支持的 MP3 码率");
        return 0;
    }
    lame_t encoder = lame_init();
    if (encoder == nullptr) {
        throwIllegalState(env, "无法初始化 MP3 编码器");
        return 0;
    }
    lame_set_in_samplerate(encoder, sampleRate);
    lame_set_out_samplerate(encoder, sampleRate);
    lame_set_num_channels(encoder, channels);
    lame_set_brate(encoder, bitRateKbps);
    lame_set_quality(encoder, 2);
    lame_set_VBR(encoder, vbr_off);
    lame_set_bWriteVbrTag(encoder, 0);
    lame_set_write_id3tag_automatic(encoder, 0);
    if (lame_init_params(encoder) < 0) {
        lame_close(encoder);
        throwIllegalState(env, "MP3 编码参数初始化失败");
        return 0;
    }
    return reinterpret_cast<jlong>(encoder);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_warptr_ShiShiRecorder_Mp3Encoder_nativeEncode(
        JNIEnv* env,
        jobject,
        jlong handle,
        jshortArray pcm,
        jint offset,
        jint sampleCount,
        jint channels) {
    lame_t encoder = fromHandle(handle);
    if (encoder == nullptr || pcm == nullptr || offset < 0 || sampleCount < 0 || (channels != 1 && channels != 2)) {
        throwIllegalState(env, "MP3 编码器状态无效");
        return nullptr;
    }
    const jsize length = env->GetArrayLength(pcm);
    if (offset > length || sampleCount > length - offset || sampleCount % channels != 0) {
        throwIllegalState(env, "PCM 数据长度无效");
        return nullptr;
    }
    const int samplesPerChannel = sampleCount / channels;
    std::vector<unsigned char> output(static_cast<size_t>(1.25 * samplesPerChannel + 7200));
    jshort* input = env->GetShortArrayElements(pcm, nullptr);
    if (input == nullptr) {
        return nullptr;
    }
    int written;
    if (channels == 2) {
        written = lame_encode_buffer_interleaved(encoder, input + offset, samplesPerChannel, output.data(), static_cast<int>(output.size()));
    } else {
        written = lame_encode_buffer(encoder, input + offset, input + offset, samplesPerChannel, output.data(), static_cast<int>(output.size()));
    }
    env->ReleaseShortArrayElements(pcm, input, JNI_ABORT);
    return encodeResult(env, written, output);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_warptr_ShiShiRecorder_Mp3Encoder_nativeFlush(JNIEnv* env, jobject, jlong handle) {
    lame_t encoder = fromHandle(handle);
    if (encoder == nullptr) {
        return env->NewByteArray(0);
    }
    std::vector<unsigned char> output(7200);
    return encodeResult(env, lame_encode_flush(encoder, output.data(), static_cast<int>(output.size())), output);
}

extern "C" JNIEXPORT void JNICALL
Java_com_warptr_ShiShiRecorder_Mp3Encoder_nativeClose(JNIEnv*, jobject, jlong handle) {
    lame_t encoder = fromHandle(handle);
    if (encoder != nullptr) {
        lame_close(encoder);
    }
}
