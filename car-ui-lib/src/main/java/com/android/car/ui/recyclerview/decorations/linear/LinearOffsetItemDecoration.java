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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.util.Objects;

/**
 * Adds an offset to the start of a RecyclerView using a LinearLayoutManager or its subclass.
 *
 * <p>If the RecyclerView.LayoutManager is oriented vertically, the offset will be added to the top
 * of the RecyclerView. If the LayoutManager is oriented horizontally, the offset will be added to
 * the left of the RecyclerView.
 */
public class LinearOffsetItemDecoration extends RecyclerView.ItemDecoration {

    private int mOffsetPx;
    private Drawable mOffsetDrawable;
    private int mOrientation;
    @OffsetPosition
    private int mOffsetPosition;

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
     * Constructor that takes in the size of the offset to be added to the start of the
     * RecyclerView.
     *
     * @param offsetPx       The size of the offset to be added to the start of the RecyclerView in
     *                       pixels
     * @param offsetPosition Position where offset needs to be applied.
     */
    public LinearOffsetItemDecoration(int offsetPx, int offsetPosition) {
        this.mOffsetPx = offsetPx;
        this.mOffsetPosition = offsetPosition;
    }

    /**
     * Constructor that takes in a {@link Drawable} to be drawn at the start of the RecyclerView.
     *
     * @param offsetDrawable The {@code Drawable} to be added to the start of the RecyclerView
     */
    public LinearOffsetItemDecoration(Drawable offsetDrawable) {
        this.mOffsetDrawable = offsetDrawable;
    }

    /**
     * Determines the size and location of the offset to be added to the start of the RecyclerView.
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

        if (mOffsetPosition == OffsetPosition.START && parent.getChildAdapterPosition(view) > 0) {
            return;
        }

        int itemCount = state.getItemCount();
        if (mOffsetPosition == OffsetPosition.END
                && parent.getChildAdapterPosition(view) != itemCount - 1) {
            return;
        }

        mOrientation = ((LinearLayoutManager) Objects.requireNonNull(
                parent.getLayoutManager())).getOrientation();
        if (mOrientation == LinearLayoutManager.HORIZONTAL) {
            if (mOffsetPx > 0) {
                if (mOffsetPosition == OffsetPosition.START) {
                    outRect.left = mOffsetPx;
                } else {
                    outRect.right = mOffsetPx;
                }
            } else if (mOffsetDrawable != null) {
                if (mOffsetPosition == OffsetPosition.START) {
                    outRect.left = mOffsetDrawable.getIntrinsicWidth();
                } else {
                    outRect.right = mOffsetDrawable.getIntrinsicWidth();
                }
            }
        } else if (mOrientation == LinearLayoutManager.VERTICAL) {
            if (mOffsetPx > 0) {
                if (mOffsetPosition == OffsetPosition.START) {
                    outRect.top = mOffsetPx;
                } else {
                    outRect.bottom = mOffsetPx;
                }
            } else if (mOffsetDrawable != null) {
                if (mOffsetPosition == OffsetPosition.START) {
                    outRect.top = mOffsetDrawable.getIntrinsicHeight();
                } else {
                    outRect.bottom = mOffsetDrawable.getIntrinsicHeight();
                }
            }
        }
    }

    /**
     * Draws horizontal or vertical offset onto the start of the parent RecyclerView.
     *
     * @param c      The {@link Canvas} onto which an offset will be drawn
     * @param parent The RecyclerView onto which an offset is being added
     * @param state  The current RecyclerView.State of the RecyclerView
     */
    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        super.onDraw(c, parent, state);
        if (mOffsetDrawable == null) {
            return;
        }

        if (mOrientation == LinearLayoutManager.HORIZONTAL) {
            drawOffsetHorizontal(c, parent);
        } else if (mOrientation == LinearLayoutManager.VERTICAL) {
            drawOffsetVertical(c, parent);
        }
    }

    private void drawOffsetHorizontal(Canvas canvas, RecyclerView parent) {
        int parentTop = parent.getPaddingTop();
        int parentBottom = parent.getHeight() - parent.getPaddingBottom();
        int parentLeft;
        int offsetDrawableRight;

        if (mOffsetPosition == OffsetPosition.START) {
            parentLeft = parent.getPaddingLeft();
        } else {
            RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
            View lastChild = layoutManager.getChildAt(layoutManager.getChildCount() - 1);
            RecyclerView.LayoutParams lastChildLayoutParams =
                    (RecyclerView.LayoutParams) lastChild.getLayoutParams();
            parentLeft = lastChild.getRight() + lastChildLayoutParams.rightMargin;
        }
        offsetDrawableRight = parentLeft + mOffsetDrawable.getIntrinsicWidth();

        mOffsetDrawable.setBounds(parentLeft, parentTop, offsetDrawableRight, parentBottom);
        mOffsetDrawable.draw(canvas);
    }

    private void drawOffsetVertical(Canvas canvas, RecyclerView parent) {
        int parentLeft = parent.getPaddingLeft();
        int parentRight = parent.getWidth() - parent.getPaddingRight();

        int parentTop;
        int offsetDrawableBottom;

        if (mOffsetPosition == OffsetPosition.START) {
            parentTop = parent.getPaddingTop();
        } else {
            RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
            View lastChild = layoutManager.getChildAt(layoutManager.getChildCount() - 1);
            RecyclerView.LayoutParams lastChildLayoutParams =
                    (RecyclerView.LayoutParams) lastChild.getLayoutParams();
            parentTop = lastChild.getBottom() + lastChildLayoutParams.bottomMargin;
        }
        offsetDrawableBottom = parentTop + mOffsetDrawable.getIntrinsicHeight();

        mOffsetDrawable.setBounds(parentLeft, parentTop, parentRight, offsetDrawableBottom);
        mOffsetDrawable.draw(canvas);
    }
}
