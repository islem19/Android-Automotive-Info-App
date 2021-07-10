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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;

/** Adds an offset to the top of a RecyclerView with a GridLayoutManager or its subclass. */
public class GridOffsetItemDecoration extends RecyclerView.ItemDecoration {

    private int mOffsetPx;
    private Drawable mOffsetDrawable;
    private int mNumColumns;
    @OffsetPosition
    private final int mOffsetPosition;

    /** The possible values for setScrollbarPosition. */
    @IntDef({
            OffsetPosition.START,
            OffsetPosition.END,
    })
    @Retention(SOURCE)
    public @interface OffsetPosition {
        /** Position the offset to the start of the screen. */
        int START = 0;

        /** Position offset to the end of the screen. */
        int END = 1;
    }

    /**
     * Constructor that takes in the size of the offset to be added to the top of the RecyclerView.
     *
     * @param offsetPx       The size of the offset to be added to the top of the RecyclerView in
     *                       pixels
     * @param numColumns     The number of columns in the grid of the RecyclerView
     * @param offsetPosition Position where offset needs to be applied.
     */
    public GridOffsetItemDecoration(int offsetPx, int numColumns, int offsetPosition) {
        this.mOffsetPx = offsetPx;
        this.mNumColumns = numColumns;
        this.mOffsetPosition = offsetPosition;
    }

    /**
     * Constructor that takes in a {@link Drawable} to be drawn at the top of the RecyclerView.
     *
     * @param offsetDrawable The {@code Drawable} to be added to the top of the RecyclerView
     * @param numColumns     The number of columns in the grid of the RecyclerView
     */
    public GridOffsetItemDecoration(Drawable offsetDrawable, int numColumns, int offsetPosition) {
        this.mOffsetDrawable = offsetDrawable;
        this.mNumColumns = numColumns;
        this.mOffsetPosition = offsetPosition;
    }

    public void setNumOfColumns(int numberOfColumns) {
        mNumColumns = numberOfColumns;
    }

    /**
     * Determines the size and the location of the offset to be added to the top of the
     * RecyclerView.
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

        if (mOffsetPosition == OffsetPosition.START) {
            boolean childIsInTopRow = parent.getChildAdapterPosition(view) < mNumColumns;
            if (childIsInTopRow) {
                if (mOffsetPx > 0) {
                    outRect.top = mOffsetPx;
                } else if (mOffsetDrawable != null) {
                    outRect.top = mOffsetDrawable.getIntrinsicHeight();
                }
            }
            return;
        }

        int childCount = state.getItemCount();
        int lastRowChildCount = getLastRowChildCount(childCount);

        boolean childIsInBottomRow =
                parent.getChildAdapterPosition(view) >= childCount - lastRowChildCount;
        if (childIsInBottomRow) {
            if (mOffsetPx > 0) {
                outRect.bottom = mOffsetPx;
            } else if (mOffsetDrawable != null) {
                outRect.bottom = mOffsetDrawable.getIntrinsicHeight();
            }
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        super.onDraw(c, parent, state);
        if (mOffsetDrawable == null) {
            return;
        }

        int parentLeft = parent.getPaddingLeft();
        int parentRight = parent.getWidth() - parent.getPaddingRight();

        if (mOffsetPosition == OffsetPosition.START) {

            int parentTop = parent.getPaddingTop();
            int offsetDrawableBottom = parentTop + mOffsetDrawable.getIntrinsicHeight();

            mOffsetDrawable.setBounds(parentLeft, parentTop, parentRight, offsetDrawableBottom);
            mOffsetDrawable.draw(c);
            return;
        }

        int childCount = state.getItemCount();
        int lastRowChildCount = getLastRowChildCount(childCount);

        int offsetDrawableTop = 0;
        int offsetDrawableBottom = 0;

        for (int i = childCount - lastRowChildCount; i < childCount; i++) {
            View child = parent.getChildAt(i);
            offsetDrawableTop = child.getBottom();
            offsetDrawableBottom = offsetDrawableTop + mOffsetDrawable.getIntrinsicHeight();
        }

        mOffsetDrawable.setBounds(parentLeft, offsetDrawableTop, parentRight, offsetDrawableBottom);
        mOffsetDrawable.draw(c);
    }

    private int getLastRowChildCount(int itemCount) {
        int lastRowChildCount = itemCount % mNumColumns;
        if (lastRowChildCount == 0) {
            lastRowChildCount = mNumColumns;
        }

        return lastRowChildCount;
    }
}
