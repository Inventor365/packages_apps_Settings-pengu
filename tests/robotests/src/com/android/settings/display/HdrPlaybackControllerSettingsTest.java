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

import static android.hardware.display.DisplayManager.APP_HDR_PLAYBACK_MODE_DEFAULT;
import static android.hardware.display.DisplayManager.APP_HDR_PLAYBACK_MODE_HDR_ALLOWED;
import static android.hardware.display.DisplayManager.APP_HDR_PLAYBACK_MODE_SDR_ONLY;

import static com.google.common.truth.Truth.assertThat;

import android.provider.SearchIndexableResource;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.display.HdrPlaybackControllerSettings.AppHdrEntry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class HdrPlaybackControllerSettingsTest {

    @Test
    public void testIndexing_containsResource() {
        List<SearchIndexableResource> resources =
                HdrPlaybackControllerSettings.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(
                                ApplicationProvider.getApplicationContext(), true /* enabled */);

        List<Integer> indexedXml = new ArrayList<>();
        for (SearchIndexableResource resource : resources) {
            indexedXml.add(resource.xmlResId);
        }

        assertThat(indexedXml).contains(R.xml.hdr_playback_controller_settings);
    }

    @Test
    public void testFilterEntries_byModeAndQuery() {
        AppHdrEntry youtube = new AppHdrEntry(
                "com.google.android.youtube", "YouTube", null, APP_HDR_PLAYBACK_MODE_SDR_ONLY);
        AppHdrEntry netflix = new AppHdrEntry(
                "com.netflix.mediaclient", "Netflix", null, APP_HDR_PLAYBACK_MODE_HDR_ALLOWED);
        AppHdrEntry gallery = new AppHdrEntry(
                "com.android.gallery3d", "Gallery", null, APP_HDR_PLAYBACK_MODE_DEFAULT);
        List<AppHdrEntry> all = Arrays.asList(youtube, netflix, gallery);
        List<AppHdrEntry> filtered = new ArrayList<>();

        // Filter ALL without query
        HdrPlaybackControllerSettings.filterEntries(
                all, filtered, "", HdrPlaybackControllerSettings.FILTER_ALL);
        assertThat(filtered).containsExactly(youtube, netflix, gallery).inOrder();

        // Filter CUSTOM
        HdrPlaybackControllerSettings.filterEntries(
                all, filtered, "", HdrPlaybackControllerSettings.FILTER_CUSTOM);
        assertThat(filtered).containsExactly(youtube, netflix).inOrder();

        // Filter SDR_ONLY
        HdrPlaybackControllerSettings.filterEntries(
                all, filtered, "", HdrPlaybackControllerSettings.FILTER_SDR_ONLY);
        assertThat(filtered).containsExactly(youtube);

        // Filter HDR_ALLOWED
        HdrPlaybackControllerSettings.filterEntries(
                all, filtered, "", HdrPlaybackControllerSettings.FILTER_HDR_ALLOWED);
        assertThat(filtered).containsExactly(netflix);

        // Search by label
        HdrPlaybackControllerSettings.filterEntries(
                all, filtered, "netf", HdrPlaybackControllerSettings.FILTER_ALL);
        assertThat(filtered).containsExactly(netflix);

        // Search by package name
        HdrPlaybackControllerSettings.filterEntries(
                all, filtered, "gallery3d", HdrPlaybackControllerSettings.FILTER_ALL);
        assertThat(filtered).containsExactly(gallery);
    }
}
