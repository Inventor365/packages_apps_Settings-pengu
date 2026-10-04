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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Process;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuickLaunchHelper {

    private static final String TAG = "QuickLaunchHelper";

    public static final String SETTING_QUICK_LAUNCH_ENABLED = "quick_launch_enabled";
    public static final String SETTING_QUICK_LAUNCH_ITEMS = "quick_launch_items";

        public static final int[] SLOT_CHECK_ORDER = new int[]{2, 3, 1, 4, 0};

    private static QuickLaunchHelper sInstance;

    private final Context mContext;
    private final LauncherApps mLauncherApps;
    private final PackageManager mPackageManager;
    private final int mDensityDpi;
    private Boolean mIsUdfpsSupported;

    private QuickLaunchHelper(Context context) {
        mContext = context.getApplicationContext();
        mLauncherApps = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        mPackageManager = mContext.getPackageManager();
        mDensityDpi = mContext.getResources().getDisplayMetrics().densityDpi;
    }

    public static synchronized QuickLaunchHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new QuickLaunchHelper(context);
        }
        return sInstance;
    }

    public boolean isUdfpsSupported() {
        if (mIsUdfpsSupported == null) {
            mIsUdfpsSupported = false;
            FingerprintManager fpm = Utils.getFingerprintManagerOrNull(mContext);
            if (fpm != null) {
                List<FingerprintSensorPropertiesInternal> props = fpm.getSensorPropertiesInternal();
                if (props != null) {
                    for (FingerprintSensorPropertiesInternal prop : props) {
                        if (prop.isAnyUdfpsType()) {
                            mIsUdfpsSupported = true;
                            break;
                        }
                    }
                }
            }
        }
        return mIsUdfpsSupported;
    }

    public boolean isQuickLaunchEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                SETTING_QUICK_LAUNCH_ENABLED, 1) == 1;
    }

    public void setQuickLaunchEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                SETTING_QUICK_LAUNCH_ENABLED, enabled ? 1 : 0);
    }

    public List<QuickLaunchItem> getQuickLaunchItems() {
        String json = Settings.Secure.getString(mContext.getContentResolver(),
                SETTING_QUICK_LAUNCH_ITEMS);
        List<QuickLaunchItem> items = QuickLaunchItem.parseJson(json);
                if (isAllEmpty(items)) {
            items = createInitialDefaultItems();
            saveQuickLaunchItems(items);
        }
        for (QuickLaunchItem item : items) {
            loadIconForItem(item);
        }
        return items;
    }

    public void saveQuickLaunchItems(List<QuickLaunchItem> items) {
        if (items == null) return;
        String json = QuickLaunchItem.toJsonString(items);
        Settings.Secure.putString(mContext.getContentResolver(),
                SETTING_QUICK_LAUNCH_ITEMS, json);
    }

    private boolean isAllEmpty(List<QuickLaunchItem> items) {
        if (items == null || items.isEmpty()) return true;
        for (QuickLaunchItem item : items) {
            if (!item.isEmpty()) return false;
        }
        return true;
    }

    
    private List<QuickLaunchItem> createInitialDefaultItems() {
        List<QuickLaunchItem> defaultSlots = QuickLaunchItem.createDefaultSlots();

                Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        ResolveInfo cameraResolve = mPackageManager.resolveActivity(cameraIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (cameraResolve != null && cameraResolve.activityInfo != null) {
            QuickLaunchItem cameraItem = new QuickLaunchItem(QuickLaunchItem.VIEW_TYPE_APP, 2);
            cameraItem.setPackageName(cameraResolve.activityInfo.packageName);
            cameraItem.setClassName(cameraResolve.activityInfo.name);
            cameraItem.setTitle(cameraResolve.loadLabel(mPackageManager).toString());
            defaultSlots.set(2, cameraItem);
        }

                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        ResolveInfo dialResolve = mPackageManager.resolveActivity(dialIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (dialResolve != null && dialResolve.activityInfo != null) {
            QuickLaunchItem dialItem = new QuickLaunchItem(QuickLaunchItem.VIEW_TYPE_APP, 1);
            dialItem.setPackageName(dialResolve.activityInfo.packageName);
            dialItem.setClassName(dialResolve.activityInfo.name);
            dialItem.setTitle(dialResolve.loadLabel(mPackageManager).toString());
            defaultSlots.set(1, dialItem);
        }

                Intent calcIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR);
        ResolveInfo calcResolve = mPackageManager.resolveActivity(calcIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (calcResolve != null && calcResolve.activityInfo != null) {
            QuickLaunchItem calcItem = new QuickLaunchItem(QuickLaunchItem.VIEW_TYPE_APP, 3);
            calcItem.setPackageName(calcResolve.activityInfo.packageName);
            calcItem.setClassName(calcResolve.activityInfo.name);
            calcItem.setTitle(calcResolve.loadLabel(mPackageManager).toString());
            defaultSlots.set(3, calcItem);
        }

        return defaultSlots;
    }

    public void loadIconForItem(QuickLaunchItem item) {
        if (item == null) return;
        if (item.isEmpty()) {
            item.setIcon(ContextCompat.getDrawable(mContext,
                    R.drawable.kgd_fp_quick_launch_empty_icon));
            return;
        }

        Drawable icon = null;
        if (item.getViewType() == QuickLaunchItem.VIEW_TYPE_SHORTCUT) {
                        if (mLauncherApps != null && !TextUtils.isEmpty(item.getShortcutId())) {
                try {
                    LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
                    query.setPackage(item.getPackageName());
                    query.setShortcutIds(Collections.singletonList(item.getShortcutId()));
                    query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                            | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                            | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
                    List<ShortcutInfo> shortcuts = mLauncherApps.getShortcuts(query,
                            Process.myUserHandle());
                    if (shortcuts != null && !shortcuts.isEmpty()) {
                        icon = mLauncherApps.getShortcutIconDrawable(shortcuts.get(0), mDensityDpi);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error loading shortcut icon for " + item.getShortcutId() + ": " + e);
                }
            }
                        try {
                item.setSubIcon(ensureCircleShape(mPackageManager.getApplicationIcon(item.getPackageName())));
            } catch (Exception ignored) {
            }
        }

        if (icon == null) {
                        if (mLauncherApps != null && !TextUtils.isEmpty(item.getPackageName())) {
                try {
                    List<LauncherActivityInfo> list = mLauncherApps.getActivityList(
                            item.getPackageName(), new UserHandle(item.getUserId()));
                    for (LauncherActivityInfo lai : list) {
                        if (lai.getComponentName().getClassName().equals(item.getClassName())) {
                            icon = lai.getIcon(mDensityDpi);
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error loading launcher activity icon: " + e);
                }
            }
            if (icon == null && !TextUtils.isEmpty(item.getPackageName())) {
                try {
                    icon = mPackageManager.getApplicationIcon(item.getPackageName());
                } catch (Exception ignored) {
                }
            }
        }

        if (icon == null) {
            icon = ContextCompat.getDrawable(mContext, R.drawable.kgd_ql_app_default_icon);
        }
        item.setIcon(ensureCircleShape(icon));

        if (TextUtils.isEmpty(item.getAppLabel()) && !TextUtils.isEmpty(item.getPackageName())) {
            try {
                item.setAppLabel(mPackageManager.getApplicationLabel(
                        mPackageManager.getApplicationInfo(item.getPackageName(), 0)).toString());
            } catch (Exception ignored) {
            }
        }
    }

    
    public List<QuickLaunchItem> queryAllApps(List<QuickLaunchItem> currentSlots) {
        List<QuickLaunchItem> appList = new ArrayList<>();
        if (mLauncherApps == null) return appList;

        UserHandle userHandle = Process.myUserHandle();
        List<LauncherActivityInfo> activityList = mLauncherApps.getActivityList(null, userHandle);
        if (activityList == null) return appList;

        Set<String> selectedKeys = getSelectedKeys(currentSlots);

        for (LauncherActivityInfo info : activityList) {
            String pkg = info.getApplicationInfo().packageName;
            String cls = info.getComponentName().getClassName();

                        if (mContext.getPackageName().equals(pkg)) continue;

            QuickLaunchItem item = new QuickLaunchItem(QuickLaunchItem.VIEW_TYPE_APP, 0);
            item.setPackageName(pkg);
            item.setClassName(cls);
            item.setTitle(info.getLabel().toString());
            item.setAppLabel(info.getLabel().toString());
            item.setUserId(userHandle.getIdentifier());
            item.setIcon(ensureCircleShape(info.getIcon(mDensityDpi)));

            if (selectedKeys.contains(getItemKey(item))) {
                item.setViewAction(QuickLaunchItem.VIEW_ACTION_SELECTED);
            } else {
                item.setViewAction(QuickLaunchItem.VIEW_ACTION_NORMAL);
            }
            appList.add(item);
        }

                Collections.sort(appList, new Comparator<QuickLaunchItem>() {
            @Override
            public int compare(QuickLaunchItem o1, QuickLaunchItem o2) {
                return o1.getTitle().compareToIgnoreCase(o2.getTitle());
            }
        });

        return appList;
    }

    
    public List<QuickLaunchItem> queryAllShortcuts(List<QuickLaunchItem> currentSlots) {
        List<QuickLaunchItem> shortcutList = new ArrayList<>();
        if (mLauncherApps == null) return shortcutList;

        UserHandle userHandle = Process.myUserHandle();
        Set<String> selectedKeys = getSelectedKeys(currentSlots);

        try {
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                    | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);

            List<ShortcutInfo> shortcuts = mLauncherApps.getShortcuts(query, userHandle);
            if (shortcuts != null) {
                for (ShortcutInfo shortcut : shortcuts) {
                    if (shortcut.getPackage() == null || shortcut.getId() == null) continue;

                    QuickLaunchItem item = new QuickLaunchItem(
                            QuickLaunchItem.VIEW_TYPE_SHORTCUT, 0);
                    item.setPackageName(shortcut.getPackage());
                    if (shortcut.getActivity() != null) {
                        item.setClassName(shortcut.getActivity().getClassName());
                    }
                    item.setShortcutId(shortcut.getId());

                    CharSequence label = shortcut.getShortLabel();
                    if (TextUtils.isEmpty(label)) {
                        label = shortcut.getLongLabel();
                    }
                    if (TextUtils.isEmpty(label)) {
                        label = shortcut.getId();
                    }
                    item.setTitle(label.toString());
                    item.setUserId(shortcut.getUserId());

                    String appLabel = "";
                    try {
                        appLabel = mPackageManager.getApplicationLabel(
                                mPackageManager.getApplicationInfo(shortcut.getPackage(), 0)).toString();
                    } catch (Exception ignored) {
                    }
                    if (TextUtils.isEmpty(appLabel)) {
                        appLabel = shortcut.getPackage();
                    }
                    item.setAppLabel(appLabel);

                    try {
                        item.setIcon(ensureCircleShape(mLauncherApps.getShortcutIconDrawable(shortcut, mDensityDpi)));
                        item.setSubIcon(ensureCircleShape(mPackageManager.getApplicationIcon(shortcut.getPackage())));
                    } catch (Exception ignored) {
                    }

                    if (selectedKeys.contains(getItemKey(item))) {
                        item.setViewAction(QuickLaunchItem.VIEW_ACTION_SELECTED);
                    } else {
                        item.setViewAction(QuickLaunchItem.VIEW_ACTION_NORMAL);
                    }
                    shortcutList.add(item);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error querying shortcuts: " + e.getMessage());
        }

                Collections.sort(shortcutList, new Comparator<QuickLaunchItem>() {
            @Override
            public int compare(QuickLaunchItem o1, QuickLaunchItem o2) {
                return o1.getTitle().compareToIgnoreCase(o2.getTitle());
            }
        });

        return shortcutList;
    }

    public static String getItemKey(QuickLaunchItem item) {
        if (item == null) return "";
        return item.getPackageName() + "/" + item.getClassName() + "#" + item.getShortcutId();
    }

    public static Set<String> getSelectedKeys(List<QuickLaunchItem> slots) {
        Set<String> set = new HashSet<>();
        if (slots == null) return set;
        for (QuickLaunchItem item : slots) {
            if (!item.isEmpty()) {
                set.add(getItemKey(item));
            }
        }
        return set;
    }

    private Drawable ensureCircleShape(Drawable icon) {
        if (icon == null) {
            return null;
        }
        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable aid = (AdaptiveIconDrawable) icon;
            if (aid.getBackground() != null) {
                return icon;
            }
            Drawable fg = aid.getForeground();
            if (fg != null) {
                return new AdaptiveIconDrawable(new ColorDrawable(Color.WHITE), fg);
            }
        }
        float inset = AdaptiveIconDrawable.getExtraInsetFraction();
        return new AdaptiveIconDrawable(new ColorDrawable(Color.WHITE), new InsetDrawable(icon, inset));
    }
}
