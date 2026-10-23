/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.display;

import static android.hardware.display.DisplayManager.APP_HDR_PLAYBACK_MODE_HDR_ALLOWED;
import static android.hardware.display.DisplayManager.APP_HDR_PLAYBACK_MODE_SDR_ONLY;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * PreferenceController for the per-application HDR Playback Controller screen.
 */
public class HdrPlaybackControllerPreferenceController extends BasePreferenceController {

    private final DisplayManager mDisplayManager;

    public HdrPlaybackControllerPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mDisplayManager = context.getSystemService(DisplayManager.class);
    }

    public static int getDisplayHdrAvailabilityStatus(@NonNull Context context) {
        DisplayManager dm = context.getSystemService(DisplayManager.class);
        if (dm == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        Display[] displays = dm.getDisplays(
                DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED);
        if (displays == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        for (Display display : displays) {
            if (display == null) {
                continue;
            }
            if (display.isHdr() || display.isHdrSdrRatioAvailable()) {
                return AVAILABLE;
            }
            Display.Mode[] modes = display.getSupportedModes();
            if (modes != null) {
                for (Display.Mode mode : modes) {
                    if (mode != null && mode.getSupportedHdrTypes().length > 0) {
                        return AVAILABLE;
                    }
                }
            }
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getAvailabilityStatus() {
        return getDisplayHdrAvailabilityStatus(mContext);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setSummary(getSummary());
        }
    }

    @Override
    public CharSequence getSummary() {
        if (mDisplayManager == null) {
            return mContext.getString(R.string.hdr_playback_controller_summary_default);
        }
        String[] packages = mDisplayManager.getAppHdrPlaybackPackages();
        if (packages == null || packages.length == 0) {
            return mContext.getString(R.string.hdr_playback_controller_summary_default);
        }
        int sdrOnlyCount = 0;
        int hdrAllowedCount = 0;
        for (int i = 0; i < packages.length; i++) {
            String pkg = packages[i];
            int mode = mDisplayManager.getAppHdrPlaybackMode(pkg);
            if (mode == APP_HDR_PLAYBACK_MODE_SDR_ONLY) {
                sdrOnlyCount++;
            } else if (mode == APP_HDR_PLAYBACK_MODE_HDR_ALLOWED) {
                hdrAllowedCount++;
            }
        }
        int total = sdrOnlyCount + hdrAllowedCount;
        if (total == 0) {
            return mContext.getString(R.string.hdr_playback_controller_summary_default);
        }
        return mContext.getString(
                R.string.hdr_playback_controller_summary_custom,
                total, sdrOnlyCount, hdrAllowedCount);
    }
}
