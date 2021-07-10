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

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import static com.android.car.ui.utils.RotaryConstants.ACTION_HIDE_IME;
import static com.android.car.ui.utils.RotaryConstants.ACTION_RESTORE_DEFAULT_FOCUS;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.utils.ViewUtils;

/**
 * A transparent {@link View} that can take focus. It's used by {@link
 * com.android.car.rotary.RotaryService} to support rotary controller navigation. It's also used to
 * initialize the focus when in rotary mode.
 * <p>
 * To support the rotary controller, each {@link android.view.Window} must have a FocusParkingView
 * as the first focusable view in the view tree, and outside of all {@link FocusArea}s.
 * <p>
 * Android doesn't clear focus automatically when focus is set in another window. If we try to clear
 * focus in the previous window, Android will re-focus a view in that window, resulting in two
 * windows being focused simultaneously. Adding this view to each window can fix this issue. This
 * view is transparent and its default focus highlight is disabled, so it's invisible to the user no
 * matter whether it's focused or not. It can take focus so that RotaryService can "park" the focus
 * on it to remove the focus highlight.
 * <p>
 * If there is only one focus area in the current window, rotating the controller within the focus
 * area will cause RotaryService to move the focus around from the view on the right to the view on
 * the left or vice versa. Adding this view to each window can fix this issue. When RotaryService
 * finds out the focus target is a FocusParkingView, it will know a wrap-around is going to happen.
 * Then it will avoid the wrap-around by not moving focus.
 * <p>
 * To ensure the focus is initialized properly when there is a window change, the FocusParkingView
 * will not get focused when the framework wants to focus on it. Instead, it will try to find a
 * better focus target in the window and focus on the target. That said, the FocusParkingView can
 * still be focused in order to clear focus highlight in the window, such as when RotaryService
 * performs {@link android.view.accessibility.AccessibilityNodeInfo#ACTION_FOCUS} on the
 * FocusParkingView, or the window has lost focus.
 */
public class FocusParkingView extends View {

    /**
     * The focused view in the window containing this FocusParkingView. It's null if no view is
     * focused, or the focused view is a FocusParkingView.
     */
    @Nullable
    private View mFocusedView;

    /** The scrollable container that contains the {@link #mFocusedView}, if any. */
    @Nullable
    ViewGroup mScrollableContainer;

    /**
     * Whether to restore focus when the frameworks wants to focus this view. When false, this view
     * allows itself to be focused instead. This should be false for the {@code FocusParkingView} in
     * an {@code ActivityView}. The default value is true.
     */
    private boolean mShouldRestoreFocus;

    public FocusParkingView(Context context) {
        super(context);
        init(context, /* attrs= */ null);
    }

    public FocusParkingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FocusParkingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public FocusParkingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FocusParkingView);
            mShouldRestoreFocus = a.getBoolean(R.styleable.FocusParkingView_shouldRestoreFocus,
                    /* defValue= */ true);
        }

        // This view is focusable, visible and enabled so it can take focus.
        setFocusable(View.FOCUSABLE);
        setVisibility(VISIBLE);
        setEnabled(true);

        // This view is not clickable so it won't affect the app's behavior when the user clicks on
        // it by accident.
        setClickable(false);

        // This view is always transparent.
        setAlpha(0f);

        // Prevent Android from drawing the default focus highlight for this view when it's focused.
        setDefaultFocusHighlightEnabled(false);

        // Keep track of the focused view so that we can recover focus when it's removed.
        getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
            mFocusedView = newFocus instanceof FocusParkingView ? null : newFocus;
            mScrollableContainer = ViewUtils.getAncestorScrollableContainer(mFocusedView);
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // This size of the view is always 1 x 1 pixel, no matter what value is set in the layout
        // file (match_parent, wrap_content, 100dp, 0dp, etc). Small size is to ensure it has little
        // impact on the layout, non-zero size is to ensure it can take focus.
        setMeasuredDimension(1, 1);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) {
            // We need to clear the focus highlight(by parking the focus on the FocusParkingView)
            // once the current window goes to background. This can't be done by RotaryService
            // because RotaryService sees the window as removed, thus can't perform any action
            // (such as focus, clear focus) on the nodes in the window. So FocusParkingView has to
            // grab the focus proactively.
            super.requestFocus(FOCUS_DOWN, null);

            // OnGlobalFocusChangeListener won't be triggered when the window lost focus, so reset
            // the focused view here.
            mFocusedView = null;
            mScrollableContainer = null;
        } else if (isFocused()) {
            // When FocusParkingView is focused and the window just gets focused, transfer the view
            // focus to a non-FocusParkingView in the window.
            restoreFocusInRoot(/* checkForTouchMode= */ true);
        }
        super.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return FocusParkingView.class.getName();
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        switch (action) {
            case ACTION_RESTORE_DEFAULT_FOCUS:
                return restoreFocusInRoot(/* checkForTouchMode= */ false);
            case ACTION_HIDE_IME:
                InputMethodManager inputMethodManager =
                        getContext().getSystemService(InputMethodManager.class);
                return inputMethodManager.hideSoftInputFromWindow(getWindowToken(),
                        /* flags= */ 0);
            case ACTION_FOCUS:
                // Don't leave this to View to handle as it will exit touch mode.
                if (!hasFocus()) {
                    return super.requestFocus(FOCUS_DOWN, null);
                }
                return false;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        if (!mShouldRestoreFocus) {
            return super.requestFocus(direction, previouslyFocusedRect);
        }
        // Find a better target to focus instead of focusing this FocusParkingView when the
        // framework wants to focus it.
        return restoreFocusInRoot(/* checkForTouchMode= */ true);
    }

    @Override
    public boolean restoreDefaultFocus() {
        if (!mShouldRestoreFocus) {
            return super.restoreDefaultFocus();
        }
        // Find a better target to focus instead of focusing this FocusParkingView when the
        // framework wants to focus it.
        return restoreFocusInRoot(/* checkForTouchMode= */ true);
    }

    /**
     * Sets whether this view should restore focus when the framework wants to focus this view. When
     * set to false, this view allows itself to be focused instead. This should be set to false for
     * the {@code FocusParkingView} in an {@code ActivityView}.  The default value is true.
     */
    public void setShouldRestoreFocus(boolean shouldRestoreFocus) {
        mShouldRestoreFocus = shouldRestoreFocus;
    }

    private boolean restoreFocusInRoot(boolean checkForTouchMode) {
        // Don't do anything in touch mode if checkForTouchMode is true.
        if (checkForTouchMode && isInTouchMode()) {
            return false;
        }
        // The focused view was in a scrollable container and the Framework unfocused it because it
        // was scrolled off the screen. In this case focus on the scrollable container so that the
        // rotary controller can scroll the scrollable container.
        if (maybeFocusOnScrollableContainer()) {
            return true;
        }
        // Otherwise try to find the best target view to focus.
        if (ViewUtils.adjustFocus(getRootView(), /* currentFocus= */ null)) {
            return true;
        }
        // It failed to find a target view (e.g., all the views are not shown), so focus on this
        // FocusParkingView as fallback.
        return super.requestFocus(FOCUS_DOWN, /* previouslyFocusedRect= */ null);
    }

    private boolean maybeFocusOnScrollableContainer() {
        // If the focused view was in a scrollable container and it was scrolled off the screen,
        // focus on the scrollable container. When a view is scrolled off the screen, it is no
        // longer attached to window and its parent is not null. When a view is removed, its parent
        // is null. There is no need to focus on the scrollable container when its focused element
        // is removed.
        if (mFocusedView != null && !mFocusedView.isAttachedToWindow()
                && mFocusedView.getParent() != null && mScrollableContainer != null
                && mScrollableContainer.isAttachedToWindow() && mScrollableContainer.isShown()) {
            RecyclerView recyclerView = mScrollableContainer instanceof RecyclerView
                    ? (RecyclerView) mScrollableContainer
                    : null;
            if (mScrollableContainer.requestFocus()) {
                if (recyclerView != null && recyclerView.isComputingLayout()) {
                    // When a RecyclerView gains focus, it won't dispatch AccessibilityEvent if its
                    // layout is not ready. So wait until its layout is ready then dispatch the
                    // event.
                    getViewTreeObserver().addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    // At this point the layout is complete and the dimensions of
                                    // recyclerView and any child views are known.
                                    recyclerView.sendAccessibilityEvent(TYPE_VIEW_FOCUSED);
                                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
                }
                return true;
            }
        }
        return false;
    }
}
