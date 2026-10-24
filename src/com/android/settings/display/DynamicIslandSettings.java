/*
 * Copyright (C) 2024-2026 Lunaris AOSP
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

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class DynamicIslandSettings extends SettingsPreferenceFragment {

    private static final String KEY_SYSTEM_BLUR_STYLE = "penguin_blur_style";
    private static final String KEY_LIQUID_GLASS = "penguin_liquid_glass";
    private static final String KEY_POPUP_COLOR_MODE =
            "status_bar_dynamic_island_popup_color_mode";

    private static final int BLUR_STYLE_STANDARD = 0;
    private static final int BLUR_STYLE_IOS_FROSTED = 1;
    private static final int BLUR_STYLE_LIQUID_GLASS = 2;

    private static final int POPUP_MODE_BLUR = 1;
    private static final int POPUP_MODE_LIQUID_GLASS = 3;

    private ListPreference mSystemBlurStylePref;
    private ListPreference mPopupColorModePref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.dynamic_island_settings);

        mSystemBlurStylePref = findPreference(KEY_SYSTEM_BLUR_STYLE);
        mPopupColorModePref = findPreference(KEY_POPUP_COLOR_MODE);

        if (mSystemBlurStylePref != null) {
            updateSystemBlurStylePreference();
            mSystemBlurStylePref.setOnPreferenceChangeListener((preference, newValue) -> {
                int style = parseInt(String.valueOf(newValue), BLUR_STYLE_IOS_FROSTED);
                writeSystemBlurStyle(style);
                updateSystemBlurStyleSummary(style);
                return true;
            });
        }

        if (mPopupColorModePref != null) {
            mPopupColorModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                int mode = parseInt(String.valueOf(newValue), 0);
                if (mode == POPUP_MODE_LIQUID_GLASS) {
                    writeSystemBlurStyle(BLUR_STYLE_LIQUID_GLASS);
                    updateSystemBlurStylePreference();
                } else if (mode == POPUP_MODE_BLUR) {
                    writeSystemBlurStyle(BLUR_STYLE_IOS_FROSTED);
                    updateSystemBlurStylePreference();
                }
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSystemBlurStylePreference();
    }

    private int readSystemBlurStyle() {
        ContentResolver cr = getContentResolver();
        boolean lg = Settings.Secure.getInt(cr, KEY_LIQUID_GLASS, 0) != 0;
        int defaultStyle = lg ? BLUR_STYLE_LIQUID_GLASS : BLUR_STYLE_IOS_FROSTED;
        int style = Settings.Secure.getInt(cr, KEY_SYSTEM_BLUR_STYLE, defaultStyle);
        if (lg) {
            return BLUR_STYLE_LIQUID_GLASS;
        }
        return Math.max(BLUR_STYLE_STANDARD, Math.min(BLUR_STYLE_LIQUID_GLASS, style));
    }

    private void writeSystemBlurStyle(int style) {
        int clamped = Math.max(BLUR_STYLE_STANDARD, Math.min(BLUR_STYLE_LIQUID_GLASS, style));
        ContentResolver cr = getContentResolver();
        Settings.Secure.putInt(cr, KEY_SYSTEM_BLUR_STYLE, clamped);
        Settings.Secure.putInt(cr, KEY_LIQUID_GLASS, clamped == BLUR_STYLE_LIQUID_GLASS ? 1 : 0);
    }

    private void updateSystemBlurStylePreference() {
        if (mSystemBlurStylePref == null) return;
        int style = readSystemBlurStyle();
        mSystemBlurStylePref.setValue(String.valueOf(style));
        updateSystemBlurStyleSummary(style);
    }

    private void updateSystemBlurStyleSummary(int style) {
        if (mSystemBlurStylePref == null) return;
        int index = mSystemBlurStylePref.findIndexOfValue(String.valueOf(style));
        CharSequence[] entries = mSystemBlurStylePref.getEntries();
        if (index >= 0 && entries != null && index < entries.length) {
            mSystemBlurStylePref.setSummary(entries[index]);
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_SYSTEM_CATEGORY;
    }
}
