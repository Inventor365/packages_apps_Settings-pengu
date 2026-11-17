/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.custom.lab;

import android.content.Intent;
import android.os.Bundle;

import com.android.settings.SubSettings;

import java.util.Set;

/**
 * Opens one of Settings' PenguinOS pages for Penguin Laboratory, which links to them from its
 * tabs. Only callers holding WRITE_SECURE_SETTINGS can start it, and only the pages listed here
 * can be opened.
 */
public class PenguinLabLinkActivity extends SubSettings {

    private static final String EXTRA_FRAGMENT = "fragment";
    private static final String EXTRA_TITLE = "title";

    private static final Set<String> PAGES = Set.of(
            "com.android.settings.custom.obscura.ObscuraAppSettings",
            "com.android.settings.custom.networktraffic.NetworkTrafficSettings",
            "com.android.settings.display.StatusbarPaddingSettings",
            "com.android.settings.display.DynamicIslandSettings",
            "com.android.settings.network.QsDataUsageSettings",
            "com.android.settings.notification.HeadsUpSettings",
            "com.android.settings.lockscreen.LockScreenWeather",
            "com.android.settings.display.MediaArtSettings",
            "com.android.settings.display.PulseSettings",
            "com.android.settings.lockscreen.WallpaperDepth",
            "com.android.settings.themes.LockGlympsSettings",
            "com.android.settings.gestures.OffscreenGestureSettings",
            "com.android.settings.gestures.quicklaunch.QuickLaunchSettings",
            "com.android.settings.gestures.PowerMenuSettings",
            "com.android.settings.gestures.GestureTweaksSettings",
            "com.android.settings.custom.taskbar.TaskbarSettings",
            "com.android.settings.sound.CustomVibrationPreferenceFragment",
            "com.android.settings.custom.smartpixel.SmartPixelsSettings",
            "com.android.settings.display.ColorModePreferenceFragment",
            "com.android.settings.system.spoofing.UserSelectedAppSpoofSettings",
            "com.android.settings.system.spoofing.AdvancedAppSpoofSettings",
            "com.android.settings.custom.spoofing.PlayIntegrityFix",
            "com.android.settings.custom.spoofing.TrickyStore",
            "com.android.settings.custom.spoofing.GameSpoofing",
            "com.android.settings.system.ThemeSettingsFragment",
            "com.android.settings.custom.font.FontSettings",
            "com.android.settings.custom.monet.MonetSettings",
            "com.android.settings.fuelgauge.IdleManagerSettings",
            "com.android.settings.fuelgauge.sleepmode.SleepMode",
            "com.android.settings.fuelgauge.powerinsight.PowerInsightSettings");

    @Override
    public Intent getIntent() {
        final Intent intent = new Intent(super.getIntent());
        intent.removeExtra(EXTRA_SHOW_FRAGMENT);
        final String page = intent.getStringExtra(EXTRA_FRAGMENT);
        if (page != null && PAGES.contains(page)) {
            intent.putExtra(EXTRA_SHOW_FRAGMENT, page);
            final String title = intent.getStringExtra(EXTRA_TITLE);
            if (title != null) {
                intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, title);
            }
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT) == null) {
            finish();
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return PAGES.contains(fragmentName);
    }
}
