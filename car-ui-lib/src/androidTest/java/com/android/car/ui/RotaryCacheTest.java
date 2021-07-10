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

package com.android.car.ui;

import static com.android.car.ui.RotaryCache.CACHE_TYPE_EXPIRED_AFTER_SOME_TIME;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link RotaryCache}. */
@RunWith(AndroidJUnit4.class)
public class RotaryCacheTest {
    private static final int CACHE_TIME_OUT_MS = 10000;

    private RotaryCache mRotaryCache;
    private long mValidTime;
    private long mExpiredTime;
    private Context mContext;
    private FocusArea mFocusArea;
    private View mFocusedView;

    @Before
    public void setUp() {
        mRotaryCache = new RotaryCache(CACHE_TYPE_EXPIRED_AFTER_SOME_TIME, CACHE_TIME_OUT_MS,
                CACHE_TYPE_EXPIRED_AFTER_SOME_TIME, CACHE_TIME_OUT_MS);
        mValidTime = CACHE_TIME_OUT_MS - 1;
        mExpiredTime = CACHE_TIME_OUT_MS + 1;
        mContext = ApplicationProvider.getApplicationContext();
        mFocusArea = new FocusArea(mContext);
        mFocusedView = new View(mContext);
    }

    @Test
    public void testGetFocusedView_inTheCache() {
        mRotaryCache.saveFocusedView(mFocusedView, 0);
        View view = mRotaryCache.getFocusedView(mValidTime);
        assertThat(view).isEqualTo(mFocusedView);
    }

    @Test
    public void testGetFocusedView_notInTheCache() {
        View view = mRotaryCache.getFocusedView(mValidTime);
        assertThat(view).isNull();
    }

    @Test
    public void testGetFocusedView_expiredCache() {
        mRotaryCache.saveFocusedView(mFocusedView, 0);
        View view = mRotaryCache.getFocusedView(mExpiredTime);
        assertThat(view).isNull();
    }

    @Test
    public void testGetCachedFocusArea_inTheCache() {
        int direction = View.FOCUS_LEFT;
        mRotaryCache.saveFocusArea(direction, mFocusArea, 0);
        FocusArea focusArea = mRotaryCache.getCachedFocusArea(direction, mValidTime);
        assertThat(focusArea).isEqualTo(mFocusArea);
    }

    @Test
    public void testGetCachedFocusArea_notInTheCache() {
        int direction = View.FOCUS_LEFT;
        mRotaryCache.saveFocusArea(direction, mFocusArea, 0);

        FocusArea focusArea = mRotaryCache.getCachedFocusArea(View.FOCUS_RIGHT, mValidTime);
        assertThat(focusArea).isNull();
        focusArea = mRotaryCache.getCachedFocusArea(View.FOCUS_UP, mValidTime);
        assertThat(focusArea).isNull();
    }

    @Test
    public void testGetCachedFocusArea_expiredCache() {
        int direction = View.FOCUS_LEFT;
        mRotaryCache.saveFocusArea(direction, mFocusArea, 0);
        FocusArea focusArea = mRotaryCache.getCachedFocusArea(direction, mExpiredTime);
        assertThat(focusArea).isNull();
    }

    @Test
    public void testClearFocusAreaHistory() {
        mRotaryCache.saveFocusArea(View.FOCUS_UP, mFocusArea, 0);
        assertThat(mRotaryCache.getCachedFocusArea(View.FOCUS_UP, mValidTime)).isEqualTo(
                mFocusArea);

        mRotaryCache.clearFocusAreaHistory();
        assertThat(mRotaryCache.getCachedFocusArea(View.FOCUS_UP, mValidTime)).isNull();
    }
}
