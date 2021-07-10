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

import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

/**
 * Class responsible for fast scrolling. This class offers two functionalities.
 * <ul>
 *     <li>User can hold the thumb and drag.</li>
 *     <li>User can click anywhere on the track and thumb will scroll to that position.</li>
 * </ul>
 */
class FastScroller implements View.OnTouchListener {

    private float mTouchDownY = -1;

    private View mScrollTrackView;
    private View mScrollThumb;
    private RecyclerView mRecyclerView;
    private int mClickActionThreshold;

    FastScroller(@NonNull RecyclerView recyclerView, @NonNull View scrollTrackView,
            @NonNull View scrollView) {
        mRecyclerView = recyclerView;
        mScrollTrackView = scrollTrackView;
        mScrollThumb = requireViewByRefId(scrollView, R.id.car_ui_scrollbar_thumb);
        mClickActionThreshold = ViewConfiguration.get(
                recyclerView.getContext()).getScaledTouchSlop();
    }

    void enable() {
        if (mRecyclerView != null) {
            mScrollTrackView.setOnTouchListener(this);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent me) {
        switch (me.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownY = me.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float thumbBottom = mScrollThumb.getY() + mScrollThumb.getHeight();
                // check if the move coordinates are within the bounds of the thumb. i.e user is
                // holding and dragging the thumb.
                if (!(me.getY() + mScrollTrackView.getY() < thumbBottom
                        && me.getY() + mScrollTrackView.getY() > mScrollThumb.getY())) {
                    // don't do anything if touch is detected outside the thumb
                    return true;
                }
                // calculate where the center of the thumb is on the screen.
                float thumbCenter = mScrollThumb.getY() + mScrollThumb.getHeight() / 2.0f;
                // me.getY() returns the coordinates relative to the view. For example, if we
                // click the top left of the scroll track the coordinates will be 0,0. Hence, we
                // need to add the relative coordinates to the actual coordinates computed by the
                // thumb center and add them to get the final Y coordinate. "(me.getY() -
                // mTouchDownY)" calculates the distance that is moved from the previous touch
                // event.
                verticalScrollTo(thumbCenter + (me.getY() - mTouchDownY));
                mTouchDownY = me.getY();
                break;
            case MotionEvent.ACTION_UP:
            default:
                if (isClick(mTouchDownY, me.getY())) {
                    verticalScrollTo(me.getY() + mScrollTrackView.getY());
                }
                mTouchDownY = -1;
        }
        return true;
    }

    /**
     * Checks if the start and end points are within the threshold to be considered as a click.
     */
    private boolean isClick(float startY, float endY) {
        return Math.abs(startY - endY) < mClickActionThreshold;
    }

    private void verticalScrollTo(float y) {
        int scrollingBy = calculateScrollDistance(y);
        if (scrollingBy != 0) {
            mRecyclerView.scrollBy(0, scrollingBy);
        }
    }

    private int calculateScrollDistance(float newDragPos) {
        final int[] scrollbarRange = getVerticalRange();
        int scrollbarLength = scrollbarRange[1] - scrollbarRange[0];

        float thumbCenter = mScrollThumb.getY() + mScrollThumb.getHeight() / 2.0f;

        if (scrollbarLength == 0) {
            return 0;
        }
        // percentage of data to be scrolled.
        float percentage = ((newDragPos - thumbCenter) / (float) scrollbarLength);
        int totalPossibleOffset =
                mRecyclerView.computeVerticalScrollRange() - mRecyclerView.getHeight();
        return (int) (percentage * totalPossibleOffset);
    }

    /**
     * Gets the (min, max) vertical positions of the vertical scroll bar. The range starts from the
     * center of thumb when thumb is top aligned to center of the thumb when thumb is bottom
     * aligned.
     */
    private int[] getVerticalRange() {
        int[] verticalRange = new int[2];
        verticalRange[0] = (int) mScrollTrackView.getY() + mScrollThumb.getHeight() / 2;
        verticalRange[1] = (int) mScrollTrackView.getY() + mScrollTrackView.getHeight()
                - mScrollThumb.getHeight() / 2;
        return verticalRange;
    }
}
