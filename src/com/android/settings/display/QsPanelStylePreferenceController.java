/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * The Quick Settings style picker, held while One UI Quick Settings is on: that experiment
 * overrides every style without changing the one picked here, which comes back once it is off.
 */
public class QsPanelStylePreferenceController extends BasePreferenceController {

    private static final String ONE_UI_SETTING = "qs_oneui_experimental";

    public QsPanelStylePreferenceController(Context context, String key) {
        super(context, key);
    }

    private boolean isOneUiOn() {
        return Settings.Secure.getInt(mContext.getContentResolver(), ONE_UI_SETTING, 0) != 0;
    }

    @Override
    public int getAvailabilityStatus() {
        return isOneUiOn() ? DISABLED_DEPENDENT_SETTING : AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (isOneUiOn()) {
            preference.setSummary(R.string.qs_panel_style_oneui_active);
        } else if (preference instanceof ListPreference) {
            preference.setSummary("%s");
        }
    }
}
