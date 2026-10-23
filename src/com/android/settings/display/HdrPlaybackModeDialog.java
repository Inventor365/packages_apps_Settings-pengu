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

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager.AppHdrPlaybackMode;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

/**
 * Dialog for selecting per-application HDR playback policy, styled to match the
 * Yuki Charging Service / PowerProfile dialog visual language.
 */
public class HdrPlaybackModeDialog extends Dialog {

    public interface OnModeSelectedListener {
        void onModeSelected(@AppHdrPlaybackMode int mode);
    }

    private static final class ModeOption {
        @AppHdrPlaybackMode final int mode;
        final int titleResId;
        final int descResId;
        final int iconResId;

        ModeOption(@AppHdrPlaybackMode int mode, int titleResId, int descResId, int iconResId) {
            this.mode = mode;
            this.titleResId = titleResId;
            this.descResId = descResId;
            this.iconResId = iconResId;
        }
    }

    private static final ModeOption[] MODE_OPTIONS = new ModeOption[] {
        new ModeOption(
                APP_HDR_PLAYBACK_MODE_DEFAULT,
                R.string.hdr_playback_mode_default_title,
                R.string.hdr_playback_mode_default_desc,
                R.drawable.ic_hdr_mode_default),
        new ModeOption(
                APP_HDR_PLAYBACK_MODE_HDR_ALLOWED,
                R.string.hdr_playback_mode_allowed_title,
                R.string.hdr_playback_mode_allowed_desc,
                R.drawable.ic_hdr_mode_allowed),
        new ModeOption(
                APP_HDR_PLAYBACK_MODE_SDR_ONLY,
                R.string.hdr_playback_mode_sdr_only_title,
                R.string.hdr_playback_mode_sdr_only_desc,
                R.drawable.ic_hdr_mode_sdr_only),
    };

    private final Context mContext;
    private final CharSequence mAppLabel;
    private final String mPackageName;
    @Nullable private final Drawable mAppIcon;
    @AppHdrPlaybackMode private int mCurrentMode;
    private final OnModeSelectedListener mListener;

    private LinearLayout mModesContainer;
    private View mResetButton;

    public HdrPlaybackModeDialog(
            @NonNull Context context,
            @NonNull CharSequence appLabel,
            @NonNull String packageName,
            @Nullable Drawable appIcon,
            @AppHdrPlaybackMode int currentMode,
            @NonNull OnModeSelectedListener listener) {
        super(context);
        mContext = context;
        mAppLabel = appLabel;
        mPackageName = packageName;
        mAppIcon = appIcon;
        mCurrentMode = currentMode;
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.hdr_playback_mode_dialog);

        ImageView appIconView = findViewById(R.id.dialog_app_icon);
        TextView titleView = findViewById(R.id.dialog_title);
        TextView subtitleView = findViewById(R.id.dialog_subtitle);
        mModesContainer = findViewById(R.id.modes_container);
        mResetButton = findViewById(R.id.reset_app_button);
        View doneButton = findViewById(R.id.done_button);

        if (appIconView != null) {
            if (mAppIcon != null) {
                appIconView.setImageDrawable(mAppIcon);
                appIconView.setVisibility(View.VISIBLE);
            } else {
                appIconView.setVisibility(View.GONE);
            }
        }
        if (titleView != null) {
            titleView.setText(mAppLabel);
        }
        if (subtitleView != null) {
            subtitleView.setText(mPackageName);
        }

        if (mResetButton != null) {
            mResetButton.setOnClickListener(v -> {
                mCurrentMode = APP_HDR_PLAYBACK_MODE_DEFAULT;
                mListener.onModeSelected(APP_HDR_PLAYBACK_MODE_DEFAULT);
                refreshModes();
                dismiss();
            });
        }
        if (doneButton != null) {
            doneButton.setOnClickListener(v -> dismiss());
        }

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = getDialogWidth(mContext);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
        }

        setCanceledOnTouchOutside(true);
        refreshModes();
    }

    private void refreshModes() {
        if (mModesContainer == null) {
            return;
        }
        mModesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mContext);

        int activeOnContainerColor = resolveColorByName(mContext,
                "materialColorOnPrimaryContainer",
                android.R.attr.textColorPrimaryInverse, 0xFFFFFFFF);
        int inactiveTitleColor = resolveColorByName(mContext,
                "materialColorOnSurface",
                android.R.attr.textColorPrimary, 0xFFFFFFFF);
        int inactiveDescColor = resolveColorByName(mContext,
                "materialColorOnSurfaceVariant",
                android.R.attr.textColorSecondary, 0xAAFFFFFF);
        int accentColor = resolveColor(mContext,
                android.R.attr.colorAccent, 0xFF386540);

        for (ModeOption option : MODE_OPTIONS) {
            View itemView = inflater.inflate(
                    R.layout.hdr_playback_mode_item, mModesContainer, false);
            ImageView iconView = itemView.findViewById(R.id.mode_icon);
            TextView titleView = itemView.findViewById(R.id.mode_title);
            TextView descView = itemView.findViewById(R.id.mode_desc);
            ImageView checkView = itemView.findViewById(R.id.mode_check);

            boolean isActive = (option.mode == mCurrentMode);
            iconView.setImageResource(option.iconResId);
            titleView.setText(option.titleResId);
            descView.setText(option.descResId);

            if (isActive) {
                itemView.setBackgroundResource(R.drawable.hdr_controller_item_bg_active);
                titleView.setTextColor(activeOnContainerColor);
                descView.setTextColor(activeOnContainerColor);
                iconView.setImageTintList(ColorStateList.valueOf(activeOnContainerColor));
                checkView.setVisibility(View.VISIBLE);
                checkView.setImageTintList(ColorStateList.valueOf(activeOnContainerColor));
            } else {
                itemView.setBackgroundResource(R.drawable.hdr_controller_item_bg_inactive);
                titleView.setTextColor(inactiveTitleColor);
                descView.setTextColor(inactiveDescColor);
                iconView.setImageTintList(ColorStateList.valueOf(accentColor));
                checkView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                mCurrentMode = option.mode;
                mListener.onModeSelected(option.mode);
                refreshModes();
            });

            mModesContainer.addView(itemView);
        }

        if (mResetButton != null) {
            mResetButton.setVisibility(
                    mCurrentMode != APP_HDR_PLAYBACK_MODE_DEFAULT ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private static int getDialogWidth(Context context) {
        WindowManager wm = context.getSystemService(WindowManager.class);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        if (wm != null) {
            WindowMetrics metrics = wm.getCurrentWindowMetrics();
            Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Rect bounds = metrics.getBounds();
            screenWidth = bounds.width() - insets.left - insets.right;
        }
        boolean isLandscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        int maxWidthDp = isLandscape ? 460 : 420;
        int maxWidthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, maxWidthDp, dm);
        int marginPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 32, dm);
        return Math.min(screenWidth - marginPx, maxWidthPx);
    }

    static int resolveColor(Context context, int attr, int fallback) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                return context.getColor(tv.resourceId);
            }
            return tv.data;
        }
        return fallback;
    }

    static int resolveColorByName(Context context, String colorResName, int fallbackAttr,
            int fallbackColor) {
        int resId = context.getResources().getIdentifier(colorResName, "color", "android");
        if (resId != 0) {
            try {
                return context.getColor(resId);
            } catch (Exception ignored) {
            }
        }
        return resolveColor(context, fallbackAttr, fallbackColor);
    }
}
