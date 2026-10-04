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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.airbnb.lottie.LottieAnimationView;
import com.android.settings.R;

public class QuickLaunchCarouselPreference extends Preference {

    private static final int[] SLIDE_ANIMATIONS = {
            R.raw.icon_fp_ql_guide_setting_animation,
            R.raw.icon_fp_ql_fast_guide_setting_animation
    };

    private static final int[] SLIDE_TEXTS = {
            R.string.fingerprint_ql_setting_guide_tips_1,
            R.string.fingerprint_ql_setting_guide_tips_2
    };

    public QuickLaunchCarouselPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.quick_launch_carousel_pref);
        setSelectable(false);
    }

    public QuickLaunchCarouselPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final ViewPager2 viewPager = (ViewPager2) holder.findViewById(R.id.guide_viewpager);
        final ImageView dot0 = (ImageView) holder.findViewById(R.id.dot_0);
        final ImageView dot1 = (ImageView) holder.findViewById(R.id.dot_1);

        if (viewPager != null && viewPager.getAdapter() == null) {
            viewPager.setAdapter(new CarouselAdapter(getContext()));
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    if (dot0 != null && dot1 != null) {
                        dot0.setImageResource(position == 0
                                ? R.drawable.fp_ql_indicator_dot_active
                                : R.drawable.fp_ql_indicator_dot);
                        dot1.setImageResource(position == 1
                                ? R.drawable.fp_ql_indicator_dot_active
                                : R.drawable.fp_ql_indicator_dot);
                    }
                }
            });

            View innerView = viewPager.getChildAt(0);
            if (innerView instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) innerView;
                final int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                rv.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                    private float mStartX;
                    private float mStartY;

                    @Override
                    public boolean onInterceptTouchEvent(@NonNull RecyclerView r, @NonNull MotionEvent e) {
                        switch (e.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                mStartX = e.getX();
                                mStartY = e.getY();
                                viewPager.requestDisallowInterceptTouchEvent(true);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                float dx = Math.abs(e.getX() - mStartX);
                                float dy = Math.abs(e.getY() - mStartY);
                                if (dy > touchSlop && dy > dx) {
                                    viewPager.requestDisallowInterceptTouchEvent(false);
                                } else if (dx > touchSlop && dx > dy) {
                                    viewPager.requestDisallowInterceptTouchEvent(true);
                                }
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                viewPager.requestDisallowInterceptTouchEvent(false);
                                break;
                        }
                        return false;
                    }

                    @Override
                    public void onTouchEvent(@NonNull RecyclerView r, @NonNull MotionEvent e) {}

                    @Override
                    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
                });
            }
        }
    }

    private static class CarouselAdapter extends RecyclerView.Adapter<CarouselViewHolder> {

        private final Context mContext;
        private final LayoutInflater mInflater;

        CarouselAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.quick_launch_carousel_slide, parent, false);
            view.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return new CarouselViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
            holder.lottieView.setAnimation(SLIDE_ANIMATIONS[position]);
            holder.lottieView.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
            holder.lottieView.playAnimation();
            holder.textView.setText(mContext.getString(SLIDE_TEXTS[position]));
        }

        @Override
        public int getItemCount() {
            return SLIDE_ANIMATIONS.length;
        }
    }

    private static class CarouselViewHolder extends RecyclerView.ViewHolder {
        LottieAnimationView lottieView;
        TextView textView;

        CarouselViewHolder(View itemView) {
            super(itemView);
            lottieView = itemView.findViewById(R.id.guide_lottie_view);
            textView = itemView.findViewById(R.id.guide_description);
        }
    }
}
