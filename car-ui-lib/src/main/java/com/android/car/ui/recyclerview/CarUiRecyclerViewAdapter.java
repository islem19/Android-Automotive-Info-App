/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.car.ui.utils.CarUiUtils.findViewByRefId;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

/** The adapter for the parent recyclerview in {@link CarUiRecyclerView} widget. */
final class CarUiRecyclerViewAdapter
        extends RecyclerView.Adapter<CarUiRecyclerViewAdapter.NestedRowViewHolder> {

    @Override
    public CarUiRecyclerViewAdapter.NestedRowViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.car_ui_recycler_view_item, parent, false);
        return new NestedRowViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager). Intentionally left empty
    // since this adapter is an empty shell for the nested recyclerview.
    @Override
    public void onBindViewHolder(@NonNull NestedRowViewHolder holder, int position) {
    }

    // Return the size of your data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return 1;
    }

    /** The viewHolder class for the parent recyclerview. */
    static class NestedRowViewHolder extends RecyclerView.ViewHolder {
        public final FrameLayout frameLayout;

        NestedRowViewHolder(View view) {
            super(view);
            frameLayout = findViewByRefId(view, R.id.nested_recycler_view_layout);
        }
    }
}
