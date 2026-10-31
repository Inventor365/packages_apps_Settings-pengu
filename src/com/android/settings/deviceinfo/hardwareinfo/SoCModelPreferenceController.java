/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.android.settings.R;

/** SoC name: the device overlay name if set, otherwise the SoC manufacturer and model. */
public class SoCModelPreferenceController extends DeviceSpecPreferenceController {

    public SoCModelPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected CharSequence loadSummary() {
        final String name = mContext.getString(R.string.config_device_soc_name);
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        if (TextUtils.isEmpty(Build.SOC_MODEL) || Build.UNKNOWN.equals(Build.SOC_MODEL)) {
            return null;
        }
        if (TextUtils.isEmpty(Build.SOC_MANUFACTURER)
                || Build.UNKNOWN.equals(Build.SOC_MANUFACTURER)) {
            return Build.SOC_MODEL;
        }
        return Build.SOC_MANUFACTURER + " " + Build.SOC_MODEL;
    }
}
