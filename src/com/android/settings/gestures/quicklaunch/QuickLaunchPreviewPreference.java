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
import android.content.Intent;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.List;

public class QuickLaunchPreviewPreference extends Preference {

    private QuickLaunchArcAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private FrameLayout mPreviewCard;
    private TextView mEditBtn;
    private boolean mIsEnabled = true;

    public QuickLaunchPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.quick_launch_preview_pref);
        setSelectable(false);
    }

    public QuickLaunchPreviewPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mRecyclerView = (RecyclerView) holder.findViewById(R.id.preview_arc_recycler);
        mPreviewCard = (FrameLayout) holder.findViewById(R.id.preview_card);
        mEditBtn = (TextView) holder.findViewById(R.id.preview_edit_btn);

        if (mRecyclerView != null) {
            if (mRecyclerView.getAdapter() == null) {
                mRecyclerView.setLayoutManager(new ArcLayoutManager(getContext().getResources()));
                mAdapter = new QuickLaunchArcAdapter(getContext(), false);
                mAdapter.setOnSlotActionListener(new QuickLaunchArcAdapter.OnSlotActionListener() {
                    @Override
                    public void onSlotClick(int position, QuickLaunchItem item) {
                        if (mIsEnabled) {
                            launchEditActivity();
                        }
                    }

                    @Override
                    public void onSlotRemove(int position, QuickLaunchItem item) {
                    }
                });
                mRecyclerView.setAdapter(mAdapter);

                final int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                    private float mStartY;

                    @Override
                    public boolean onInterceptTouchEvent(@NonNull RecyclerView r, @NonNull MotionEvent e) {
                        switch (e.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                mStartY = e.getY();
                                r.getParent().requestDisallowInterceptTouchEvent(false);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (Math.abs(e.getY() - mStartY) > touchSlop) {
                                    r.getParent().requestDisallowInterceptTouchEvent(false);
                                }
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                r.getParent().requestDisallowInterceptTouchEvent(false);
                                break;
                        }
                        return false;
                    }

                    @Override
                    public void onTouchEvent(@NonNull RecyclerView r, @NonNull MotionEvent e) {}

                    @Override
                    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
                });

                mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
                    private float mDownY;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                mDownY = event.getY();
                                v.getParent().requestDisallowInterceptTouchEvent(false);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (Math.abs(event.getY() - mDownY) > touchSlop) {
                                    v.getParent().requestDisallowInterceptTouchEvent(false);
                                }
                                break;
                        }
                        return false;
                    }
                });
            }
            loadItems();
        }

        if (mPreviewCard != null) {
            mPreviewCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mIsEnabled) {
                        launchEditActivity();
                    }
                }
            });
        }

        if (mEditBtn != null) {
            mEditBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mIsEnabled) {
                        launchEditActivity();
                    }
                }
            });
        }

        updateEnabledState();
    }

    public void setQuickLaunchEnabled(boolean enabled) {
        mIsEnabled = enabled;
        updateEnabledState();
    }

    private void updateEnabledState() {
        if (mPreviewCard != null) {
            mPreviewCard.setAlpha(mIsEnabled ? 1.0f : 0.4f);
        }
        if (mEditBtn != null) {
            mEditBtn.setEnabled(mIsEnabled);
            mEditBtn.setAlpha(mIsEnabled ? 1.0f : 0.4f);
        }
    }

    public void loadItems() {
        if (mAdapter != null) {
            List<QuickLaunchItem> items = QuickLaunchHelper.getInstance(getContext()).getQuickLaunchItems();
            mAdapter.setItems(items);
        }
    }

    private void launchEditActivity() {
        Intent intent = new Intent(getContext(), EditQuickLaunchActivity.class);
        getContext().startActivity(intent);
    }
}
