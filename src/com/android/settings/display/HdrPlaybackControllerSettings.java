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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.AppHdrPlaybackMode;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Native Settings fragment for configuring per-application HDR playback policies.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class HdrPlaybackControllerSettings extends SettingsPreferenceFragment {

    public static final int FILTER_ALL = 0;
    public static final int FILTER_CUSTOM = 1;
    public static final int FILTER_SDR_ONLY = 2;
    public static final int FILTER_HDR_ALLOWED = 3;

    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_ACTIVE_FILTER = "active_filter";

    /**
     * Value object representing an application entry and its current HDR playback policy.
     */
    public static final class AppHdrEntry {
        public final String packageName;
        public final String label;
        @Nullable public final Drawable icon;
        @AppHdrPlaybackMode public int mode;

        public AppHdrEntry(
                @NonNull String packageName,
                @NonNull String label,
                @Nullable Drawable icon,
                @AppHdrPlaybackMode int mode) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.mode = mode;
        }
    }

    private DisplayManager mDisplayManager;
    private PackageManager mPackageManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService mExecutor;

    private TextView mHeroStatusChip;
    private EditText mSearchInput;
    private View mSearchClearButton;
    private TextView mChipAll;
    private TextView mChipCustom;
    private TextView mChipSdrOnly;
    private TextView mChipHdrAllowed;
    private View mResetAllChip;
    private TextView mEmptyView;
    private RecyclerView mRecyclerView;

    private AppHdrAdapter mAdapter;
    private final List<AppHdrEntry> mAllEntries = new ArrayList<>();
    private final List<AppHdrEntry> mFilteredEntries = new ArrayList<>();
    private String mSearchQuery = "";
    private int mActiveFilter = FILTER_ALL;
    private boolean mIsLoading = true;

    private final ContentObserver mPolicyObserver = new ContentObserver(mMainHandler) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            refreshModesFromDisplayManager();
        }
    };

    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApplicationsAsync();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = requireContext();
        mDisplayManager = context.getSystemService(DisplayManager.class);
        mPackageManager = context.getPackageManager();
        mExecutor = Executors.newSingleThreadExecutor();

        if (savedInstanceState != null) {
            mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            mActiveFilter = savedInstanceState.getInt(KEY_ACTIVE_FILTER, FILTER_ALL);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.hdr_playback_controller_fragment, container, false);
        bindViews(root);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.APP_HDR_PLAYBACK_POLICIES),
                    false,
                    mPolicyObserver,
                    UserHandle.USER_CURRENT);
            IntentFilter packageFilter = new IntentFilter();
            packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            packageFilter.addDataScheme("package");
            context.registerReceiver(mPackageReceiver, packageFilter);
        }
        loadApplicationsAsync();
    }

    @Override
    public void onStop() {
        super.onStop();
        Context context = getContext();
        if (context != null) {
            try {
                context.getContentResolver().unregisterContentObserver(mPolicyObserver);
            } catch (Exception ignored) {
            }
            try {
                context.unregisterReceiver(mPackageReceiver);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mExecutor != null) {
            mExecutor.shutdownNow();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEARCH_QUERY, mSearchQuery);
        outState.putInt(KEY_ACTIVE_FILTER, mActiveFilter);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.HDR_BRIGHTNESS_SETTINGS;
    }

    private void bindViews(View root) {
        mHeroStatusChip = root.findViewById(R.id.hdr_hero_status_chip);
        mSearchInput = root.findViewById(R.id.hdr_search_input);
        mSearchClearButton = root.findViewById(R.id.hdr_search_clear);
        mChipAll = root.findViewById(R.id.filter_chip_all);
        mChipCustom = root.findViewById(R.id.filter_chip_custom);
        mChipSdrOnly = root.findViewById(R.id.filter_chip_sdr_only);
        mChipHdrAllowed = root.findViewById(R.id.filter_chip_hdr_allowed);
        mResetAllChip = root.findViewById(R.id.hdr_reset_all_chip);
        mEmptyView = root.findViewById(R.id.hdr_empty_view);
        mRecyclerView = root.findViewById(R.id.hdr_apps_recycler_view);

        mAdapter = new AppHdrAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);

        if (!TextUtils.isEmpty(mSearchQuery)) {
            mSearchInput.setText(mSearchQuery);
            mSearchClearButton.setVisibility(View.VISIBLE);
        }

        mSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearchQuery = s != null ? s.toString().trim() : "";
                if (mSearchClearButton != null) {
                    mSearchClearButton.setVisibility(
                            TextUtils.isEmpty(mSearchQuery) ? View.GONE : View.VISIBLE);
                }
                applyFiltersAndNotify();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mSearchClearButton.setOnClickListener(v -> {
            mSearchInput.setText("");
            mSearchQuery = "";
            mSearchClearButton.setVisibility(View.GONE);
            applyFiltersAndNotify();
        });

        mChipAll.setOnClickListener(v -> setActiveFilter(FILTER_ALL));
        mChipCustom.setOnClickListener(v -> setActiveFilter(FILTER_CUSTOM));
        mChipSdrOnly.setOnClickListener(v -> setActiveFilter(FILTER_SDR_ONLY));
        mChipHdrAllowed.setOnClickListener(v -> setActiveFilter(FILTER_HDR_ALLOWED));

        mResetAllChip.setOnClickListener(v -> showResetAllConfirmationDialog());
        updateFilterChipsSelection();
    }

    private void setActiveFilter(int filter) {
        if (mActiveFilter == filter) {
            return;
        }
        mActiveFilter = filter;
        updateFilterChipsSelection();
        applyFiltersAndNotify();
    }

    private void updateFilterChipsSelection() {
        if (mChipAll != null) {
            mChipAll.setSelected(mActiveFilter == FILTER_ALL);
        }
        if (mChipCustom != null) {
            mChipCustom.setSelected(mActiveFilter == FILTER_CUSTOM);
        }
        if (mChipSdrOnly != null) {
            mChipSdrOnly.setSelected(mActiveFilter == FILTER_SDR_ONLY);
        }
        if (mChipHdrAllowed != null) {
            mChipHdrAllowed.setSelected(mActiveFilter == FILTER_HDR_ALLOWED);
        }
    }

    private void showResetAllConfirmationDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle(R.string.hdr_playback_reset_all_title)
                .setMessage(R.string.hdr_playback_reset_all_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.hdr_playback_reset_all, (dialog, which) -> {
                    if (mDisplayManager != null) {
                        mDisplayManager.resetAllAppHdrPlaybackModes();
                    }
                    refreshModesFromDisplayManager();
                })
                .show();
    }

    private void loadApplicationsAsync() {
        if (mExecutor == null || mExecutor.isShutdown() || mPackageManager == null) {
            return;
        }
        mIsLoading = true;
        mExecutor.execute(() -> {
            List<AppHdrEntry> loaded = loadCategorizedApplications();
            mMainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                mIsLoading = false;
                mAllEntries.clear();
                mAllEntries.addAll(loaded);
                updateHeroAndResetState();
                applyFiltersAndNotify();
            });
        });
    }

    private List<AppHdrEntry> loadCategorizedApplications() {
        Set<String> targetPackages = new ArraySet<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = mPackageManager.queryIntentActivities(mainIntent, 0);
        if (resolved != null) {
            for (ResolveInfo info : resolved) {
                if (info != null && info.activityInfo != null
                        && !TextUtils.isEmpty(info.activityInfo.packageName)) {
                    targetPackages.add(info.activityInfo.packageName);
                }
            }
        }

        if (mDisplayManager != null) {
            String[] overridden = mDisplayManager.getAppHdrPlaybackPackages();
            if (overridden != null) {
                Collections.addAll(targetPackages, overridden);
            }
        }

        List<AppHdrEntry> entries = new ArrayList<>(targetPackages.size());
        for (String packageName : targetPackages) {
            try {
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, 0);
                if (!appInfo.enabled) {
                    continue;
                }
                CharSequence rawLabel = mPackageManager.getApplicationLabel(appInfo);
                String label = rawLabel != null ? rawLabel.toString() : packageName;
                Drawable icon = mPackageManager.getApplicationIcon(appInfo);
                int mode = mDisplayManager != null
                        ? mDisplayManager.getAppHdrPlaybackMode(packageName)
                        : APP_HDR_PLAYBACK_MODE_DEFAULT;
                entries.add(new AppHdrEntry(packageName, label, icon, mode));
            } catch (PackageManager.NameNotFoundException ignored) {
                // Uninstalled packages remain persisted in AppHdrPolicyController,
                // but are omitted from the installed applications list until reinstalled.
            }
        }

        final Collator collator = Collator.getInstance();
        Collections.sort(entries, (a, b) -> {
            int cmp = collator.compare(a.label, b.label);
            return cmp != 0 ? cmp : a.packageName.compareTo(b.packageName);
        });
        return entries;
    }

    private void refreshModesFromDisplayManager() {
        if (mDisplayManager == null) {
            return;
        }
        for (int i = 0; i < mAllEntries.size(); i++) {
            AppHdrEntry entry = mAllEntries.get(i);
            entry.mode = mDisplayManager.getAppHdrPlaybackMode(entry.packageName);
        }
        updateHeroAndResetState();
        applyFiltersAndNotify();
    }

    private void updateHeroAndResetState() {
        Context context = getContext();
        if (context == null || mHeroStatusChip == null) {
            return;
        }
        int sdrOnlyCount = 0;
        int hdrAllowedCount = 0;
        if (mDisplayManager != null) {
            String[] packages = mDisplayManager.getAppHdrPlaybackPackages();
            if (packages != null) {
                for (int i = 0; i < packages.length; i++) {
                    int mode = mDisplayManager.getAppHdrPlaybackMode(packages[i]);
                    if (mode == APP_HDR_PLAYBACK_MODE_SDR_ONLY) {
                        sdrOnlyCount++;
                    } else if (mode == APP_HDR_PLAYBACK_MODE_HDR_ALLOWED) {
                        hdrAllowedCount++;
                    }
                }
            }
        }
        int totalCustom = sdrOnlyCount + hdrAllowedCount;
        if (totalCustom == 0) {
            mHeroStatusChip.setText(R.string.hdr_playback_status_all_default);
            if (mResetAllChip != null) {
                mResetAllChip.setVisibility(View.GONE);
            }
        } else {
            mHeroStatusChip.setText(context.getString(
                    R.string.hdr_playback_status_custom,
                    totalCustom, sdrOnlyCount, hdrAllowedCount));
            if (mResetAllChip != null) {
                mResetAllChip.setVisibility(View.VISIBLE);
            }
        }
    }

    private void applyFiltersAndNotify() {
        filterEntries(mAllEntries, mFilteredEntries, mSearchQuery, mActiveFilter);
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        if (mEmptyView != null && mRecyclerView != null) {
            if (mIsLoading) {
                mEmptyView.setText(R.string.hdr_playback_loading_apps);
                mEmptyView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else if (mFilteredEntries.isEmpty()) {
                mEmptyView.setText(R.string.hdr_playback_no_apps_found);
                mEmptyView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Filters application entries by search query (label or package name) and active mode filter.
     */
    @VisibleForTesting
    public static void filterEntries(
            @NonNull List<AppHdrEntry> source,
            @NonNull List<AppHdrEntry> destination,
            @Nullable String query,
            int filterMode) {
        destination.clear();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean hasQuery = !normalizedQuery.isEmpty();

        for (int i = 0; i < source.size(); i++) {
            AppHdrEntry entry = source.get(i);
            if (filterMode == FILTER_CUSTOM && entry.mode == APP_HDR_PLAYBACK_MODE_DEFAULT) {
                continue;
            }
            if (filterMode == FILTER_SDR_ONLY && entry.mode != APP_HDR_PLAYBACK_MODE_SDR_ONLY) {
                continue;
            }
            if (filterMode == FILTER_HDR_ALLOWED
                    && entry.mode != APP_HDR_PLAYBACK_MODE_HDR_ALLOWED) {
                continue;
            }
            if (hasQuery) {
                boolean matchesLabel = entry.label.toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery);
                boolean matchesPackage = entry.packageName.toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery);
                if (!matchesLabel && !matchesPackage) {
                    continue;
                }
            }
            destination.add(entry);
        }
    }

    private void setAppMode(@NonNull AppHdrEntry entry, @AppHdrPlaybackMode int newMode) {
        entry.mode = newMode;
        if (mDisplayManager != null) {
            mDisplayManager.setAppHdrPlaybackMode(entry.packageName, newMode);
        }
        updateHeroAndResetState();
        applyFiltersAndNotify();
    }

    private void showModeDialog(@NonNull AppHdrEntry entry) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        HdrPlaybackModeDialog dialog = new HdrPlaybackModeDialog(
                context,
                entry.label,
                entry.packageName,
                entry.icon,
                entry.mode,
                selectedMode -> setAppMode(entry, selectedMode));
        dialog.show();
    }

    private final class AppHdrAdapter extends RecyclerView.Adapter<AppHdrViewHolder> {

        @NonNull
        @Override
        public AppHdrViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.hdr_playback_app_item, parent, false);
            return new AppHdrViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AppHdrViewHolder holder, int position) {
            final AppHdrEntry entry = mFilteredEntries.get(position);
            Context context = holder.itemView.getContext();

            holder.iconView.setImageDrawable(entry.icon);
            holder.titleView.setText(entry.label);
            holder.packageView.setText(entry.packageName);

            int activeOnContainerColor = HdrPlaybackModeDialog.resolveColorByName(
                    context, "materialColorOnPrimaryContainer",
                    android.R.attr.textColorPrimaryInverse, 0xFFFFFFFF);
            int primaryTextColor = HdrPlaybackModeDialog.resolveColor(
                    context, android.R.attr.textColorPrimary, 0xFFFFFFFF);
            int secondaryTextColor = HdrPlaybackModeDialog.resolveColor(
                    context, android.R.attr.textColorSecondary, 0xAAFFFFFF);
            int accentColor = HdrPlaybackModeDialog.resolveColor(
                    context, android.R.attr.colorAccent, 0xFF386540);

            boolean isCustom = (entry.mode != APP_HDR_PLAYBACK_MODE_DEFAULT);
            if (entry.mode == APP_HDR_PLAYBACK_MODE_SDR_ONLY) {
                holder.modeChip.setText(R.string.hdr_playback_chip_sdr_only);
            } else if (entry.mode == APP_HDR_PLAYBACK_MODE_HDR_ALLOWED) {
                holder.modeChip.setText(R.string.hdr_playback_chip_allowed);
            } else {
                holder.modeChip.setText(R.string.hdr_playback_chip_default);
            }

            if (isCustom) {
                holder.rootView.setBackgroundResource(R.drawable.hdr_controller_item_bg_active);
                holder.titleView.setTextColor(activeOnContainerColor);
                holder.packageView.setTextColor(activeOnContainerColor);
                holder.resetButton.setVisibility(View.VISIBLE);
                holder.resetButton.setImageTintList(
                        ColorStateList.valueOf(activeOnContainerColor));
            } else {
                holder.rootView.setBackgroundResource(R.drawable.hdr_controller_item_bg_inactive);
                holder.titleView.setTextColor(primaryTextColor);
                holder.packageView.setTextColor(secondaryTextColor);
                holder.resetButton.setVisibility(View.GONE);
                holder.resetButton.setImageTintList(ColorStateList.valueOf(accentColor));
            }

            holder.rootView.setOnClickListener(v -> showModeDialog(entry));
            holder.resetButton.setOnClickListener(
                    v -> setAppMode(entry, APP_HDR_PLAYBACK_MODE_DEFAULT));
        }

        @Override
        public int getItemCount() {
            return mFilteredEntries.size();
        }
    }

    private static final class AppHdrViewHolder extends RecyclerView.ViewHolder {
        final View rootView;
        final ImageView iconView;
        final TextView titleView;
        final TextView packageView;
        final TextView modeChip;
        final ImageView resetButton;

        AppHdrViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView.findViewById(R.id.hdr_app_item_root);
            iconView = itemView.findViewById(R.id.app_icon);
            titleView = itemView.findViewById(R.id.app_title);
            packageView = itemView.findViewById(R.id.app_package);
            modeChip = itemView.findViewById(R.id.app_mode_chip);
            resetButton = itemView.findViewById(R.id.app_reset_button);
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.hdr_playback_controller_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return HdrPlaybackControllerPreferenceController
                            .getDisplayHdrAvailabilityStatus(context) == AVAILABLE;
                }
            };
}
