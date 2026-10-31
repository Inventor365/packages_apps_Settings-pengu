/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;
import android.util.Log;

import com.android.settings.R;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** CPU core count and highest core clock, for example "8 cores, up to 4.32 GHz". */
public class CpuInfoPreferenceController extends DeviceSpecPreferenceController {

    private static final String TAG = "CpuInfoPrefCtrl";
    private static final String MAX_FREQ =
            "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq";

    public CpuInfoPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected CharSequence loadSummary() {
        final int cores = Runtime.getRuntime().availableProcessors();
        long maxKhz = 0;
        for (int cpu = 0; cpu < cores; cpu++) {
            maxKhz = Math.max(maxKhz, readKhz(String.format(MAX_FREQ, cpu)));
        }
        if (maxKhz <= 0) {
            return mContext.getResources().getQuantityString(
                    R.plurals.device_spec_cpu_cores, cores, cores);
        }
        final String ghz = String.format("%.2f", maxKhz / 1000000f);
        return mContext.getResources().getQuantityString(
                R.plurals.device_spec_cpu_cores_freq, cores, cores, ghz);
    }

    private static long readKhz(String path) {
        try {
            return Long.parseLong(new String(Files.readAllBytes(Paths.get(path)),
                    StandardCharsets.UTF_8).trim());
        } catch (IOException | NumberFormatException e) {
            Log.d(TAG, "Cannot read " + path, e);
            return 0;
        }
    }
}
