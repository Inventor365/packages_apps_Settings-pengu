/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.custom.smartpixel;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

/** Hides Smart Pixels on devices whose display does not need it. */
public class SmartPixelsPreferenceController extends BasePreferenceController {

    public SmartPixelsPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_supportSmartPixels)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
