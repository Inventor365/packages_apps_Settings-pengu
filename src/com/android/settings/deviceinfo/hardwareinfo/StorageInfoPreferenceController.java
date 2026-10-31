/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;
import android.os.storage.StorageManager;

import com.android.settings.R;

/** Total internal storage, as marketed (StorageManager rounds it to a standard size). */
public class StorageInfoPreferenceController extends DeviceSpecPreferenceController {

    private static final long GB = 1000L * 1000L * 1000L;

    public StorageInfoPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected CharSequence loadSummary() {
        final StorageManager sm = mContext.getSystemService(StorageManager.class);
        final long bytes = sm != null ? sm.getPrimaryStorageSize() : 0;
        if (bytes <= 0) {
            return null;
        }
        final long gb = Math.round((double) bytes / GB);
        return gb >= 1000 && gb % 1000 == 0
                ? mContext.getString(R.string.device_spec_tb, gb / 1000)
                : mContext.getString(R.string.device_spec_gb, gb);
    }
}
