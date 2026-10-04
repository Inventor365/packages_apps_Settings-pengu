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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class QuickLaunchCategoryListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(QuickLaunchItem item);
    }

    public static class ListItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_CONTENT = 1;

        public static final int SHAPE_SINGLE = 0;
        public static final int SHAPE_TOP = 1;
        public static final int SHAPE_MIDDLE = 2;
        public static final int SHAPE_BOTTOM = 3;

        public final int type;
        public final String headerTitle;
        public final QuickLaunchItem item;
        public int shape;

        public static ListItem header(String title) {
            return new ListItem(TYPE_HEADER, title, null, 0);
        }

        public static ListItem content(QuickLaunchItem item, int shape) {
            return new ListItem(TYPE_CONTENT, null, item, shape);
        }

        private ListItem(int type, String headerTitle, QuickLaunchItem item, int shape) {
            this.type = type;
            this.headerTitle = headerTitle;
            this.item = item;
            this.shape = shape;
        }
    }

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final List<ListItem> mDisplayList = new ArrayList<>();
    private OnItemClickListener mListener;

    public QuickLaunchCategoryListAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    
    public void setFunctions(List<QuickLaunchItem> shortcuts) {
        Map<String, List<QuickLaunchItem>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (shortcuts != null) {
            for (QuickLaunchItem item : shortcuts) {
                String appLabel = item.getAppLabel();
                if (TextUtils.isEmpty(appLabel)) {
                    appLabel = item.getPackageName();
                }
                List<QuickLaunchItem> list = grouped.get(appLabel);
                if (list == null) {
                    list = new ArrayList<>();
                    grouped.put(appLabel, list);
                }
                list.add(item);
            }
        }

        List<ListItem> displayList = new ArrayList<>();
        for (Map.Entry<String, List<QuickLaunchItem>> entry : grouped.entrySet()) {
            String appName = entry.getKey();
            List<QuickLaunchItem> itemsInApp = entry.getValue();

                        Collections.sort(itemsInApp, new Comparator<QuickLaunchItem>() {
                @Override
                public int compare(QuickLaunchItem o1, QuickLaunchItem o2) {
                    return o1.getTitle().compareToIgnoreCase(o2.getTitle());
                }
            });

                        displayList.add(ListItem.header(appName));

            int count = itemsInApp.size();
            for (int i = 0; i < count; i++) {
                int shape;
                if (count == 1) {
                    shape = ListItem.SHAPE_SINGLE;
                } else if (i == 0) {
                    shape = ListItem.SHAPE_TOP;
                } else if (i == count - 1) {
                    shape = ListItem.SHAPE_BOTTOM;
                } else {
                    shape = ListItem.SHAPE_MIDDLE;
                }
                displayList.add(ListItem.content(itemsInApp.get(i), shape));
            }
        }

        mDisplayList.clear();
        mDisplayList.addAll(displayList);
        notifyDataSetChanged();
    }

    
    public void setApps(List<QuickLaunchItem> apps) {
        Map<String, List<QuickLaunchItem>> grouped = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                boolean isS1Letter = !s1.isEmpty() && Character.isLetter(s1.charAt(0));
                boolean isS2Letter = !s2.isEmpty() && Character.isLetter(s2.charAt(0));
                if (isS1Letter && isS2Letter) {
                    return s1.compareToIgnoreCase(s2);
                }
                if (isS1Letter) return -1;
                if (isS2Letter) return 1;
                return s1.compareToIgnoreCase(s2);
            }
        });

        if (apps != null) {
            for (QuickLaunchItem item : apps) {
                String title = item.getTitle();
                String section = "#";
                if (!TextUtils.isEmpty(title)) {
                    char firstChar = title.trim().charAt(0);
                    if (Character.isLetter(firstChar)) {
                        section = String.valueOf(Character.toUpperCase(firstChar));
                    }
                }
                List<QuickLaunchItem> list = grouped.get(section);
                if (list == null) {
                    list = new ArrayList<>();
                    grouped.put(section, list);
                }
                list.add(item);
            }
        }

        List<ListItem> displayList = new ArrayList<>();
        for (Map.Entry<String, List<QuickLaunchItem>> entry : grouped.entrySet()) {
            String section = entry.getKey();
            List<QuickLaunchItem> itemsInSection = entry.getValue();

                        Collections.sort(itemsInSection, new Comparator<QuickLaunchItem>() {
                @Override
                public int compare(QuickLaunchItem o1, QuickLaunchItem o2) {
                    return o1.getTitle().compareToIgnoreCase(o2.getTitle());
                }
            });

                        displayList.add(ListItem.header(section));

            int count = itemsInSection.size();
            for (int i = 0; i < count; i++) {
                int shape;
                if (count == 1) {
                    shape = ListItem.SHAPE_SINGLE;
                } else if (i == 0) {
                    shape = ListItem.SHAPE_TOP;
                } else if (i == count - 1) {
                    shape = ListItem.SHAPE_BOTTOM;
                } else {
                    shape = ListItem.SHAPE_MIDDLE;
                }
                displayList.add(ListItem.content(itemsInSection.get(i), shape));
            }
        }

        mDisplayList.clear();
        mDisplayList.addAll(displayList);
        notifyDataSetChanged();
    }

    public void updateSelection(Set<String> selectedKeys) {
        for (int i = 0; i < mDisplayList.size(); i++) {
            ListItem listItem = mDisplayList.get(i);
            if (listItem.type == ListItem.TYPE_CONTENT && listItem.item != null) {
                String key = QuickLaunchHelper.getItemKey(listItem.item);
                int newAction = (selectedKeys != null && selectedKeys.contains(key))
                        ? QuickLaunchItem.VIEW_ACTION_SELECTED
                        : QuickLaunchItem.VIEW_ACTION_NORMAL;
                if (listItem.item.getViewAction() != newAction) {
                    listItem.item.setViewAction(newAction);
                    notifyItemChanged(i);
                }
            }
        }
    }

    public void markItemDisabled(QuickLaunchItem targetItem, boolean disabled) {
        if (targetItem == null) return;
        String targetKey = QuickLaunchHelper.getItemKey(targetItem);
        for (int i = 0; i < mDisplayList.size(); i++) {
            ListItem listItem = mDisplayList.get(i);
            if (listItem.type == ListItem.TYPE_CONTENT && listItem.item != null) {
                if (targetKey.equals(QuickLaunchHelper.getItemKey(listItem.item))) {
                    listItem.item.setViewAction(disabled
                            ? QuickLaunchItem.VIEW_ACTION_SELECTED
                            : QuickLaunchItem.VIEW_ACTION_NORMAL);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mDisplayList.get(position).type;
    }

    @Override
    public int getItemCount() {
        return mDisplayList.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ListItem.TYPE_HEADER) {
            View view = mInflater.inflate(R.layout.quick_launch_category_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = mInflater.inflate(R.layout.quick_launch_list_item, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem listItem = mDisplayList.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).title.setText(listItem.headerTitle);
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            final QuickLaunchItem item = listItem.item;
            if (item == null) return;

                        switch (listItem.shape) {
                case ListItem.SHAPE_SINGLE:
                    itemHolder.itemView.setBackgroundResource(
                            com.android.settingslib.widget.theme.R.drawable.settingslib_round_background);
                    break;
                case ListItem.SHAPE_TOP:
                    itemHolder.itemView.setBackgroundResource(
                            com.android.settingslib.widget.theme.R.drawable.settingslib_round_background_top);
                    break;
                case ListItem.SHAPE_MIDDLE:
                    itemHolder.itemView.setBackgroundResource(
                            com.android.settingslib.widget.theme.R.drawable.settingslib_round_background_center);
                    break;
                case ListItem.SHAPE_BOTTOM:
                    itemHolder.itemView.setBackgroundResource(
                            com.android.settingslib.widget.theme.R.drawable.settingslib_round_background_bottom);
                    break;
            }
            itemHolder.title.setText(item.getTitle());

                        if (item.getViewType() == QuickLaunchItem.VIEW_TYPE_SHORTCUT
                    && !TextUtils.isEmpty(item.getAppLabel())
                    && !item.getAppLabel().equalsIgnoreCase(item.getTitle())) {
                itemHolder.subtitle.setText(item.getAppLabel());
                itemHolder.subtitle.setVisibility(View.VISIBLE);
            } else {
                itemHolder.subtitle.setVisibility(View.GONE);
            }

                        itemHolder.icon.setImageDrawable(item.getIcon());
            if (item.getSubIcon() != null) {
                itemHolder.subIcon.setImageDrawable(item.getSubIcon());
                itemHolder.subIcon.setVisibility(View.VISIBLE);
            } else {
                itemHolder.subIcon.setVisibility(View.GONE);
            }

                        final boolean isSelected = item.getViewAction() == QuickLaunchItem.VIEW_ACTION_SELECTED;
            itemHolder.itemView.setAlpha(1.0f);
            if (isSelected) {
                itemHolder.icon.setAlpha(0.5f);
                itemHolder.title.setAlpha(0.5f);
                itemHolder.subtitle.setAlpha(0.5f);
                itemHolder.addBtn.setVisibility(View.GONE);
                itemHolder.checkedBadge.setVisibility(View.VISIBLE);
                itemHolder.checkedBadge.setAlpha(1.0f);
            } else {
                itemHolder.icon.setAlpha(1.0f);
                itemHolder.title.setAlpha(1.0f);
                itemHolder.subtitle.setAlpha(1.0f);
                itemHolder.addBtn.setVisibility(View.VISIBLE);
                itemHolder.checkedBadge.setVisibility(View.GONE);
            }

            itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null && !isSelected) {
                        mListener.onItemClick(item);
                    }
                }
            });
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.category_header_title);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final ImageView subIcon;
        final TextView title;
        final TextView subtitle;
        final ImageView addBtn;
        final ImageView checkedBadge;

        ItemViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.list_item_icon);
            subIcon = itemView.findViewById(R.id.list_item_sub_icon);
            title = itemView.findViewById(R.id.list_item_title);
            subtitle = itemView.findViewById(R.id.list_item_subtitle);
            addBtn = itemView.findViewById(R.id.list_item_add_btn);
            checkedBadge = itemView.findViewById(R.id.list_item_checked_badge);
        }
    }
}
