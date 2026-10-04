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

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class QuickLaunchPreferenceController extends BasePreferenceController {

    public QuickLaunchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return QuickLaunchHelper.getInstance(mContext).isUdfpsSupported()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        boolean enabled = QuickLaunchHelper.getInstance(mContext).isQuickLaunchEnabled();
        return mContext.getText(enabled ? R.string.gesture_setting_on : R.string.gesture_setting_off);
    }
}
