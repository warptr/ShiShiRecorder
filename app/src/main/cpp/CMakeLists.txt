cmake_minimum_required(VERSION 3.22.1)
project("shishirecorder_mp3")

set(LAME_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/lame/jni)

add_library(mp3lame STATIC
    ${LAME_ROOT}/libmp3lame/bitstream.c
    ${LAME_ROOT}/libmp3lame/encoder.c
    ${LAME_ROOT}/libmp3lame/fft.c
    ${LAME_ROOT}/libmp3lame/gain_analysis.c
    ${LAME_ROOT}/libmp3lame/id3tag.c
    ${LAME_ROOT}/libmp3lame/lame.c
    ${LAME_ROOT}/libmp3lame/mpglib_interface.c
    ${LAME_ROOT}/libmp3lame/newmdct.c
    ${LAME_ROOT}/libmp3lame/presets.c
    ${LAME_ROOT}/libmp3lame/psymodel.c
    ${LAME_ROOT}/libmp3lame/quantize.c
    ${LAME_ROOT}/libmp3lame/quantize_pvt.c
    ${LAME_ROOT}/libmp3lame/reservoir.c
    ${LAME_ROOT}/libmp3lame/set_get.c
    ${LAME_ROOT}/libmp3lame/tables.c
    ${LAME_ROOT}/libmp3lame/takehiro.c
    ${LAME_ROOT}/libmp3lame/util.c
    ${LAME_ROOT}/libmp3lame/VbrTag.c
    ${LAME_ROOT}/libmp3lame/vbrquantize.c
    ${LAME_ROOT}/libmp3lame/version.c
)

target_include_directories(mp3lame PUBLIC ${LAME_ROOT}/include)
target_include_directories(mp3lame PRIVATE ${LAME_ROOT}/libmp3lame)
target_compile_definitions(mp3lame PRIVATE STDC_HEADERS HAVE_STDINT_H HAVE_INTTYPES_H HAVE_ERRNO_H HAVE_FCNTL_H)

add_library(mp3-jni SHARED mp3_jni.cpp)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(mp3-jni mp3lame ${log-lib} ${android-lib} m)
