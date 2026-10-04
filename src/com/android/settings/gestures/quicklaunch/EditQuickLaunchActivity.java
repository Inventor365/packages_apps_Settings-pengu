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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.android.settings.R;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditQuickLaunchActivity extends FragmentActivity {

    private static final String TAG = "EditQuickLaunchActivity";

    private QuickLaunchHelper mHelper;
    private QuickLaunchArcAdapter mArcAdapter;
    private RecyclerView mArcRecyclerView;
    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;
    private Vibrator mVibrator;

    private QuickLaunchCategoryListAdapter mFunctionsAdapter;
    private QuickLaunchCategoryListAdapter mAppsAdapter;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        int themeId = SettingsThemeHelper.isExpressiveTheme(this)
                ? R.style.Theme_SubSettings_Expressive : R.style.Theme_SubSettings;
        setTheme(themeId);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.quick_launch_edit_activity);

        mHelper = QuickLaunchHelper.getInstance(this);
        if (!mHelper.isUdfpsSupported()) {
            finish();
            return;
        }
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initToolbar();
        initArcPreview();
        initTabsAndViewPager();
        loadData();
    }

    private void initToolbar() {
        ImageView btnClose = findViewById(R.id.edit_btn_close);
        ImageView btnSave = findViewById(R.id.edit_btn_save);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndFinish();
            }
        });
    }

    private void initArcPreview() {
        mArcRecyclerView = findViewById(R.id.edit_arc_recycler);
        mArcRecyclerView.setLayoutManager(new ArcLayoutManager(getResources()));
        mArcAdapter = new QuickLaunchArcAdapter(this, true );

        mArcAdapter.setOnSlotActionListener(new QuickLaunchArcAdapter.OnSlotActionListener() {
            @Override
            public void onSlotClick(int position, QuickLaunchItem item) {
                            }

            @Override
            public void onSlotRemove(int position, QuickLaunchItem item) {
                vibrateShort();
                if (mFunctionsAdapter != null) {
                    mFunctionsAdapter.markItemDisabled(item, false);
                }
                if (mAppsAdapter != null) {
                    mAppsAdapter.markItemDisabled(item, false);
                }
            }
        });

        mArcRecyclerView.setAdapter(mArcAdapter);

                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                mArcAdapter.swapItems(fromPos, toPos);
                vibrateShort();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                            }

            @Override
            public int getDragDirs(@NonNull RecyclerView recyclerView,
                                   @NonNull RecyclerView.ViewHolder viewHolder) {
                int pos = viewHolder.getAdapterPosition();
                if (pos >= 0 && pos < mArcAdapter.getItems().size()) {
                    if (mArcAdapter.getItems().get(pos).isEmpty()) {
                        return 0;
                    }
                }
                return super.getDragDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        });
        itemTouchHelper.attachToRecyclerView(mArcRecyclerView);
    }

    private void initTabsAndViewPager() {
        mTabLayout = findViewById(R.id.edit_tab_layout);
        mViewPager = findViewById(R.id.edit_viewpager);

        mFunctionsAdapter = new QuickLaunchCategoryListAdapter(this);
        mAppsAdapter = new QuickLaunchCategoryListAdapter(this);

        QuickLaunchCategoryListAdapter.OnItemClickListener itemClickListener = new QuickLaunchCategoryListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(QuickLaunchItem item) {
                tryAddItemToArc(item);
            }
        };

        mFunctionsAdapter.setOnItemClickListener(itemClickListener);
        mAppsAdapter.setOnItemClickListener(itemClickListener);

        mViewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return TabContentFragment.newInstance(position);
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(mTabLayout, mViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                if (position == 0) {
                    tab.setText(R.string.fingerprint_ql_select_shortcuts_function);
                } else {
                    tab.setText(R.string.fingerprint_ql_select_application);
                }
            }
        }).attach();
    }

    private void loadData() {
        final List<QuickLaunchItem> currentSlots = mHelper.getQuickLaunchItems();
        mArcAdapter.setItems(currentSlots);
        final List<QuickLaunchItem> slotsSnapshot = new java.util.ArrayList<>(currentSlots);

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<QuickLaunchItem> shortcuts = mHelper.queryAllShortcuts(slotsSnapshot);
                final List<QuickLaunchItem> apps = mHelper.queryAllApps(slotsSnapshot);

                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !isDestroyed()) {
                            mFunctionsAdapter.setFunctions(shortcuts);
                            mAppsAdapter.setApps(apps);
                        }
                    }
                });
            }
        });
    }

    private void tryAddItemToArc(QuickLaunchItem item) {
        boolean added = mArcAdapter.addItem(item);
        if (added) {
            vibrateShort();
            if (mFunctionsAdapter != null) {
                mFunctionsAdapter.markItemDisabled(item, true);
            }
            if (mAppsAdapter != null) {
                mAppsAdapter.markItemDisabled(item, true);
            }
        } else {
            Toast.makeText(this, R.string.fingerprint_ql_setting_exceed_tips,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAndFinish() {
        mHelper.saveQuickLaunchItems(mArcAdapter.getItems());
        finish();
    }

    private void vibrateShort() {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }

    public static class TabContentFragment extends Fragment {
        private static final String ARG_POSITION = "arg_tab_position";

        private QuickLaunchCategoryListAdapter mAdapter;
        private RecyclerView.AdapterDataObserver mDataObserver;

        public static TabContentFragment newInstance(int position) {
            TabContentFragment fragment = new TabContentFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_POSITION, position);
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.quick_launch_tab_fragment, container, false);
            RecyclerView rv = root.findViewById(R.id.tab_recycler_view);
            final ProgressBar progress = root.findViewById(R.id.tab_loading_progress);
            final TextView emptyView = root.findViewById(R.id.tab_empty_text);

            rv.setLayoutManager(new LinearLayoutManager(getContext()));

            EditQuickLaunchActivity activity = (EditQuickLaunchActivity) getActivity();
            int tabIndex = getArguments() != null ? getArguments().getInt(ARG_POSITION, 0) : 0;

            mAdapter = (tabIndex == 0)
                    ? activity.mFunctionsAdapter
                    : activity.mAppsAdapter;
            rv.setAdapter(mAdapter);

            emptyView.setText(tabIndex == 0
                    ? R.string.fingerprint_ql_no_functions
                    : R.string.fingerprint_ql_no_apps);

            mDataObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    if (progress != null && emptyView != null && mAdapter != null) {
                        progress.setVisibility(View.GONE);
                        emptyView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    }
                }
            };
            mAdapter.registerAdapterDataObserver(mDataObserver);

            if (mAdapter.getItemCount() > 0) {
                progress.setVisibility(View.GONE);
            }

            return root;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (mAdapter != null && mDataObserver != null) {
                mAdapter.unregisterAdapterDataObserver(mDataObserver);
                mDataObserver = null;
            }
        }
    }
}
