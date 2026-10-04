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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuickLaunchArcAdapter extends RecyclerView.Adapter<QuickLaunchArcAdapter.SlotViewHolder> {

    public interface OnSlotActionListener {
        void onSlotClick(int position, QuickLaunchItem item);
        void onSlotRemove(int position, QuickLaunchItem item);
    }

    private final Context mContext;
    private final LayoutInflater mInflater;
    private List<QuickLaunchItem> mItems = new ArrayList<>(QuickLaunchItem.MAX_ITEMS);
    private boolean mIsEditMode;
    private OnSlotActionListener mListener;

    public QuickLaunchArcAdapter(Context context, boolean isEditMode) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mIsEditMode = isEditMode;
        setItems(QuickLaunchItem.createDefaultSlots());
    }

    public void setItems(List<QuickLaunchItem> items) {
        mItems.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        while (mItems.size() < QuickLaunchItem.MAX_ITEMS) {
            mItems.add(new QuickLaunchItem(QuickLaunchItem.VIEW_TYPE_EMPTY, mItems.size()));
        }
        notifyDataSetChanged();
    }

    public List<QuickLaunchItem> getItems() {
        return mItems;
    }

    public void setOnSlotActionListener(OnSlotActionListener listener) {
        mListener = listener;
    }

    public void swapItems(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= mItems.size()
                || toPosition < 0 || toPosition >= mItems.size()) {
            return;
        }
        Collections.swap(mItems, fromPosition, toPosition);
        mItems.get(fromPosition).setPosition(fromPosition);
        mItems.get(toPosition).setPosition(toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public boolean addItem(QuickLaunchItem itemToAdd) {
        if (itemToAdd == null) return false;

                for (QuickLaunchItem slot : mItems) {
            if (!slot.isEmpty() && slot.equals(itemToAdd)) {
                return false;
            }
        }

                for (int slotIndex : QuickLaunchHelper.SLOT_CHECK_ORDER) {
            if (slotIndex < mItems.size() && mItems.get(slotIndex).isEmpty()) {
                QuickLaunchItem copy = itemToAdd.copy();
                copy.setPosition(slotIndex);
                copy.setViewAction(QuickLaunchItem.VIEW_ACTION_SELECTED);
                mItems.set(slotIndex, copy);
                notifyItemChanged(slotIndex);
                return true;
            }
        }
        return false;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < mItems.size()) {
            QuickLaunchItem oldItem = mItems.get(position);
            QuickLaunchItem emptySlot = new QuickLaunchItem(QuickLaunchItem.VIEW_TYPE_EMPTY, position);
            QuickLaunchHelper.getInstance(mContext).loadIconForItem(emptySlot);
            mItems.set(position, emptySlot);
            notifyItemChanged(position);
            if (mListener != null && !oldItem.isEmpty()) {
                mListener.onSlotRemove(position, oldItem);
            }
        }
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.quick_launch_slot_item, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        final QuickLaunchItem item = mItems.get(position);
        final int pos = position;

        if (item.isEmpty()) {
            holder.slotBg.setBackground(ContextCompat.getDrawable(mContext,
                    R.drawable.fp_ql_slot_empty_border));
            holder.slotIcon.setImageDrawable(ContextCompat.getDrawable(mContext,
                    R.drawable.kgd_fp_quick_launch_empty_icon));
            holder.slotSubIcon.setVisibility(View.GONE);
            holder.removeBadge.setVisibility(View.GONE);
        } else {
            holder.slotBg.setBackground(ContextCompat.getDrawable(mContext,
                    R.drawable.fp_ql_slot_filled_bg));
            holder.slotIcon.setImageDrawable(item.getIcon());

            if (item.getSubIcon() != null) {
                holder.slotSubIcon.setImageDrawable(item.getSubIcon());
                holder.slotSubIcon.setVisibility(View.VISIBLE);
            } else {
                holder.slotSubIcon.setVisibility(View.GONE);
            }

            if (mIsEditMode) {
                holder.removeBadge.setVisibility(View.VISIBLE);
            } else {
                holder.removeBadge.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsEditMode) {
                    if (!item.isEmpty()) {
                        removeItem(pos);
                    }
                } else {
                    if (mListener != null) {
                        mListener.onSlotClick(pos, item);
                    }
                }
            }
        });

        holder.removeBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsEditMode && !item.isEmpty()) {
                    removeItem(pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        FrameLayout container;
        ImageView slotBg;
        ImageView slotIcon;
        ImageView slotSubIcon;
        ImageView removeBadge;

        SlotViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.slot_container);
            slotBg = itemView.findViewById(R.id.slot_bg);
            slotIcon = itemView.findViewById(R.id.slot_icon);
            slotSubIcon = itemView.findViewById(R.id.slot_sub_icon);
            removeBadge = itemView.findViewById(R.id.slot_remove_badge);
        }
    }
}
