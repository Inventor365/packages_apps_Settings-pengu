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
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QuickLaunchItem implements Parcelable {

    private static final String TAG = "QuickLaunchItem";

    public static final int MAX_ITEMS = 5;

    public static final int VIEW_TYPE_EMPTY = 0;
    public static final int VIEW_TYPE_APP = 1;
    public static final int VIEW_TYPE_SHORTCUT = 2;

    public static final int VIEW_ACTION_NORMAL = 0;
    public static final int VIEW_ACTION_SELECTED = 1; 

    public static final String KEY_PACKAGE_NAME = "packageName";
    public static final String KEY_CLASS_NAME = "className";
    public static final String KEY_SHORTCUT_ID = "shortcutId";
    public static final String KEY_TITLE = "title";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_POSITION = "position";
    public static final String KEY_VIEW_TYPE = "viewType";
    public static final String KEY_APP_LABEL = "appLabel";

    private String mPackageName = "";
    private String mClassName = "";
    private String mShortcutId = "";
    private String mTitle = "";
    private String mAppLabel = "";
    private int mUserId = 0;
    private int mPosition = 0;
    private int mViewType = VIEW_TYPE_EMPTY;
    private int mViewAction = VIEW_ACTION_NORMAL;
    private boolean mIsClone = false;

        private Drawable mIcon;
    private Drawable mSubIcon; 

    public QuickLaunchItem() {
        this(VIEW_TYPE_EMPTY, 0);
    }

    public QuickLaunchItem(int viewType, int position) {
        mViewType = viewType;
        mPosition = position;
    }

    protected QuickLaunchItem(Parcel in) {
        mPackageName = in.readString();
        mClassName = in.readString();
        mShortcutId = in.readString();
        mTitle = in.readString();
        mAppLabel = in.readString();
        mUserId = in.readInt();
        mPosition = in.readInt();
        mViewType = in.readInt();
        mViewAction = in.readInt();
        mIsClone = in.readByte() != 0;
    }

    public static final Creator<QuickLaunchItem> CREATOR = new Creator<QuickLaunchItem>() {
        @Override
        public QuickLaunchItem createFromParcel(Parcel in) {
            return new QuickLaunchItem(in);
        }

        @Override
        public QuickLaunchItem[] newArray(int size) {
            return new QuickLaunchItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPackageName);
        dest.writeString(mClassName);
        dest.writeString(mShortcutId);
        dest.writeString(mTitle);
        dest.writeString(mAppLabel);
        dest.writeInt(mUserId);
        dest.writeInt(mPosition);
        dest.writeInt(mViewType);
        dest.writeInt(mViewAction);
        dest.writeByte((byte) (mIsClone ? 1 : 0));
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName != null ? packageName : "";
    }

    public String getClassName() {
        return mClassName;
    }

    public void setClassName(String className) {
        mClassName = className != null ? className : "";
    }

    public String getShortcutId() {
        return mShortcutId;
    }

    public void setShortcutId(String shortcutId) {
        mShortcutId = shortcutId != null ? shortcutId : "";
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title != null ? title : "";
    }

    public String getAppLabel() {
        return mAppLabel;
    }

    public void setAppLabel(String appLabel) {
        mAppLabel = appLabel != null ? appLabel : "";
    }

    public int getUserId() {
        return mUserId;
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public int getViewType() {
        return mViewType;
    }

    public void setViewType(int viewType) {
        mViewType = viewType;
    }

    public int getViewAction() {
        return mViewAction;
    }

    public void setViewAction(int viewAction) {
        mViewAction = viewAction;
    }

    public boolean isClone() {
        return mIsClone;
    }

    public void setClone(boolean clone) {
        mIsClone = clone;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

    public Drawable getSubIcon() {
        return mSubIcon;
    }

    public void setSubIcon(Drawable subIcon) {
        mSubIcon = subIcon;
    }

    public boolean isEmpty() {
        return mViewType == VIEW_TYPE_EMPTY;
    }

    public ComponentName getComponentName() {
        if (!TextUtils.isEmpty(mPackageName) && !TextUtils.isEmpty(mClassName)) {
            return new ComponentName(mPackageName, mClassName);
        }
        return null;
    }

    public QuickLaunchItem copy() {
        QuickLaunchItem copy = new QuickLaunchItem(mViewType, mPosition);
        copy.mPackageName = mPackageName;
        copy.mClassName = mClassName;
        copy.mShortcutId = mShortcutId;
        copy.mTitle = mTitle;
        copy.mAppLabel = mAppLabel;
        copy.mUserId = mUserId;
        copy.mViewAction = mViewAction;
        copy.mIsClone = mIsClone;
        copy.mIcon = mIcon;
        copy.mSubIcon = mSubIcon;
        return copy;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put(KEY_PACKAGE_NAME, mPackageName);
            obj.put(KEY_CLASS_NAME, mClassName);
            obj.put(KEY_SHORTCUT_ID, mShortcutId);
            obj.put(KEY_TITLE, mTitle);
            obj.put(KEY_APP_LABEL, mAppLabel);
            obj.put(KEY_USER_ID, mUserId);
            obj.put(KEY_POSITION, mPosition);
            obj.put(KEY_VIEW_TYPE, mViewType);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize QuickLaunchItem: " + e.getMessage());
        }
        return obj;
    }

    public static QuickLaunchItem fromJsonObject(JSONObject obj) {
        QuickLaunchItem item = new QuickLaunchItem();
        if (obj == null) return item;
        item.mPackageName = obj.optString(KEY_PACKAGE_NAME, "");
        item.mClassName = obj.optString(KEY_CLASS_NAME, "");
        item.mShortcutId = obj.optString(KEY_SHORTCUT_ID, "");
        item.mTitle = obj.optString(KEY_TITLE, "");
        item.mAppLabel = obj.optString(KEY_APP_LABEL, "");
        item.mUserId = obj.optInt(KEY_USER_ID, 0);
        item.mPosition = obj.optInt(KEY_POSITION, 0);
        item.mViewType = obj.optInt(KEY_VIEW_TYPE, VIEW_TYPE_EMPTY);
        return item;
    }

    public static List<QuickLaunchItem> createDefaultSlots() {
        List<QuickLaunchItem> slots = new ArrayList<>(MAX_ITEMS);
        for (int i = 0; i < MAX_ITEMS; i++) {
            slots.add(new QuickLaunchItem(VIEW_TYPE_EMPTY, i));
        }
        return slots;
    }

    public static List<QuickLaunchItem> parseJson(String jsonStr) {
        if (TextUtils.isEmpty(jsonStr)) {
            return createDefaultSlots();
        }
        try {
            JSONArray array = new JSONArray(jsonStr);
            List<QuickLaunchItem> items = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    QuickLaunchItem item = fromJsonObject(obj);
                    item.setPosition(i);
                    items.add(item);
                }
            }
            while (items.size() < MAX_ITEMS) {
                items.add(new QuickLaunchItem(VIEW_TYPE_EMPTY, items.size()));
            }
            return items;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing QuickLaunch items json: " + e.getMessage());
            return createDefaultSlots();
        }
    }

    public static String toJsonString(List<QuickLaunchItem> items) {
        if (items == null) return "[]";
        JSONArray array = new JSONArray();
        for (QuickLaunchItem item : items) {
            array.put(item.toJsonObject());
        }
        return array.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuickLaunchItem)) return false;
        QuickLaunchItem that = (QuickLaunchItem) o;
        if (mViewType == VIEW_TYPE_EMPTY && that.mViewType == VIEW_TYPE_EMPTY) {
            return mPosition == that.mPosition;
        }
        return mUserId == that.mUserId
                && Objects.equals(mPackageName, that.mPackageName)
                && Objects.equals(mClassName, that.mClassName)
                && Objects.equals(mShortcutId, that.mShortcutId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackageName, mClassName, mShortcutId, mUserId);
    }

    @Override
    public String toString() {
        return "QuickLaunchItem{" +
                "pkg='" + mPackageName + '\'' +
                ", class='" + mClassName + '\'' +
                ", shortcut='" + mShortcutId + '\'' +
                ", title='" + mTitle + '\'' +
                ", pos=" + mPosition +
                ", type=" + mViewType +
                ", action=" + mViewAction +
                '}';
    }
}
