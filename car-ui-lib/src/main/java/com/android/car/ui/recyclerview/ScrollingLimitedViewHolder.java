/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * {@link RecyclerView.ViewHolder} for the last item in a scrolling limited list.
 */
public final class ScrollingLimitedViewHolder extends RecyclerView.ViewHolder {

    private final TextView mMessage;

    /**
     * Return an instance of {@link ScrollingLimitedViewHolder} with an already inflated root view.
     * @param parent - the parent {@link ViewGroup} to use during inflation of the root view.
     */
    public static ScrollingLimitedViewHolder create(@NonNull ViewGroup parent) {
        View rootView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.car_ui_list_limiting_message, parent, false);
        return new ScrollingLimitedViewHolder(rootView);
    }

    ScrollingLimitedViewHolder(@NonNull View itemView) {
        super(itemView);
        mMessage = CarUiUtils.requireViewByRefId(itemView, R.id.car_ui_list_limiting_message);
    }

    /**
     * Update the content of this {@link ScrollingLimitedViewHolder} object using the provided
     * message String resource id.
     * @param messageId
     */
    public void bind(@StringRes @Nullable Integer messageId) {
        int resId = (messageId != null) ? messageId : R.string.car_ui_scrolling_limited_message;
        mMessage.setText(mMessage.getContext().getString(resId));
    }
}
