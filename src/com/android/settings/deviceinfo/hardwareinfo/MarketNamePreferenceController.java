/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;

/** Marketing name of the device, such as "OnePlus 13". */
public class MarketNamePreferenceController extends DeviceSpecPreferenceController {

    private static final String[] PROPS = {
        "ro.product.marketname",
        "ro.product.vendor.marketname",
        "ro.vendor.oplus.market.name",
        "ro.vendor.oplus.market.enname",
    };

    public MarketNamePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected CharSequence loadSummary() {
        for (String prop : PROPS) {
            final String name = SystemProperties.get(prop);
            if (!TextUtils.isEmpty(name)) {
                return name;
            }
        }
        return Build.MODEL;
    }
}
