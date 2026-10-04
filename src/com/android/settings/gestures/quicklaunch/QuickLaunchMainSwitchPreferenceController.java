/*
 * Copyright (C) 2026 Project ASCP
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

package com.android.settings.gestures.quicklaunch;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;

public class QuickLaunchMainSwitchPreferenceController extends TogglePreferenceController {

    private MainSwitchPreference mSwitchPreference;
    private QuickLaunchPreviewPreference mPreviewPreference;

    public QuickLaunchMainSwitchPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return QuickLaunchHelper.getInstance(mContext).isUdfpsSupported()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return QuickLaunchHelper.getInstance(mContext).isQuickLaunchEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        QuickLaunchHelper.getInstance(mContext).setQuickLaunchEnabled(isChecked);
        if (mPreviewPreference != null) {
            mPreviewPreference.setQuickLaunchEnabled(isChecked);
        }
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitchPreference = screen.findPreference(getPreferenceKey());
        mPreviewPreference = screen.findPreference("quick_launch_preview");
        if (mPreviewPreference != null) {
            mPreviewPreference.setQuickLaunchEnabled(isChecked());
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
