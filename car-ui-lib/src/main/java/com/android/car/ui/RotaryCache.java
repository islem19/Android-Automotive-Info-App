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

import android.os.SystemClock;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;

/**
 * Cache used by {@link FocusArea} to save focus history and nudge history of the rotary controller.
 */
class RotaryCache {
    /** The cache is disabled. */
    @VisibleForTesting
    static final int CACHE_TYPE_DISABLED = 1;
    /** Entries in the cache will expire after a period of time. */
    @VisibleForTesting
    static final int CACHE_TYPE_EXPIRED_AFTER_SOME_TIME = 2;
    /** Entries in the cache will never expire. */
    @VisibleForTesting
    static final int CACHE_TYPE_NEVER_EXPIRE = 3;

    @IntDef(flag = true, value = {
            CACHE_TYPE_DISABLED, CACHE_TYPE_EXPIRED_AFTER_SOME_TIME, CACHE_TYPE_NEVER_EXPIRE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CacheType {
    }

    /** Cache of focused view. */
    @NonNull
    private final FocusCache mFocusCache;

    /** Cache of FocusAreas that were nudged to. */
    @NonNull
    private final FocusAreaCache mFocusAreaCache;

    /** A record of when a View was focused. */
    private static class FocusHistory {
        /** The focused view. */
        final View mFocusedView;
        /** The {@link SystemClock#uptimeMillis} when this history was recorded. */
        final long mTimestamp;

        FocusHistory(@NonNull View focusedView, long timestamp) {
            mFocusedView = focusedView;
            mTimestamp = timestamp;
        }
    }

    /** Cache of focused view. */
    private static class FocusCache {
        /** The cache type. */
        @CacheType
        final int mCacheType;
        /** How many milliseconds before the entry in the cache expires. */
        long mExpirationPeriodMs;
        /** The record of focused view. */
        @NonNull
        FocusHistory mFocusHistory;

        FocusCache(@CacheType int cacheType, final long expirationPeriodMs) {
            mCacheType = cacheType;
            mExpirationPeriodMs = expirationPeriodMs;
            if (mCacheType == CACHE_TYPE_EXPIRED_AFTER_SOME_TIME && mExpirationPeriodMs <= 0) {
                throw new IllegalArgumentException(
                        "Expiration time must be positive if CacheType is "
                                + "CACHE_TYPE_EXPIRED_AFTER_SOME_TIME");
            }
        }

        View getFocusedView(long elapsedRealtime) {
            return isValidHistory(elapsedRealtime) ? mFocusHistory.mFocusedView : null;
        }

        void setFocusedView(@NonNull View focusedView, long elapsedRealtime) {
            if (mCacheType == CACHE_TYPE_DISABLED) {
                return;
            }
            mFocusHistory = new FocusHistory(focusedView, elapsedRealtime);
        }

        boolean isValidHistory(long elapsedRealtime) {
            if (mFocusHistory == null) {
                return false;
            }
            switch (mCacheType) {
                case CACHE_TYPE_NEVER_EXPIRE:
                    return true;
                case CACHE_TYPE_EXPIRED_AFTER_SOME_TIME:
                    return elapsedRealtime < mFocusHistory.mTimestamp + mExpirationPeriodMs;
                default:
                    return false;
            }
        }
    }

    /** A record of a FocusArea that was nudged to. */
    private static class FocusAreaHistory {
        /** The FocusArea that was nudged to. */
        @NonNull
        final FocusArea mFocusArea;
        /** The {@link SystemClock#uptimeMillis} when this history was recorded. */
        final long mTimestamp;

        FocusAreaHistory(@NonNull FocusArea focusArea, long timestamp) {
            mFocusArea = focusArea;
            mTimestamp = timestamp;
        }
    }

    /** Cache of FocusAreas that were nudged to. */
    private static class FocusAreaCache extends HashMap<Integer, FocusAreaHistory> {
        /** Type of the cache. */
        @CacheType
        private final int mCacheType;
        /** How many milliseconds before an entry in the cache expires. */
        private final int mExpirationPeriodMs;

        FocusAreaCache(@CacheType int cacheType, int expirationPeriodMs) {
            mCacheType = cacheType;
            mExpirationPeriodMs = expirationPeriodMs;
            if (mCacheType == CACHE_TYPE_EXPIRED_AFTER_SOME_TIME && mExpirationPeriodMs <= 0) {
                throw new IllegalArgumentException(
                        "Expiration time must be positive if CacheType is "
                                + "CACHE_TYPE_EXPIRED_AFTER_SOME_TIME");
            }
        }

        void put(int direction, @NonNull FocusArea targetFocusArea, long elapsedRealtime) {
            if (mCacheType == CACHE_TYPE_DISABLED) {
                return;
            }
            put(direction, new FocusAreaHistory(targetFocusArea, elapsedRealtime));
        }

        FocusArea get(int direction, long elapsedRealtime) {
            FocusAreaHistory history = get(direction);
            return isValidHistory(history, elapsedRealtime) ? history.mFocusArea : null;
        }

        boolean isValidHistory(@Nullable FocusAreaHistory history, long elapsedRealtime) {
            if (history == null) {
                return false;
            }
            switch (mCacheType) {
                case CACHE_TYPE_NEVER_EXPIRE:
                    return true;
                case CACHE_TYPE_EXPIRED_AFTER_SOME_TIME:
                    return elapsedRealtime - history.mTimestamp < mExpirationPeriodMs;
                default:
                    return false;
            }
        }
    }

    RotaryCache(@CacheType int focusHistoryCacheType,
            int focusHistoryExpirationPeriodMs,
            @CacheType int focusAreaHistoryCacheType,
            int focusAreaHistoryExpirationPeriodMs) {
        mFocusCache = new FocusCache(focusHistoryCacheType, focusHistoryExpirationPeriodMs);
        mFocusAreaCache = new FocusAreaCache(
                focusAreaHistoryCacheType, focusAreaHistoryExpirationPeriodMs);
    }

    /**
     * Searches the cache to find the last focused view of the FocusArea. Returns the view, or null
     * if there is nothing in the cache, the cache is stale.
     */
    @Nullable
    View getFocusedView(long elapsedRealtime) {
        return mFocusCache.getFocusedView(elapsedRealtime);
    }

    /** Saves the focused view. */
    void saveFocusedView(@NonNull View view, long elapsedRealtime) {
        mFocusCache.setFocusedView(view, elapsedRealtime);
    }

    /**
     * Searches the cache to find the target FocusArea for a nudge in a given {@code direction}.
     * Returns the target FocusArea, or null if there is nothing in the cache, the cache is stale.
     */
    @Nullable
    FocusArea getCachedFocusArea(int direction, long elapsedRealtime) {
        return mFocusAreaCache.get(direction, elapsedRealtime);
    }

    /** Saves the FocusArea nudge history. */
    void saveFocusArea(int direction, @NonNull FocusArea targetFocusArea, long elapsedRealtime) {
        mFocusAreaCache.put(direction, targetFocusArea, elapsedRealtime);
    }

    /** Clears the FocusArea nudge history cache. */
    void clearFocusAreaHistory() {
        mFocusAreaCache.clear();
    }
}
