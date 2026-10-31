/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import com.android.settings.R;

/** Size, resolution and highest refresh rate of the built-in display. */
public class DisplayInfoPreferenceController extends DeviceSpecPreferenceController {

    public DisplayInfoPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected CharSequence loadSummary() {
        final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
        final Display display = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        if (display == null) {
            return null;
        }
        // The largest mode is the native panel resolution, whatever the user picked.
        int width = 0;
        int height = 0;
        float refresh = 0;
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() * mode.getPhysicalHeight() > width * height) {
                width = mode.getPhysicalWidth();
                height = mode.getPhysicalHeight();
            }
            refresh = Math.max(refresh, mode.getRefreshRate());
        }
        if (width <= 0 || height <= 0) {
            return null;
        }
        final int shortSide = Math.min(width, height);
        final int longSide = Math.max(width, height);
        final int hz = Math.round(refresh);
        final String size = mContext.getString(R.string.config_device_display_size);
        return size.isEmpty()
                ? mContext.getString(R.string.device_spec_display, shortSide, longSide, hz)
                : mContext.getString(R.string.device_spec_display_size, size, shortSide,
                        longSide, hz);
    }
}
