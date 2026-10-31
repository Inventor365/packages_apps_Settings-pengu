/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;

import com.android.settings.R;

/** Camera summary, only shown when the device overlay describes its cameras. */
public class CameraInfoPreferenceController extends DeviceSpecPreferenceController {

    public CameraInfoPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected CharSequence loadSummary() {
        return mContext.getString(R.string.config_device_camera_spec);
    }
}
