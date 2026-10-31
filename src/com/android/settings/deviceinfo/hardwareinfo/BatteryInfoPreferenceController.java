/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;

import com.android.internal.os.PowerProfile;
import com.android.settings.R;

/** Rated battery capacity from the device power profile. */
public class BatteryInfoPreferenceController extends DeviceSpecPreferenceController {

    public BatteryInfoPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected CharSequence loadSummary() {
        final double mah = new PowerProfile(mContext).getBatteryCapacity();
        // The AOSP default power profile reports a placeholder capacity.
        if (mah <= 1000) {
            return null;
        }
        return mContext.getString(R.string.device_spec_battery, Math.round(mah));
    }
}
