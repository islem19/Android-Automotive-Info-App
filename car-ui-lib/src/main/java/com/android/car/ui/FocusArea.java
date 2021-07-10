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

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import static com.android.car.ui.utils.RotaryConstants.ACTION_NUDGE_SHORTCUT;
import static com.android.car.ui.utils.RotaryConstants.ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_BOTTOM_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_LEFT_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_RIGHT_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_TOP_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.NUDGE_DIRECTION;
import static com.android.car.ui.utils.ViewUtils.NO_FOCUS;
import static com.android.car.ui.utils.ViewUtils.REGULAR_FOCUS;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.car.ui.utils.CarUiUtils;
import com.android.car.ui.utils.ViewUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link LinearLayout} used as a navigation block for the rotary controller.
 * <p>
 * The {@link com.android.car.rotary.RotaryService} looks for instances of {@link FocusArea} in the
 * view hierarchy when handling rotate and nudge actions. When receiving a rotation event ({@link
 * android.car.input.RotaryEvent}), RotaryService will move the focus to another {@link View} that
 * can take focus within the same FocusArea. When receiving a nudge event ({@link
 * KeyEvent#KEYCODE_SYSTEM_NAVIGATION_UP}, {@link KeyEvent#KEYCODE_SYSTEM_NAVIGATION_DOWN}, {@link
 * KeyEvent#KEYCODE_SYSTEM_NAVIGATION_LEFT}, or {@link KeyEvent#KEYCODE_SYSTEM_NAVIGATION_RIGHT}),
 * RotaryService will move the focus to another view that can take focus in another (typically
 * adjacent) FocusArea.
 * <p>
 * If enabled, FocusArea can draw highlights when one of its descendants has focus and it's not in
 * touch mode.
 * <p>
 * When creating a navigation block in the layout file, if you intend to use a LinearLayout as a
 * container for that block, just use a FocusArea instead; otherwise wrap the block in a FocusArea.
 * <p>
 * DO NOT nest a FocusArea inside another FocusArea because it will result in undefined navigation
 * behavior.
 */
public class FocusArea extends LinearLayout {

    private static final String TAG = "FocusArea";

    private static final int INVALID_DIMEN = -1;

    private static final int INVALID_DIRECTION = -1;

    private static final List<Integer> NUDGE_DIRECTIONS =
            Arrays.asList(FOCUS_LEFT, FOCUS_RIGHT, FOCUS_UP, FOCUS_DOWN);

    /** Whether the FocusArea's descendant has focus (the FocusArea itself is not focusable). */
    private boolean mHasFocus;

    /**
     * Whether to draw {@link #mForegroundHighlight} when one of the FocusArea's descendants has
     * focus and it's not in touch mode.
     */
    private boolean mEnableForegroundHighlight;

    /**
     * Whether to draw {@link #mBackgroundHighlight} when one of the FocusArea's descendants has
     * focus and it's not in touch mode.
     */
    private boolean mEnableBackgroundHighlight;

    /**
     * Highlight (typically outline of the FocusArea) drawn on top of the FocusArea and its
     * descendants.
     */
    private Drawable mForegroundHighlight;

    /**
     * Highlight (typically a solid or gradient shape) drawn on top of the FocusArea but behind its
     * descendants.
     */
    private Drawable mBackgroundHighlight;

    /** The padding (in pixels) of the FocusArea highlight. */
    private int mPaddingLeft;
    private int mPaddingRight;
    private int mPaddingTop;
    private int mPaddingBottom;

    /** The offset (in pixels) of the FocusArea's bounds. */
    private int mLeftOffset;
    private int mRightOffset;
    private int mTopOffset;
    private int mBottomOffset;

    /** Whether the layout direction is {@link View#LAYOUT_DIRECTION_RTL}. */
    private boolean mRtl;

    /** The ID of the view specified in {@code app:defaultFocus}. */
    private int mDefaultFocusId;
    /** The view specified in {@code app:defaultFocus}. */
    @Nullable
    private View mDefaultFocusView;

    /**
     * Whether to focus on the {@code app:defaultFocus} view when nudging to the FocusArea, even if
     * there was another view in the FocusArea focused before.
     */
    private boolean mDefaultFocusOverridesHistory;

    /** The ID of the view specified in {@code app:nudgeShortcut}. */
    private int mNudgeShortcutId;
    /** The view specified in {@code app:nudgeShortcut}. */
    @Nullable
    private View mNudgeShortcutView;

    /** The direction specified in {@code app:nudgeShortcutDirection}. */
    private int mNudgeShortcutDirection;

    /**
     * Map of nudge target FocusArea IDs specified in {@code app:nudgeLeft}, {@code app:nudgRight},
     * {@code app:nudgeUp}, or {@code app:nudgeDown}.
     */
    private Map<Integer, Integer> mSpecifiedNudgeIdMap;

    /** Map of specified nudge target FocusAreas. */
    private Map<Integer, FocusArea> mSpecifiedNudgeFocusAreaMap;

    /**
     * Cache of focus history and nudge history of the rotary controller.
     * <p>
     * For focus history, the previously focused view and a timestamp will be saved when the
     * focused view has changed.
     * <p>
     * For nudge history, the target FocusArea, direction, and a timestamp will be saved when the
     * focus has moved from another FocusArea to this FocusArea. There are 2 cases:
     * <ul>
     *     <li>The focus is moved to another FocusArea because this FocusArea has called {@link
     *         #nudgeToAnotherFocusArea}. In this case, the target FocusArea and direction are
     *         trivial to this FocusArea.
     *     <li>The focus is moved to this FocusArea because RotaryService has performed {@link
     *         AccessibilityNodeInfo#ACTION_FOCUS} on this FocusArea. In this case, this FocusArea
     *         can get the source FocusArea through the {@link
     *         android.view.ViewTreeObserver.OnGlobalFocusChangeListener} registered, and can get
     *         the direction when handling the action. Since the listener is triggered before
     *         {@link #requestFocus} returns (which is called when handling the action), the
     *         source FocusArea is revealed earlier than the direction, so the nudge history should
     *         be saved when the direction is revealed.
     * </ul>
     */
    private RotaryCache mRotaryCache;

    /** Whether to clear focus area history when the user rotates the rotary controller. */
    private boolean mClearFocusAreaHistoryWhenRotating;

    /** The FocusArea that had focus before this FocusArea, if any. */
    private FocusArea mPreviousFocusArea;

    /** The focused view in this FocusArea, if any. */
    private View mFocusedView;

    public FocusArea(Context context) {
        super(context);
        init(context, null);
    }

    public FocusArea(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FocusArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public FocusArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        Resources resources = getContext().getResources();
        mEnableForegroundHighlight = resources.getBoolean(
                R.bool.car_ui_enable_focus_area_foreground_highlight);
        mEnableBackgroundHighlight = resources.getBoolean(
                R.bool.car_ui_enable_focus_area_background_highlight);
        mForegroundHighlight = resources.getDrawable(
                R.drawable.car_ui_focus_area_foreground_highlight, getContext().getTheme());
        mBackgroundHighlight = resources.getDrawable(
                R.drawable.car_ui_focus_area_background_highlight, getContext().getTheme());

        mDefaultFocusOverridesHistory = resources.getBoolean(
                R.bool.car_ui_focus_area_default_focus_overrides_history);
        mClearFocusAreaHistoryWhenRotating = resources.getBoolean(
                R.bool.car_ui_clear_focus_area_history_when_rotating);

        @RotaryCache.CacheType
        int focusHistoryCacheType = resources.getInteger(R.integer.car_ui_focus_history_cache_type);
        int focusHistoryExpirationPeriodMs =
                resources.getInteger(R.integer.car_ui_focus_history_expiration_period_ms);
        @RotaryCache.CacheType
        int focusAreaHistoryCacheType = resources.getInteger(
                R.integer.car_ui_focus_area_history_cache_type);
        int focusAreaHistoryExpirationPeriodMs =
                resources.getInteger(R.integer.car_ui_focus_area_history_expiration_period_ms);
        mRotaryCache = new RotaryCache(focusHistoryCacheType, focusHistoryExpirationPeriodMs,
                focusAreaHistoryCacheType, focusAreaHistoryExpirationPeriodMs);

        // Ensure that an AccessibilityNodeInfo is created for this view.
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        // By default all ViewGroup subclasses do not call their draw() and onDraw() methods. We
        // should enable it since we override these methods.
        setWillNotDraw(false);

        registerFocusChangeListener();

        initAttrs(context, attrs);
    }

    private void registerFocusChangeListener() {
        getViewTreeObserver().addOnGlobalFocusChangeListener(
                (oldFocus, newFocus) -> {
                    boolean hasFocus = hasFocus();
                    saveFocusHistory(hasFocus);
                    maybeUpdatePreviousFocusArea(hasFocus, oldFocus);
                    maybeClearFocusAreaHistory(hasFocus, oldFocus);
                    maybeUpdateFocusAreaHighlight(hasFocus);
                    mHasFocus = hasFocus;
                });
    }

    private void saveFocusHistory(boolean hasFocus) {
        if (!hasFocus) {
            mRotaryCache.saveFocusedView(mFocusedView, SystemClock.uptimeMillis());
            mFocusedView = null;
            return;
        }
        View v = getFocusedChild();
        while (v != null) {
            if (v.isFocused()) {
                break;
            }
            v = v instanceof ViewGroup ? ((ViewGroup) v).getFocusedChild() : null;
        }
        mFocusedView = v;
    }

    /**
     * Updates {@link #mPreviousFocusArea} when the focus has moved from another FocusArea to this
     * FocusArea, and sets it to {@code null} in any other cases.
     */
    private void maybeUpdatePreviousFocusArea(boolean hasFocus, View oldFocus) {
        if (mHasFocus || !hasFocus || oldFocus == null || oldFocus instanceof FocusParkingView) {
            mPreviousFocusArea = null;
            return;
        }
        mPreviousFocusArea = ViewUtils.getAncestorFocusArea(oldFocus);
        if (mPreviousFocusArea == null) {
            Log.w(TAG, "No parent FocusArea for " + oldFocus);
        }
    }

    /**
     * Clears FocusArea nudge history when the user rotates the controller to move focus within this
     * FocusArea.
     */
    private void maybeClearFocusAreaHistory(boolean hasFocus, View oldFocus) {
        if (!mClearFocusAreaHistoryWhenRotating) {
            return;
        }
        if (!hasFocus || oldFocus == null) {
            return;
        }
        FocusArea oldFocusArea = ViewUtils.getAncestorFocusArea(oldFocus);
        if (oldFocusArea != this) {
            return;
        }
        mRotaryCache.clearFocusAreaHistory();
    }

    /** Updates highlight of the FocusArea if this FocusArea has gained or lost focus. */
    private void maybeUpdateFocusAreaHighlight(boolean hasFocus) {
        if (!mEnableBackgroundHighlight && !mEnableForegroundHighlight) {
            return;
        }
        if (mHasFocus != hasFocus) {
            invalidate();
        }
    }

    private void initAttrs(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FocusArea);
        try {
            mDefaultFocusId = a.getResourceId(R.styleable.FocusArea_defaultFocus, View.NO_ID);

            // Initialize the highlight padding. The padding, for example, left padding, is set in
            // the following order:
            // 1. if highlightPaddingStart (or highlightPaddingEnd in RTL layout) specified, use it
            // 2. otherwise, if highlightPaddingHorizontal is specified, use it
            // 3. otherwise use 0

            int paddingStart = a.getDimensionPixelSize(
                    R.styleable.FocusArea_highlightPaddingStart, INVALID_DIMEN);
            if (paddingStart == INVALID_DIMEN) {
                paddingStart = a.getDimensionPixelSize(
                        R.styleable.FocusArea_highlightPaddingHorizontal, 0);
            }

            int paddingEnd = a.getDimensionPixelSize(
                    R.styleable.FocusArea_highlightPaddingEnd, INVALID_DIMEN);
            if (paddingEnd == INVALID_DIMEN) {
                paddingEnd = a.getDimensionPixelSize(
                        R.styleable.FocusArea_highlightPaddingHorizontal, 0);
            }

            mRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
            mPaddingLeft = mRtl ? paddingEnd : paddingStart;
            mPaddingRight = mRtl ? paddingStart : paddingEnd;

            mPaddingTop = a.getDimensionPixelSize(
                    R.styleable.FocusArea_highlightPaddingTop, INVALID_DIMEN);
            if (mPaddingTop == INVALID_DIMEN) {
                mPaddingTop = a.getDimensionPixelSize(
                        R.styleable.FocusArea_highlightPaddingVertical, 0);
            }

            mPaddingBottom = a.getDimensionPixelSize(
                    R.styleable.FocusArea_highlightPaddingBottom, INVALID_DIMEN);
            if (mPaddingBottom == INVALID_DIMEN) {
                mPaddingBottom = a.getDimensionPixelSize(
                        R.styleable.FocusArea_highlightPaddingVertical, 0);
            }

            // Initialize the offset of the FocusArea's bounds. The offset, for example, left
            // offset, is set in the following order:
            // 1. if startBoundOffset (or endBoundOffset in RTL layout) specified, use it
            // 2. otherwise, if horizontalBoundOffset is specified, use it
            // 3. otherwise use mPaddingLeft

            int startOffset = a.getDimensionPixelSize(
                    R.styleable.FocusArea_startBoundOffset, INVALID_DIMEN);
            if (startOffset == INVALID_DIMEN) {
                startOffset = a.getDimensionPixelSize(
                        R.styleable.FocusArea_horizontalBoundOffset, paddingStart);
            }

            int endOffset = a.getDimensionPixelSize(
                    R.styleable.FocusArea_endBoundOffset, INVALID_DIMEN);
            if (endOffset == INVALID_DIMEN) {
                endOffset = a.getDimensionPixelSize(
                        R.styleable.FocusArea_horizontalBoundOffset, paddingEnd);
            }

            mLeftOffset = mRtl ? endOffset : startOffset;
            mRightOffset = mRtl ? startOffset : endOffset;

            mTopOffset = a.getDimensionPixelSize(
                    R.styleable.FocusArea_topBoundOffset, INVALID_DIMEN);
            if (mTopOffset == INVALID_DIMEN) {
                mTopOffset = a.getDimensionPixelSize(
                        R.styleable.FocusArea_verticalBoundOffset, mPaddingTop);
            }

            mBottomOffset = a.getDimensionPixelSize(
                    R.styleable.FocusArea_bottomBoundOffset, INVALID_DIMEN);
            if (mBottomOffset == INVALID_DIMEN) {
                mBottomOffset = a.getDimensionPixelSize(
                        R.styleable.FocusArea_verticalBoundOffset, mPaddingBottom);
            }

            mNudgeShortcutId = a.getResourceId(R.styleable.FocusArea_nudgeShortcut, View.NO_ID);
            mNudgeShortcutDirection = a.getInt(
                    R.styleable.FocusArea_nudgeShortcutDirection, INVALID_DIRECTION);
            if ((mNudgeShortcutId == View.NO_ID) ^ (mNudgeShortcutDirection == INVALID_DIRECTION)) {
                throw new IllegalStateException("nudgeShortcut and nudgeShortcutDirection must "
                        + "be specified together");
            }

            mSpecifiedNudgeIdMap = new HashMap<>();
            mSpecifiedNudgeIdMap.put(FOCUS_LEFT,
                    a.getResourceId(R.styleable.FocusArea_nudgeLeft, View.NO_ID));
            mSpecifiedNudgeIdMap.put(FOCUS_RIGHT,
                    a.getResourceId(R.styleable.FocusArea_nudgeRight, View.NO_ID));
            mSpecifiedNudgeIdMap.put(FOCUS_UP,
                    a.getResourceId(R.styleable.FocusArea_nudgeUp, View.NO_ID));
            mSpecifiedNudgeIdMap.put(FOCUS_DOWN,
                    a.getResourceId(R.styleable.FocusArea_nudgeDown, View.NO_ID));
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mDefaultFocusId != View.NO_ID) {
            mDefaultFocusView = CarUiUtils.requireViewByRefId(this, mDefaultFocusId);
        }
        if (mNudgeShortcutId != View.NO_ID) {
            mNudgeShortcutView = CarUiUtils.requireViewByRefId(this, mNudgeShortcutId);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        if (mRtl != rtl) {
            mRtl = rtl;

            int temp = mPaddingLeft;
            mPaddingLeft = mPaddingRight;
            mPaddingRight = temp;

            temp = mLeftOffset;
            mLeftOffset = mRightOffset;
            mRightOffset = temp;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // To ensure the focus is initialized properly in rotary mode when there is a window focus
        // change, this FocusArea will grab the focus from the currently focused view if one of this
        // FocusArea's descendants is a better focus candidate than the currently focused view.
        if (hasWindowFocus && !isInTouchMode()) {
            maybeAdjustFocus();
        }
        super.onWindowFocusChanged(hasWindowFocus);
    }

    /**
     * Focuses on another view in this FocusArea if the view is a better focus candidate than the
     * currently focused view.
     */
    private boolean maybeAdjustFocus() {
        View root = getRootView();
        View focus = root.findFocus();
        return ViewUtils.adjustFocus(root, focus);
    }


    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        switch (action) {
            case ACTION_FOCUS:
                // Repurpose ACTION_FOCUS to focus on its descendant. We can do this because
                // FocusArea is not focusable and it didn't consume ACTION_FOCUS previously.
                boolean success = focusOnDescendant();
                if (success && mPreviousFocusArea != null) {
                    int direction = getNudgeDirection(arguments);
                    if (direction != INVALID_DIRECTION) {
                        saveFocusAreaHistory(direction, mPreviousFocusArea, this,
                                SystemClock.uptimeMillis());
                    }
                }
                return success;
            case ACTION_NUDGE_SHORTCUT:
                return nudgeToShortcutView(arguments);
            case ACTION_NUDGE_TO_ANOTHER_FOCUS_AREA:
                return nudgeToAnotherFocusArea(arguments);
            default:
                return super.performAccessibilityAction(action, arguments);
        }
    }

    private boolean focusOnDescendant() {
        if (mDefaultFocusOverridesHistory) {
            // Check mDefaultFocus before last focused view.
            if (focusDefaultFocusView() || focusOnLastFocusedView()) {
                return true;
            }
        } else {
            // Check last focused view before mDefaultFocus.
            if (focusOnLastFocusedView() || focusDefaultFocusView()) {
                return true;
            }
        }
        return focusOnFirstFocusableView();
    }

    private boolean focusDefaultFocusView() {
        return ViewUtils.adjustFocus(this, /* currentLevel= */ REGULAR_FOCUS);
    }

    /**
     * Gets the {@code app:defaultFocus} view.
     *
     * @hidden
     */
    public View getDefaultFocusView() {
        return mDefaultFocusView;
    }

    private boolean focusOnLastFocusedView() {
        View lastFocusedView = mRotaryCache.getFocusedView(SystemClock.uptimeMillis());
        return ViewUtils.requestFocus(lastFocusedView);
    }

    private boolean focusOnFirstFocusableView() {
        return ViewUtils.adjustFocus(this, /* currentLevel= */ NO_FOCUS);
    }

    private boolean nudgeToShortcutView(Bundle arguments) {
        if (mNudgeShortcutDirection == INVALID_DIRECTION) {
            // No nudge shortcut configured for this FocusArea.
            return false;
        }
        if (arguments == null
                || arguments.getInt(NUDGE_DIRECTION, INVALID_DIRECTION)
                    != mNudgeShortcutDirection) {
            // The user is not nudging in the nudge shortcut direction.
            return false;
        }
        if (mNudgeShortcutView.isFocused()) {
            // The nudge shortcut view is already focused; return false so that the user can
            // nudge to another FocusArea.
            return false;
        }
        return ViewUtils.requestFocus(mNudgeShortcutView);
    }

    private boolean nudgeToAnotherFocusArea(Bundle arguments) {
        int direction = getNudgeDirection(arguments);
        long elapsedRealtime = SystemClock.uptimeMillis();

        // Try to nudge to specified FocusArea, if any.
        FocusArea targetFocusArea = getSpecifiedFocusArea(direction);
        boolean success = targetFocusArea != null && targetFocusArea.focusOnDescendant();

        // If failed, try to nudge to cached FocusArea, if any.
        if (!success) {
            targetFocusArea = mRotaryCache.getCachedFocusArea(direction, elapsedRealtime);
            success = targetFocusArea != null && targetFocusArea.focusOnDescendant();
        }

        return success;
    }

    private static int getNudgeDirection(Bundle arguments) {
        return arguments == null
                ? INVALID_DIRECTION
                : arguments.getInt(NUDGE_DIRECTION, INVALID_DIRECTION);
    }

    private void saveFocusAreaHistory(int direction, @NonNull FocusArea sourceFocusArea,
            @NonNull FocusArea targetFocusArea, long elapsedRealtime) {
        // Save one-way rather than two-way nudge history to avoid infinite nudge loop.
        if (sourceFocusArea.mRotaryCache.getCachedFocusArea(direction, elapsedRealtime) == null) {
            // Save reversed nudge history so that the users can nudge back to where they were.
            int oppositeDirection = getOppositeDirection(direction);
            targetFocusArea.mRotaryCache.saveFocusArea(oppositeDirection, sourceFocusArea,
                    elapsedRealtime);
        }
    }

    /** Returns the direction opposite the given {@code direction} */
    @VisibleForTesting
    private static int getOppositeDirection(int direction) {
        switch (direction) {
            case View.FOCUS_LEFT:
                return View.FOCUS_RIGHT;
            case View.FOCUS_RIGHT:
                return View.FOCUS_LEFT;
            case View.FOCUS_UP:
                return View.FOCUS_DOWN;
            case View.FOCUS_DOWN:
                return View.FOCUS_UP;
        }
        throw new IllegalArgumentException("direction must be "
                + "FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, or FOCUS_RIGHT.");
    }

    @Nullable
    private FocusArea getSpecifiedFocusArea(int direction) {
        maybeInitializeSpecifiedFocusAreas();
        return mSpecifiedNudgeFocusAreaMap.get(direction);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw highlight on top of this FocusArea (including its background and content) but
        // behind its children.
        if (mEnableBackgroundHighlight && mHasFocus && !isInTouchMode()) {
            mBackgroundHighlight.setBounds(
                    mPaddingLeft + getScrollX(),
                    mPaddingTop + getScrollY(),
                    getScrollX() + getWidth() - mPaddingRight,
                    getScrollY() + getHeight() - mPaddingBottom);
            mBackgroundHighlight.draw(canvas);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Draw highlight on top of this FocusArea (including its background and content) and its
        // children (including background, content, focus highlight, etc).
        if (mEnableForegroundHighlight && mHasFocus && !isInTouchMode()) {
            mForegroundHighlight.setBounds(
                    mPaddingLeft + getScrollX(),
                    mPaddingTop + getScrollY(),
                    getScrollX() + getWidth() - mPaddingRight,
                    getScrollY() + getHeight() - mPaddingBottom);
            mForegroundHighlight.draw(canvas);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return FocusArea.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        Bundle bundle = info.getExtras();
        bundle.putInt(FOCUS_AREA_LEFT_BOUND_OFFSET, mLeftOffset);
        bundle.putInt(FOCUS_AREA_RIGHT_BOUND_OFFSET, mRightOffset);
        bundle.putInt(FOCUS_AREA_TOP_BOUND_OFFSET, mTopOffset);
        bundle.putInt(FOCUS_AREA_BOTTOM_BOUND_OFFSET, mBottomOffset);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (isInTouchMode()) {
            return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
        }
        return maybeAdjustFocus();
    }

    @Override
    public boolean restoreDefaultFocus() {
        return maybeAdjustFocus();
    }

    private void maybeInitializeSpecifiedFocusAreas() {
        if (mSpecifiedNudgeFocusAreaMap != null) {
            return;
        }
        View root = getRootView();
        mSpecifiedNudgeFocusAreaMap = new HashMap<>();
        for (Integer direction : NUDGE_DIRECTIONS) {
            int id = mSpecifiedNudgeIdMap.get(direction);
            mSpecifiedNudgeFocusAreaMap.put(direction, root.findViewById(id));
        }
    }

    /**
     * Sets the padding (in pixels) of the FocusArea highlight.
     * <p>
     * It doesn't affect other values, such as the paddings on its child views.
     */
    public void setHighlightPadding(int left, int top, int right, int bottom) {
        if (mPaddingLeft == left && mPaddingTop == top && mPaddingRight == right
                && mPaddingBottom == bottom) {
            return;
        }
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
        invalidate();
    }

    /**
     * Sets the offset (in pixels) of the FocusArea's bounds.
     * <p>
     * It only affects the perceived bounds for the purposes of finding the nudge target. It doesn't
     * affect the FocusArea's view bounds or highlight bounds. The offset should only be used when
     * FocusAreas are overlapping and nudge interaction is ambiguous.
     */
    public void setBoundsOffset(int left, int top, int right, int bottom) {
        mLeftOffset = left;
        mTopOffset = top;
        mRightOffset = right;
        mBottomOffset = bottom;
    }

    /** Sets the default focus view in this FocusArea. */
    public void setDefaultFocus(@NonNull View defaultFocus) {
        mDefaultFocusView = defaultFocus;
    }

    @VisibleForTesting
    void enableForegroundHighlight() {
        mEnableForegroundHighlight = true;
    }

    @VisibleForTesting
    void setDefaultFocusOverridesHistory(boolean override) {
        mDefaultFocusOverridesHistory = override;
    }

    @VisibleForTesting
    void setRotaryCache(@NonNull RotaryCache rotaryCache) {
        mRotaryCache = rotaryCache;
    }

    @VisibleForTesting
    void setClearFocusAreaHistoryWhenRotating(boolean clear) {
        mClearFocusAreaHistoryWhenRotating = clear;
    }
}
