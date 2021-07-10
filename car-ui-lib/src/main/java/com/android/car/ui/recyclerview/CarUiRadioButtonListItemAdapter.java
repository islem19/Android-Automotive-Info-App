/*
 * Copyright 2020 The Android Open Source Project
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

package com.android.car.ui.recyclerview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

import java.util.List;

/**
 * Adapter for {@link CarUiRecyclerView} to display {@link CarUiRadioButtonListItem}. This adapter
 * allows for at most one item to be selected at a time.
 *
 * <ul>
 * <li> Implements {@link CarUiRecyclerView.ItemCap} - defaults to unlimited item count.
 * </ul>
 */
public class CarUiRadioButtonListItemAdapter extends CarUiListItemAdapter {

    private int mSelectedIndex = -1;

    public CarUiRadioButtonListItemAdapter(List<CarUiRadioButtonListItem> items) {
        super(items);
        for (int i = 0; i < items.size(); i++) {
            CarUiRadioButtonListItem item = items.get(i);
            if (item.isChecked() && mSelectedIndex >= 0) {
                throw new IllegalStateException(
                        "At most one item in a CarUiRadioButtonListItemAdapter can be checked");
            }

            if (item.isChecked()) {
                mSelectedIndex = i;
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_LIST_ITEM) {
            return new RadioButtonListItemViewHolder(
                    inflater.inflate(R.layout.car_ui_list_item, parent, false));
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_LIST_ITEM) {
            if (!(holder instanceof RadioButtonListItemViewHolder)) {
                throw new IllegalStateException("Incorrect view holder type for list item.");
            }

            CarUiListItem item = getItems().get(position);
            if (!(item instanceof CarUiRadioButtonListItem)) {
                throw new IllegalStateException(
                        "Expected item to be bound to viewHolder to be instance of "
                                + "CarUiRadioButtonListItem.");
            }

            RadioButtonListItemViewHolder actualHolder = ((RadioButtonListItemViewHolder) holder);
            actualHolder.bind((CarUiRadioButtonListItem) item);
            actualHolder.setOnCheckedChangeListener(isChecked -> {
                if (isChecked && mSelectedIndex >= 0) {
                    CarUiRadioButtonListItem previousSelectedItem =
                            (CarUiRadioButtonListItem) getItems().get(mSelectedIndex);
                    previousSelectedItem.setChecked(false);
                    notifyItemChanged(mSelectedIndex);
                }

                if (isChecked) {
                    mSelectedIndex = position;
                    CarUiRadioButtonListItem currentSelectedItem =
                            (CarUiRadioButtonListItem) getItems().get(mSelectedIndex);
                    currentSelectedItem.setChecked(true);
                    notifyItemChanged(mSelectedIndex);
                }
            });

        } else {
            super.onBindViewHolder(holder, position);
        }
    }

    /*
     * @return the position of the currently selected item, -1 if no item is selected.
     */
    public int getSelectedItemPosition() {
        return mSelectedIndex;
    }

    static class RadioButtonListItemViewHolder extends ListItemViewHolder {
        /**
         * Callback to be invoked when the checked state of a {@link RadioButtonListItemViewHolder}
         * changed.
         */
        public interface OnCheckedChangeListener {
            /**
             * Called when the checked state of a {@link RadioButtonListItemViewHolder} has changed.
             *
             * @param isChecked new checked state of list item.
             */
            void onCheckedChanged(boolean isChecked);
        }

        @Nullable
        private OnCheckedChangeListener mListener;

        RadioButtonListItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
            mListener = listener;
        }

        @Override
        void bind(@NonNull CarUiContentListItem item) {
            super.bind(item);
            mRadioButton.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {
                        item.setChecked(isChecked);
                        if (mListener != null) {
                            mListener.onCheckedChanged(isChecked);
                        }
                    });
        }
    }
}
