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
package com.android.car.ui.recyclerview.decorations.grid;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

import java.util.Objects;

/** Adds interior dividers to a RecyclerView with a GridLayoutManager. */
public class GridDividerItemDecoration extends RecyclerView.ItemDecoration {

    private final Drawable mHorizontalDivider;
    private final Drawable mVerticalDivider;
    private int mNumColumns;

    /**
     * Sole constructor. Takes in {@link Drawable} objects to be used as horizontal and vertical
     * dividers.
     *
     * @param horizontalDivider A divider {@code Drawable} to be drawn on the rows of the grid of
     *                          the
     *                          RecyclerView
     * @param verticalDivider   A divider {@code Drawable} to be drawn on the columns of the grid of
     *                          the
     *                          RecyclerView
     * @param numColumns        The number of columns in the grid of the RecyclerView
     */
    public GridDividerItemDecoration(
            Drawable horizontalDivider, Drawable verticalDivider, int numColumns) {
        this.mHorizontalDivider = horizontalDivider;
        this.mVerticalDivider = verticalDivider;
        this.mNumColumns = numColumns;
    }

    /**
     * Draws horizontal and/or vertical dividers onto the parent RecyclerView.
     *
     * @param canvas The {@link Canvas} onto which dividers will be drawn
     * @param parent The RecyclerView onto which dividers are being added
     * @param state  The current RecyclerView.State of the RecyclerView
     */
    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        drawVerticalDividers(canvas, parent);
        drawHorizontalDividers(canvas, parent);
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
            Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        outRect.set(
                0, 0, mHorizontalDivider.getIntrinsicWidth(),
                mHorizontalDivider.getIntrinsicHeight());
    }

    /**
     * Adds horizontal dividers to a RecyclerView with a GridLayoutManager or its subclass.
     *
     * @param canvas The {@link Canvas} onto which dividers will be drawn
     * @param parent The RecyclerView onto which dividers are being added
     */
    private void drawHorizontalDividers(Canvas canvas, RecyclerView parent) {
        RecyclerView.LayoutManager layoutManager = Objects.requireNonNull(
                parent.getLayoutManager());
        int childCount = layoutManager.getChildCount();
        int rowCount = childCount / mNumColumns;
        int lastRowChildCount = childCount % mNumColumns;
        int lastColumn = Math.min(childCount, mNumColumns);

        for (int i = 1; i < lastColumn; i++) {
            int lastRowChildIndex;
            if (i < lastRowChildCount) {
                lastRowChildIndex = i + (rowCount * mNumColumns);
            } else {
                lastRowChildIndex = i + ((rowCount - 1) * mNumColumns);
            }


            View firstRowChild = layoutManager.getChildAt(i);
            View lastRowChild = layoutManager.getChildAt(lastRowChildIndex);

            int dividerTop =
                    firstRowChild.getTop() + (int) parent.getContext().getResources().getDimension(
                            R.dimen.car_ui_recyclerview_divider_top_margin);
            int dividerRight = firstRowChild.getLeft();
            int dividerLeft = dividerRight - mHorizontalDivider.getIntrinsicWidth();
            int dividerBottom = lastRowChild.getBottom()
                    - (int) parent.getContext().getResources().getDimension(
                    R.dimen.car_ui_recyclerview_divider_bottom_margin);

            mHorizontalDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
            mHorizontalDivider.draw(canvas);
        }
    }

    public void setNumOfColumns(int numberOfColumns) {
        mNumColumns = numberOfColumns;
    }

    /**
     * Adds vertical dividers to a RecyclerView with a GridLayoutManager or its subclass.
     *
     * @param canvas The {@link Canvas} onto which dividers will be drawn
     * @param parent The RecyclerView onto which dividers are being added
     */
    private void drawVerticalDividers(Canvas canvas, RecyclerView parent) {
        RecyclerView.LayoutManager layoutManager = Objects.requireNonNull(
                parent.getLayoutManager());
        double childCount = layoutManager.getChildCount();
        double rowCount = Math.ceil(childCount / mNumColumns);
        int rightmostChildIndex;
        for (int i = 1; i <= rowCount; i++) {
            // we don't want the divider on top of first row.
            if (i == 1) {
                continue;
            }
            if (i == rowCount) {
                rightmostChildIndex = ((i - 1) * mNumColumns) - 1;
            } else {
                rightmostChildIndex = (i * mNumColumns) - 1;
            }

            View leftmostChild = layoutManager.getChildAt(mNumColumns * (i - 1));
            View rightmostChild = layoutManager.getChildAt(rightmostChildIndex);

            // draws on top of each row.
            int dividerLeft =
                    leftmostChild.getLeft() + (int) parent.getContext().getResources().getDimension(
                            R.dimen.car_ui_recyclerview_divider_start_margin);
            int dividerBottom = leftmostChild.getTop();
            int dividerTop = dividerBottom - mVerticalDivider.getIntrinsicHeight();
            int dividerRight = rightmostChild.getRight()
                    - (int) parent.getContext().getResources().getDimension(
                    R.dimen.car_ui_recyclerview_divider_end_margin);

            mVerticalDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
            mVerticalDivider.draw(canvas);
        }
    }
}
