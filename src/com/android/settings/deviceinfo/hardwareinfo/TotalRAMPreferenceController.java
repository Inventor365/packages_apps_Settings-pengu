/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;
import android.os.Process;

import com.android.settings.R;

/** Installed RAM, rounded up to the marketed size (the kernel reserves part of it). */
public class TotalRAMPreferenceController extends DeviceSpecPreferenceController {

    private static final int[] SIZES_GB = {1, 2, 3, 4, 6, 8, 10, 12, 16, 18, 20, 24, 32, 48, 64};

    public TotalRAMPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected CharSequence loadSummary() {
        final long bytes = Process.getTotalMemory();
        if (bytes <= 0) {
            return null;
        }
        final double gib = bytes / (1024.0 * 1024.0 * 1024.0);
        int size = (int) Math.ceil(gib);
        for (int candidate : SIZES_GB) {
            if (candidate >= gib) {
                size = candidate;
                break;
            }
        }
        return mContext.getString(R.string.device_spec_gb, size);
    }
}
