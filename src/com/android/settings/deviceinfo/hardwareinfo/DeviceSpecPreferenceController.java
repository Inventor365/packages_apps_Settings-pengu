/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Base for the read-only device specification rows of the Model and hardware page. */
public abstract class DeviceSpecPreferenceController extends BasePreferenceController {

    private CharSequence mSummary;
    private boolean mLoaded;

    public DeviceSpecPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /** Returns the specification text, or null when the device does not report it. */
    protected abstract CharSequence loadSummary();

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_device_model)
                && !TextUtils.isEmpty(getSummary()) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        if (!mLoaded) {
            mSummary = loadSummary();
            mLoaded = true;
        }
        return mSummary;
    }
}
