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
package com.android.car.ui.recyclerview;

import static com.android.car.ui.utils.CarUiUtils.findViewByRefId;
import static com.android.car.ui.utils.RotaryConstants.ROTARY_CONTAINER;
import static com.android.car.ui.utils.RotaryConstants.ROTARY_HORIZONTALLY_SCROLLABLE;
import static com.android.car.ui.utils.RotaryConstants.ROTARY_VERTICALLY_SCROLLABLE;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;
import com.android.car.ui.recyclerview.decorations.grid.GridDividerItemDecoration;
import com.android.car.ui.recyclerview.decorations.grid.GridOffsetItemDecoration;
import com.android.car.ui.recyclerview.decorations.linear.LinearDividerItemDecoration;
import com.android.car.ui.recyclerview.decorations.linear.LinearOffsetItemDecoration;
import com.android.car.ui.recyclerview.decorations.linear.LinearOffsetItemDecoration.OffsetPosition;
import com.android.car.ui.utils.CarUiUtils;
import com.android.car.ui.utils.CarUxRestrictionsUtil;

import java.lang.annotation.Retention;
import java.util.Objects;

/**
 * View that extends a {@link RecyclerView} and wraps itself into a {@link LinearLayout} which could
 * potentially include a scrollbar that has page up and down arrows. Interaction with this view is
 * similar to a {@code RecyclerView} as it takes the same adapter and the layout manager.
 */
public final class CarUiRecyclerView extends RecyclerView {

    private static final String TAG = "CarUiRecyclerView";

    private final CarUxRestrictionsUtil.OnUxRestrictionsChangedListener mListener =
            new UxRestrictionChangedListener();

    @NonNull
    private final CarUxRestrictionsUtil mCarUxRestrictionsUtil;
    private boolean mScrollBarEnabled;
    @Nullable
    private String mScrollBarClass;
    private int mScrollBarPaddingTop;
    private int mScrollBarPaddingBottom;

    @Nullable
    private ScrollBar mScrollBar;

    @Nullable
    private GridOffsetItemDecoration mTopOffsetItemDecorationGrid;
    @Nullable
    private GridOffsetItemDecoration mBottomOffsetItemDecorationGrid;
    @Nullable
    private RecyclerView.ItemDecoration mTopOffsetItemDecorationLinear;
    @Nullable
    private RecyclerView.ItemDecoration mBottomOffsetItemDecorationLinear;
    @Nullable
    private GridDividerItemDecoration mDividerItemDecorationGrid;
    @Nullable
    private RecyclerView.ItemDecoration mDividerItemDecorationLinear;
    private int mNumOfColumns;
    private boolean mInstallingExtScrollBar = false;
    private int mContainerVisibility = View.VISIBLE;
    @Nullable
    private Rect mContainerPadding;
    @Nullable
    private Rect mContainerPaddingRelative;
    @Nullable
    private LinearLayout mContainer;

    // Set to true when when styled attributes are read and initialized.
    private boolean mIsInitialized;
    private boolean mEnableDividers;
    private int mTopOffset;
    private int mBottomOffset;

    private boolean mHasScrolled = false;

    private OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            if (dx > 0 || dy > 0) {
                mHasScrolled = true;
                removeOnScrollListener(this);
            }
        }
    };

    /**
     * The possible values for setScrollBarPosition. The default value is actually {@link
     * CarUiRecyclerViewLayout#LINEAR}.
     */
    @IntDef({
            CarUiRecyclerViewLayout.LINEAR,
            CarUiRecyclerViewLayout.GRID,
    })
    @Retention(SOURCE)
    public @interface CarUiRecyclerViewLayout {
        /**
         * Arranges items either horizontally in a single row or vertically in a single column. This
         * is default.
         */
        int LINEAR = 0;

        /**
         * Arranges items in a Grid.
         */
        int GRID = 1;
    }

    /**
     * Interface for a {@link RecyclerView.Adapter} to cap the number of items.
     *
     * <p>NOTE: it is still up to the adapter to use maxItems in {@link
     * RecyclerView.Adapter#getItemCount()}.
     *
     * <p>the recommended way would be with:
     *
     * <pre>{@code
     * {@literal@}Override
     * public int getItemCount() {
     *   return Math.min(super.getItemCount(), mMaxItems);
     * }
     * }</pre>
     */
    public interface ItemCap {

        /**
         * A value to pass to {@link #setMaxItems(int)} that indicates there should be no limit.
         */
        int UNLIMITED = -1;

        /**
         * Sets the maximum number of items available in the adapter. A value less than '0' means
         * the list should not be capped.
         */
        void setMaxItems(int maxItems);
    }

    public CarUiRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public CarUiRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.carUiRecyclerViewStyle);
    }

    public CarUiRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        mCarUxRestrictionsUtil = CarUxRestrictionsUtil.getInstance(context);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setClipToPadding(false);
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.CarUiRecyclerView,
                defStyleAttr,
                R.style.Widget_CarUi_CarUiRecyclerView);
        initRotaryScroll(a);

        mScrollBarEnabled = context.getResources().getBoolean(R.bool.car_ui_scrollbar_enable);

        mScrollBarPaddingTop = context.getResources()
                .getDimensionPixelSize(R.dimen.car_ui_scrollbar_padding_top);
        mScrollBarPaddingBottom = context.getResources()
                .getDimensionPixelSize(R.dimen.car_ui_scrollbar_padding_bottom);

        @CarUiRecyclerViewLayout int carUiRecyclerViewLayout =
                a.getInt(R.styleable.CarUiRecyclerView_layoutStyle, CarUiRecyclerViewLayout.LINEAR);
        mNumOfColumns = a.getInt(R.styleable.CarUiRecyclerView_numOfColumns, /* defValue= */ 2);
        mEnableDividers =
                a.getBoolean(R.styleable.CarUiRecyclerView_enableDivider, /* defValue= */ false);

        mDividerItemDecorationLinear = new LinearDividerItemDecoration(
                context.getDrawable(R.drawable.car_ui_recyclerview_divider));

        mDividerItemDecorationGrid =
                new GridDividerItemDecoration(
                        context.getDrawable(R.drawable.car_ui_divider),
                        context.getDrawable(R.drawable.car_ui_divider),
                        mNumOfColumns);

        mTopOffset = a.getInteger(R.styleable.CarUiRecyclerView_topOffset, /* defValue= */0);
        mBottomOffset = a.getInteger(
                R.styleable.CarUiRecyclerView_bottomOffset, /* defValue= */0);
        mTopOffsetItemDecorationLinear =
                new LinearOffsetItemDecoration(mTopOffset, OffsetPosition.START);
        mBottomOffsetItemDecorationLinear =
                new LinearOffsetItemDecoration(mBottomOffset, OffsetPosition.END);
        mTopOffsetItemDecorationGrid =
                new GridOffsetItemDecoration(mTopOffset, mNumOfColumns,
                        OffsetPosition.START);
        mBottomOffsetItemDecorationGrid =
                new GridOffsetItemDecoration(mBottomOffset, mNumOfColumns,
                        OffsetPosition.END);

        mIsInitialized = true;

        // Check if a layout manager has already been set via XML
        boolean isLayoutMangerSet = getLayoutManager() != null;
        if (!isLayoutMangerSet && carUiRecyclerViewLayout == CarUiRecyclerViewLayout.LINEAR) {
            setLayoutManager(new LinearLayoutManager(getContext()));
        } else if (!isLayoutMangerSet && carUiRecyclerViewLayout == CarUiRecyclerViewLayout.GRID) {
            setLayoutManager(new GridLayoutManager(getContext(), mNumOfColumns));
        }
        addOnScrollListener(mOnScrollListener);

        a.recycle();

        if (!mScrollBarEnabled) {
            return;
        }

        mContainer = new LinearLayout(getContext());

        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);

        mScrollBarClass = context.getResources().getString(R.string.car_ui_scrollbar_component);
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layoutManager) {
        // Cannot setup item decorations before stylized attributes have been read.
        if (mIsInitialized) {
            addItemDecorations(layoutManager);
        }
        super.setLayoutManager(layoutManager);
    }

    // This method should not be invoked before item decorations are initialized by the #init()
    // method.
    private void addItemDecorations(LayoutManager layoutManager) {
        // remove existing Item decorations.
        removeItemDecoration(Objects.requireNonNull(mDividerItemDecorationGrid));
        removeItemDecoration(Objects.requireNonNull(mTopOffsetItemDecorationGrid));
        removeItemDecoration(Objects.requireNonNull(mBottomOffsetItemDecorationGrid));
        removeItemDecoration(Objects.requireNonNull(mDividerItemDecorationLinear));
        removeItemDecoration(Objects.requireNonNull(mTopOffsetItemDecorationLinear));
        removeItemDecoration(Objects.requireNonNull(mBottomOffsetItemDecorationLinear));

        if (layoutManager instanceof GridLayoutManager) {
            if (mEnableDividers) {
                addItemDecoration(Objects.requireNonNull(mDividerItemDecorationGrid));
            }
            addItemDecoration(Objects.requireNonNull(mTopOffsetItemDecorationGrid));
            addItemDecoration(Objects.requireNonNull(mBottomOffsetItemDecorationGrid));
            setNumOfColumns(((GridLayoutManager) layoutManager).getSpanCount());
        } else {
            if (mEnableDividers) {
                addItemDecoration(Objects.requireNonNull(mDividerItemDecorationLinear));
            }
            addItemDecoration(Objects.requireNonNull(mTopOffsetItemDecorationLinear));
            addItemDecoration(Objects.requireNonNull(mBottomOffsetItemDecorationLinear));
        }
    }

    /**
     * If this view's {@code rotaryScrollEnabled} attribute is set to true, sets the content
     * description so that the {@code RotaryService} will treat it as a scrollable container and
     * initializes this view accordingly.
     */
    private void initRotaryScroll(@Nullable TypedArray styledAttributes) {
        boolean rotaryScrollEnabled = styledAttributes != null && styledAttributes.getBoolean(
                R.styleable.CarUiRecyclerView_rotaryScrollEnabled, /* defValue=*/ false);
        if (rotaryScrollEnabled) {
            int orientation = styledAttributes.getInt(R.styleable.RecyclerView_android_orientation,
                    LinearLayout.VERTICAL);
            CarUiUtils.setRotaryScrollEnabled(
                    this, /* isVertical= */ orientation == LinearLayout.VERTICAL);
        } else {
            CharSequence contentDescription = getContentDescription();
            rotaryScrollEnabled = contentDescription != null
                    && (ROTARY_HORIZONTALLY_SCROLLABLE.contentEquals(contentDescription)
                    || ROTARY_VERTICALLY_SCROLLABLE.contentEquals(contentDescription));
        }

        // If rotary scrolling is enabled, set a generic motion event listener to convert
        // SOURCE_ROTARY_ENCODER scroll events into SOURCE_MOUSE scroll events that RecyclerView
        // knows how to handle.
        setOnGenericMotionListener(rotaryScrollEnabled ? (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_SCROLL) {
                if (event.getSource() == InputDevice.SOURCE_ROTARY_ENCODER) {
                    MotionEvent mouseEvent = MotionEvent.obtain(event);
                    mouseEvent.setSource(InputDevice.SOURCE_MOUSE);
                    CarUiRecyclerView.super.onGenericMotionEvent(mouseEvent);
                    return true;
                }
            }
            return false;
        } : null);

        // If rotary scrolling is enabled, mark this view as focusable. This view will be focused
        // when no focusable elements are visible.
        setFocusable(rotaryScrollEnabled);

        // Focus this view before descendants so that the RotaryService can focus this view when it
        // wants to.
        setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);

        // Disable the default focus highlight. No highlight should appear when this view is
        // focused.
        setDefaultFocusHighlightEnabled(false);

        // This view is a rotary container if it's not a scrollable container.
        if (!rotaryScrollEnabled) {
            super.setContentDescription(ROTARY_CONTAINER);
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

        // If we're restoring an existing RecyclerView, consider
        // it as having already scrolled some.
        mHasScrolled = true;
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if (mScrollBar != null) {
            mScrollBar.requestLayout();
        }
    }

    /**
     * Sets the number of columns in which grid needs to be divided.
     */
    public void setNumOfColumns(int numberOfColumns) {
        mNumOfColumns = numberOfColumns;
        if (mTopOffsetItemDecorationGrid != null) {
            mTopOffsetItemDecorationGrid.setNumOfColumns(mNumOfColumns);
        }
        if (mDividerItemDecorationGrid != null) {
            mDividerItemDecorationGrid.setNumOfColumns(mNumOfColumns);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mContainerVisibility = visibility;
        if (mContainer != null) {
            mContainer.setVisibility(visibility);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mCarUxRestrictionsUtil.register(mListener);
        if (mInstallingExtScrollBar || !mScrollBarEnabled) {
            return;
        }
        // When CarUiRV is detached from the current parent and attached to the container with
        // the scrollBar, onAttachedToWindow() will get called immediately when attaching the
        // CarUiRV to the container. This flag will help us keep track of this state and avoid
        // recursion. We also want to reset the state of this flag as soon as the container is
        // successfully attached to the CarUiRV's original parent.
        mInstallingExtScrollBar = true;
        installExternalScrollBar();
        mInstallingExtScrollBar = false;
    }

    /**
     * This method will detach the current recycler view from its parent and attach it to the
     * container which is a LinearLayout. Later the entire container is attached to the parent where
     * the recycler view was set with the same layout params.
     */
    private void installExternalScrollBar() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.car_ui_recycler_view, mContainer, true);
        mContainer.setVisibility(mContainerVisibility);

        if (mContainerPadding != null) {
            mContainer.setPadding(mContainerPadding.left, mContainerPadding.top,
                    mContainerPadding.right, mContainerPadding.bottom);
        } else if (mContainerPaddingRelative != null) {
            mContainer.setPaddingRelative(mContainerPaddingRelative.left,
                    mContainerPaddingRelative.top, mContainerPaddingRelative.right,
                    mContainerPaddingRelative.bottom);
        } else {
            mContainer.setPadding(getPaddingLeft(), /* top= */ 0,
                    getPaddingRight(), /* bottom= */ 0);
            setPadding(/* left= */ 0, getPaddingTop(),
                    /* right= */ 0, getPaddingBottom());
        }

        mContainer.setLayoutParams(getLayoutParams());
        ViewGroup parent = (ViewGroup) getParent();
        int index = parent.indexOfChild(this);
        parent.removeViewInLayout(this);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ((CarUiRecyclerViewContainer) Objects.requireNonNull(
                findViewByRefId(mContainer, R.id.car_ui_recycler_view)))
                .addRecyclerView(this, params);
        parent.addView(mContainer, index);

        createScrollBarFromConfig(findViewByRefId(mContainer, R.id.car_ui_scroll_bar));
    }

    private void createScrollBarFromConfig(View scrollView) {
        Class<?> cls;
        try {
            cls = !TextUtils.isEmpty(mScrollBarClass)
                    ? getContext().getClassLoader().loadClass(mScrollBarClass)
                    : DefaultScrollBar.class;
        } catch (Throwable t) {
            throw andLog("Error loading scroll bar component: " + mScrollBarClass, t);
        }
        try {
            mScrollBar = (ScrollBar) cls.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            throw andLog("Error creating scroll bar component: " + mScrollBarClass, t);
        }

        mScrollBar.initialize(this, scrollView);

        setScrollBarPadding(mScrollBarPaddingTop, mScrollBarPaddingBottom);
    }

    @Override
    public void setAlpha(float value) {
        if (mScrollBarEnabled) {
            mContainer.setAlpha(value);
        } else {
            super.setAlpha(value);
        }
    }

    @Override
    public ViewPropertyAnimator animate() {
        return mScrollBarEnabled ? mContainer.animate() : super.animate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCarUxRestrictionsUtil.unregister(mListener);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        mContainerPaddingRelative = null;
        if (mScrollBarEnabled) {
            super.setPadding(0, top, 0, bottom);
            if (!mHasScrolled) {
                // If we haven't scrolled, and thus are still at the top of the screen,
                // we should stay scrolled to the top after applying padding. Without this
                // scroll, the padding will start scrolled offscreen. We need the padding
                // to be onscreen to shift the content into a good visible range.
                scrollToPosition(0);
            }
            mContainerPadding = new Rect(left, 0, right, 0);
            if (mContainer != null) {
                mContainer.setPadding(left, 0, right, 0);
            }
            setScrollBarPadding(mScrollBarPaddingTop, mScrollBarPaddingBottom);
        } else {
            super.setPadding(left, top, right, bottom);
        }
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        mContainerPadding = null;
        if (mScrollBarEnabled) {
            super.setPaddingRelative(0, top, 0, bottom);
            if (!mHasScrolled) {
                // If we haven't scrolled, and thus are still at the top of the screen,
                // we should stay scrolled to the top after applying padding. Without this
                // scroll, the padding will start scrolled offscreen. We need the padding
                // to be onscreen to shift the content into a good visible range.
                scrollToPosition(0);
            }
            mContainerPaddingRelative = new Rect(start, 0, end, 0);
            if (mContainer != null) {
                mContainer.setPaddingRelative(start, 0, end, 0);
            }
            setScrollBarPadding(mScrollBarPaddingTop, mScrollBarPaddingBottom);
        } else {
            super.setPaddingRelative(start, top, end, bottom);
        }
    }

    /**
     * Sets the scrollbar's padding top and bottom. This padding is applied in addition to the
     * padding of the RecyclerView.
     */
    public void setScrollBarPadding(int paddingTop, int paddingBottom) {
        if (mScrollBarEnabled) {
            mScrollBarPaddingTop = paddingTop;
            mScrollBarPaddingBottom = paddingBottom;

            if (mScrollBar != null) {
                mScrollBar.setPadding(paddingTop + getPaddingTop(),
                        paddingBottom + getPaddingBottom());
            }
        }
    }

    /**
     * Sets divider item decoration for linear layout.
     */
    public void setLinearDividerItemDecoration(boolean enableDividers) {
        if (enableDividers) {
            addItemDecoration(mDividerItemDecorationLinear);
            return;
        }
        removeItemDecoration(mDividerItemDecorationLinear);
    }

    /**
     * Sets divider item decoration for grid layout.
     */
    public void setGridDividerItemDecoration(boolean enableDividers) {
        if (enableDividers) {
            addItemDecoration(mDividerItemDecorationGrid);
            return;
        }
        removeItemDecoration(mDividerItemDecorationGrid);
    }

    @Override
    public void setContentDescription(CharSequence contentDescription) {
        super.setContentDescription(contentDescription);
        initRotaryScroll(/* styledAttributes= */ null);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        if (mScrollBar != null) {
            // Make sure this is called before super so that scrollbar can get a reference to
            // the adapter using RecyclerView#getAdapter()
            mScrollBar.adapterChanged(adapter);
        }
        super.setAdapter(adapter);
    }

    private static RuntimeException andLog(String msg, Throwable t) {
        Log.e(TAG, msg, t);
        throw new RuntimeException(msg, t);
    }

    private class UxRestrictionChangedListener implements
            CarUxRestrictionsUtil.OnUxRestrictionsChangedListener {

        @Override
        public void onRestrictionsChanged(@NonNull CarUxRestrictions carUxRestrictions) {
            Adapter<?> adapter = getAdapter();
            // If the adapter does not implement ItemCap, then the max items on it cannot be
            // updated.
            if (!(adapter instanceof ItemCap)) {
                return;
            }

            int maxItems = ItemCap.UNLIMITED;
            if ((carUxRestrictions.getActiveRestrictions()
                    & CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT)
                    != 0) {
                maxItems = carUxRestrictions.getMaxCumulativeContentItems();
            }

            int originalCount = adapter.getItemCount();
            ((ItemCap) adapter).setMaxItems(maxItems);
            int newCount = adapter.getItemCount();

            if (newCount == originalCount) {
                return;
            }

            if (newCount < originalCount) {
                adapter.notifyItemRangeRemoved(newCount, originalCount - newCount);
            } else {
                adapter.notifyItemRangeInserted(originalCount, newCount - originalCount);
            }
        }
    }
}
