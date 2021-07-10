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
package com.android.car.ui.recyclerview.decorations.linear;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

import java.util.Objects;

/** Adds interior dividers to a RecyclerView with a LinearLayoutManager or its subclass. */
public class LinearDividerItemDecoration extends RecyclerView.ItemDecoration {

    private final Drawable mDivider;
    private int mOrientation;

    /**
     * Sole constructor. Takes in a {@link Drawable} to be used as the interior
     * car_ui_recyclerview_divider.
     *
     * @param divider A car_ui_recyclerview_divider {@code Drawable} to be drawn on the
     *                RecyclerView
     */
    public LinearDividerItemDecoration(Drawable divider) {
        this.mDivider = divider;
    }

    /**
     * Draws horizontal or vertical dividers onto the parent RecyclerView.
     *
     * @param canvas The {@link Canvas} onto which dividers will be drawn
     * @param parent The RecyclerView onto which dividers are being added
     * @param state  The current RecyclerView.State of the RecyclerView
     */
    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        if (mOrientation == LinearLayoutManager.HORIZONTAL) {
            drawHorizontalDividers(canvas, parent);
        } else if (mOrientation == LinearLayoutManager.VERTICAL) {
            drawVerticalDividers(canvas, parent);
        }
    }

    /**
     * Determines the size and location of offsets between items in the parent RecyclerView.
     *
     * @param outRect The {@link Rect} of offsets to be added around the child view
     * @param view    The child view to be decorated with an offset
     * @param parent  The RecyclerView onto which dividers are being added
     * @param state   The current RecyclerView.State of the RecyclerView
     */
    @Override
    public void getItemOffsets(
            @NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if (parent.getChildAdapterPosition(view) == 0) {
            return;
        }

        mOrientation = ((LinearLayoutManager) Objects.requireNonNull(
                parent.getLayoutManager())).getOrientation();
        if (mOrientation == LinearLayoutManager.HORIZONTAL) {
            outRect.left = mDivider.getIntrinsicWidth();
        } else if (mOrientation == LinearLayoutManager.VERTICAL) {
            outRect.top = mDivider.getIntrinsicHeight();
        }
    }

    /**
     * Adds dividers to a RecyclerView with a LinearLayoutManager or its subclass oriented
     * horizontally.
     *
     * @param canvas The {@link Canvas} onto which horizontal dividers will be drawn
     * @param parent The RecyclerView onto which horizontal dividers are being added
     */
    private void drawHorizontalDividers(Canvas canvas, RecyclerView parent) {
        int parentTop =
                parent.getPaddingTop() + (int) parent.getContext().getResources().getDimension(
                        R.dimen.car_ui_recyclerview_divider_top_margin);
        int parentBottom = parent.getHeight() - parent.getPaddingBottom()
                - (int) parent.getContext().getResources().getDimension(
                R.dimen.car_ui_recyclerview_divider_bottom_margin);

        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        int childCount = layoutManager.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = layoutManager.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int parentLeft = child.getRight() + params.rightMargin;
            int parentRight = parentLeft + mDivider.getIntrinsicWidth();

            mDivider.setBounds(parentLeft, parentTop, parentRight, parentBottom);
            mDivider.draw(canvas);
        }
    }

    /**
     * Adds dividers to a RecyclerView with a LinearLayoutManager or its subclass oriented
     * vertically.
     *
     * @param canvas The {@link Canvas} onto which vertical dividers will be drawn
     * @param parent The RecyclerView onto which vertical dividers are being added
     */
    private void drawVerticalDividers(Canvas canvas, RecyclerView parent) {
        int parentLeft =
                parent.getPaddingLeft() + (int) parent.getContext().getResources().getDimension(
                        R.dimen.car_ui_recyclerview_divider_start_margin);
        int parentRight = parent.getWidth() - parent.getPaddingRight()
                - (int) parent.getContext().getResources().getDimension(
                R.dimen.car_ui_recyclerview_divider_end_margin);

        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        int childCount = layoutManager.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = layoutManager.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int parentTop = child.getBottom() + params.bottomMargin;
            int parentBottom = parentTop + mDivider.getIntrinsicHeight();

            mDivider.setBounds(parentLeft, parentTop, parentRight, parentBottom);
            mDivider.draw(canvas);
        }
    }
}
