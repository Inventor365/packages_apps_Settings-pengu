/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.Log;
import android.provider.SearchIndexableResource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.applications.appcompat.UserAspectRatioAppsPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import static android.os.UserHandle.USER_SYSTEM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Settings page for apps. */
// LINT.IfChange
@SearchIndexable
public class AppDashboardFragment extends DashboardFragment {

    private static final String TAG = "AppDashboardFragment";
    private static final String ADVANCED_CATEGORY_KEY = "advanced_category";
    private static final String ASPECT_RATIO_PREF_KEY = "aspect_ratio_apps";
    private static final String REVAN_PREF_KEY = "persist_revan_mod";
    private static final String REVAN_PROP = "persist.sys.revan.mod";
    private static final String[] REVAN_PACKAGES = {
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music"
    };
    /** Reboot even if a PackageManager callback never arrives. */
    private static final long REVAN_ENABLE_TIMEOUT_MS = 30_000L;
    private AppsPreferenceController mAppsPreferenceController;

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new AppsPreferenceController(context));

        final UserAspectRatioAppsPreferenceController aspectRatioAppsPreferenceController =
                new UserAspectRatioAppsPreferenceController(context, ASPECT_RATIO_PREF_KEY);
        final AdvancedAppsPreferenceCategoryController advancedCategoryController =
                new AdvancedAppsPreferenceCategoryController(context, ADVANCED_CATEGORY_KEY);
        advancedCategoryController.setChildren(List.of(aspectRatioAppsPreferenceController));
        controllers.add(advancedCategoryController);
        return controllers;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MANAGE_APPLICATIONS;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return AppDashboardScreen.KEY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_apps_and_notifications;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.apps;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAppsPreferenceController = use(AppsPreferenceController.class);
        mAppsPreferenceController.setFragment(this /* fragment */);
        getSettingsLifecycle().addObserver(mAppsPreferenceController);

        final HibernatedAppsPreferenceController hibernatedAppsPreferenceController =
                use(HibernatedAppsPreferenceController.class);
        getSettingsLifecycle().addObserver(hibernatedAppsPreferenceController);
    }

    @Override
    public void onStart() {
        super.onStart();

        final Preference pref = findPreference(REVAN_PREF_KEY);
        if (pref instanceof SwitchPreferenceCompat) {
            final SwitchPreferenceCompat toggle = (SwitchPreferenceCompat) pref;
            toggle.setChecked(SystemProperties.getBoolean(REVAN_PROP, true));
            toggle.setOnPreferenceChangeListener((p, newVal) -> {
                showRevanRestartDialog(toggle, (Boolean) newVal);
                return false;
            });
        }
    }

    private void showRevanRestartDialog(SwitchPreferenceCompat toggle, boolean enabled) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.revan_restart_dialog_title)
                .setMessage(R.string.revan_restart_dialog_message)
                .setPositiveButton(R.string.revan_restart_now, (dialog, which) -> {
                    final Context context = requireContext().getApplicationContext();
                    toggle.setChecked(enabled);
                    if (enabled) {
                        uninstallUpdatesThenEnable(context);
                    } else {
                        // init.rv.rc drops the patched APK mounts as soon as the
                        // property is false, and again on every boot it stays false.
                        SystemProperties.set(REVAN_PROP, "false");
                        Log.i(TAG, "ReVanced off: " + REVAN_PROP + "="
                                + SystemProperties.get(REVAN_PROP) + ", rebooting");
                        reboot(context);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * The patched APKs are mounted over the /product copies, so a Play Store
     * update in /data would run instead of them: remove the updates first, with
     * the property still false so PackageManager restores the stock system
     * package, then enable and reboot. Only updated packages are deleted (on a
     * system package without an update the call only fails), and the reboot
     * happens even if a callback never arrives.
     */
    private void uninstallUpdatesThenEnable(Context context) {
        final PackageManager pm = context.getPackageManager();
        final List<String> updated = new ArrayList<>();
        for (String pkg : REVAN_PACKAGES) {
            try {
                final ApplicationInfo info = pm.getApplicationInfo(pkg,
                        PackageManager.MATCH_UNINSTALLED_PACKAGES);
                if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    updated.add(pkg);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "ReVanced on: " + pkg + " is not installed");
            }
        }

        final AtomicBoolean enabled = new AtomicBoolean();
        final Runnable enableAndReboot = () -> {
            if (!enabled.compareAndSet(false, true)) return;
            SystemProperties.set(REVAN_PROP, "true");
            Log.i(TAG, "ReVanced on: " + REVAN_PROP + "="
                    + SystemProperties.get(REVAN_PROP) + ", rebooting");
            reboot(context);
        };
        if (updated.isEmpty()) {
            enableAndReboot.run();
            return;
        }

        final AtomicInteger pendingDeletes = new AtomicInteger(updated.size());
        final IPackageDeleteObserver observer = new IPackageDeleteObserver.Stub() {
            @Override
            public void packageDeleted(String packageName, int returnCode) {
                Log.i(TAG, "ReVanced on: removed updates of " + packageName
                        + " (result " + returnCode + ")");
                if (pendingDeletes.decrementAndGet() == 0) {
                    enableAndReboot.run();
                }
            }
        };
        new Handler(Looper.getMainLooper()).postDelayed(enableAndReboot, REVAN_ENABLE_TIMEOUT_MS);
        for (String pkg : updated) {
            pm.deletePackageAsUser(pkg, observer, 0, USER_SYSTEM);
        }
    }

    private void reboot(Context context) {
        context.getSystemService(PowerManager.class).reboot(null);
    }

    @VisibleForTesting
    PreferenceCategoryController getAdvancedAppsPreferenceCategoryController() {
        return use(AdvancedAppsPreferenceCategoryController.class);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.apps;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context);
                }
            };
}
// LINT.ThenChange(AppDashboardScreen.kt)
