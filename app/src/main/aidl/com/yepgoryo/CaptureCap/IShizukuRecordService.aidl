package com.yepgoryo.CaptureCap;

import android.os.ParcelFileDescriptor;

interface IShizukuRecordService {
    ParcelFileDescriptor startRecording(
        String audioSource,
        String serverPath
    ) = 1;

    void stopRecording() = 2;

    void destroy() = 16777114;
}
