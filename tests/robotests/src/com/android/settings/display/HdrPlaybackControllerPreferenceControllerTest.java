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
import static android.view.Display.HdrCapabilities.HDR_TYPE_HDR10;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.testing.TestableContext;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.DisplayInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class HdrPlaybackControllerPreferenceControllerTest {

    private HdrPlaybackControllerPreferenceController mController;

    @Mock
    private DisplayManager mDisplayManager;

    @Mock
    private DisplayManagerGlobal mDisplayManagerGlobal;

    @Rule
    public final TestableContext mContext = new TestableContext(
            InstrumentationRegistry.getInstrumentation().getContext());

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext.addMockSystemService(DisplayManager.class, mDisplayManager);
        mController = new HdrPlaybackControllerPreferenceController(
                mContext, "hdr_playback_controller");
    }

    @Test
    public void getAvailabilityStatus_hdrSupported_returnsAvailable() {
        DisplayAdjustments daj = null;
        DisplayInfo info = new DisplayInfo();
        info.hdrCapabilities = new Display.HdrCapabilities(
                new int[]{HDR_TYPE_HDR10}, 1000f, 1000f, 0f);
        info.hdrSdrRatio = 4.0f;
        Display.Mode mode = new Display.Mode(
                0, 1080, 2400, 120f, new float[0], new int[]{HDR_TYPE_HDR10});
        info.supportedModes = new Display.Mode[]{mode};
        Display display = new Display(mDisplayManagerGlobal, 0, info, daj);

        when(mDisplayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED))
                .thenReturn(new Display[]{display});

        assertEquals(AVAILABLE, mController.getAvailabilityStatus());
    }

    @Test
    public void getAvailabilityStatus_noHdrDisplays_returnsUnsupported() {
        DisplayAdjustments daj = null;
        Display display = new Display(mDisplayManagerGlobal, 0, new DisplayInfo(), daj);
        when(mDisplayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED))
                .thenReturn(new Display[]{display});

        assertEquals(UNSUPPORTED_ON_DEVICE, mController.getAvailabilityStatus());
    }

    @Test
    public void getSummary_noOverrides_returnsDefaultSummary() {
        when(mDisplayManager.getAppHdrPlaybackPackages()).thenReturn(new String[0]);

        assertEquals(
                mContext.getString(R.string.hdr_playback_controller_summary_default),
                mController.getSummary().toString());
    }

    @Test
    public void getSummary_withOverrides_returnsCustomCounts() {
        when(mDisplayManager.getAppHdrPlaybackPackages())
                .thenReturn(new String[]{"com.app.one", "com.app.two", "com.app.three"});
        when(mDisplayManager.getAppHdrPlaybackMode("com.app.one"))
                .thenReturn(APP_HDR_PLAYBACK_MODE_SDR_ONLY);
        when(mDisplayManager.getAppHdrPlaybackMode("com.app.two"))
                .thenReturn(APP_HDR_PLAYBACK_MODE_SDR_ONLY);
        when(mDisplayManager.getAppHdrPlaybackMode("com.app.three"))
                .thenReturn(APP_HDR_PLAYBACK_MODE_HDR_ALLOWED);

        assertEquals(
                mContext.getString(R.string.hdr_playback_controller_summary_custom, 3, 2, 1),
                mController.getSummary().toString());
    }
}
