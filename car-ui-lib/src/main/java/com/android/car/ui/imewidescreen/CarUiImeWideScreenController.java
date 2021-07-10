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

package com.android.car.ui.imewidescreen;

import static com.android.car.ui.core.SearchResultsProvider.CONTENT;
import static com.android.car.ui.core.SearchResultsProvider.SEARCH_RESULTS_PROVIDER;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.ExtractEditText;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;
import com.android.car.ui.core.SearchResultsProvider;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItemAdapter;
import com.android.car.ui.utils.CarUiUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to build an IME that support widescreen mode.
 *
 * <p> This class provides helper methods that should be invoked during the lifecycle of an IME.
 * Usage of these methods are listed below.
 * <ul>
 *      <li>create an instance of {@link CarUiImeWideScreenController} in
 *      {@link InputMethodService#onCreate()}</li>
 *      <li>return {@link #onEvaluateFullscreenMode(boolean)} from
 *      {@link InputMethodService#onEvaluateFullscreenMode()}</li>
 *      <li>return the view created by
 *      {@link #createWideScreenImeView(View)}
 *      from {@link InputMethodService#onCreateInputView()}</li>
 *      <li>{@link #onComputeInsets(InputMethodService.Insets) should be called from
 *      {@link InputMethodService#onComputeInsets(InputMethodService.Insets)}</li>
 *      <li>{@link #onAppPrivateCommand(String, Bundle) should be called from {
 *      @link InputMethodService#onAppPrivateCommand(String, Bundle)}}</li>
 *      <li>{@link #setExtractViewShown(boolean)} should be called from
 *      {@link InputMethodService#setExtractViewShown(boolean)}</li>
 *      <li>{@link #onStartInputView(EditorInfo, InputConnection, CharSequence)} should be called
 *      from {@link InputMethodService#onStartInputView(EditorInfo, boolean)}</li>
 *      <li>{@link #onFinishInputView()} should be called from
 *      {@link InputMethodService#onFinishInputView(boolean)}</li>
 * </ul>
 *
 * <p> All the methods in this class are guarded with a check {@link #isWideScreenMode()}. If
 * wide screen mode is disabled all the method would return without doing anything. Also, IME
 * should check for {@link #isWideScreenMode()} in
 * {@link InputMethodService#setExtractViewShown(boolean)} and return the original value instead
 * of false. for more info see {@link #setExtractViewShown(boolean)}
 */
public class CarUiImeWideScreenController {

    private static final String TAG = "ImeWideScreenController";
    private static final String NOT_ASTERISK_OR_CAPTURED_ASTERISK = "[^*]+|(\\*)";

    // Automotive wide screen mode bundle keys.

    // Action name of the action to support wide screen mode templates data.
    public static final String WIDE_SCREEN_ACTION = "automotive_wide_screen";
    // Action name of action that will be used by IMS to notify the application to clear the data
    // in the EditText.
    public static final String WIDE_SCREEN_CLEAR_DATA_ACTION = "automotive_wide_screen_clear_data";
    // Action name used by applications to notify that new search results are available.
    public static final String WIDE_SCREEN_SEARCH_RESULTS = "wide_screen_search_results";
    // Key to provide the resource id for the icon that will be displayed in the input area. If
    // this is not provided applications icon will be used. Value format is int.
    public static final String WIDE_SCREEN_EXTRACTED_TEXT_ICON_RES_ID =
            "extracted_text_icon_res_id";
    // Key to determine if IME should display the content area or not. Content area is referred to
    // the area used by IME to display search results, description title and description
    // provided by the application. By default it will be shown but this value could be ignored
    // if bool/car_ui_ime_wide_screen_allow_app_hide_content_area is set to false. Value format
    // is boolean.
    public static final String REQUEST_RENDER_CONTENT_AREA = "request_render_content_area";
    // Key used to provide the description title to be rendered in the content area. Value format
    // is String.
    public static final String ADD_DESC_TITLE_TO_CONTENT_AREA = "add_desc_title_to_content_area";
    // Key used to provide the description to be rendered in the content area. Value format is
    // String.
    public static final String ADD_DESC_TO_CONTENT_AREA = "add_desc_to_content_area";
    // Key used to provide the error description to be rendered in the input area. Value format
    // is String.
    public static final String ADD_ERROR_DESC_TO_INPUT_AREA = "add_error_desc_to_input_area";

    // wide screen search item keys. Each search item contains a title, sub-title, primary image
    // and an secondary image. Click actions can be performed on item and secondary image.
    // Application will be notified with the Ids of item clicked.

    // Each key below represents a list. Search results will be displayed in the same order as
    // the list provided by the application. For example, to create the search item at index 0
    // controller will get the information from each lists index 0.

    // Key used to provide list of unique id for each item. This same id will be sent back to
    // the application when the item is clicked. Value format is ArrayList<String>
    public static final String SEARCH_RESULT_ITEM_ID_LIST = "search_result_item_id_list";

    public static final String SEARCH_RESULT_SUPPLEMENTAL_ICON_ID_LIST =
            "search_result_supplemental_icon_id_list";
    // key used to provide the surface package information by the application to the IME. IME
    // will send the surface info each time its being displayed.
    public static final String CONTENT_AREA_SURFACE_PACKAGE = "content_area_surface_package";
    // key to provide the host token of surface view by IME to the application.
    public static final String CONTENT_AREA_SURFACE_HOST_TOKEN = "content_area_surface_host_token";
    // key to provide the display id of surface view by IME to the application.
    public static final String CONTENT_AREA_SURFACE_DISPLAY_ID = "content_area_surface_display_id";
    // key to provide the height of surface view by IME to the application.
    public static final String CONTENT_AREA_SURFACE_HEIGHT = "content_area_surface_height";
    // key to provide the width of surface view by IME to the application.
    public static final String CONTENT_AREA_SURFACE_WIDTH = "content_area_surface_width";

    private View mRootView;
    private final Context mContext;
    @Nullable
    private View mExtractActionAutomotive;
    @NonNull
    private View mContentAreaAutomotive;
    // whether to render the content area for automotive when in wide screen mode.
    private boolean mImeRendersAllContent = true;
    private boolean mAllowAppToHideContentArea;
    @Nullable
    private ArrayList<CarUiContentListItem> mAutomotiveSearchItems;
    @NonNull
    private TextView mWideScreenDescriptionTitle;
    @NonNull
    private TextView mWideScreenDescription;
    @NonNull
    private TextView mWideScreenErrorMessage;
    @NonNull
    private ImageView mWideScreenErrorImage;
    @NonNull
    private ImageView mWideScreenClearData;
    @NonNull
    private RecyclerView mRecyclerView;
    @Nullable
    private ImageView mWideScreenExtractedTextIcon;
    private boolean mIsExtractIconProvidedByApp;
    @NonNull
    private FrameLayout mInputFrame;
    @NonNull
    private ExtractEditText mExtractEditText;
    private EditorInfo mInputEditorInfo;
    private InputConnection mInputConnection;
    private boolean mExtractViewHidden;
    @NonNull
    private View mFullscreenArea;
    @NonNull
    private SurfaceView mContentAreaSurfaceView;
    @NonNull
    private FrameLayout mInputExtractEditTextContainer;
    private final InputMethodService mInputMethodService;

    public CarUiImeWideScreenController(@NonNull Context context, @NonNull InputMethodService ims) {
        mContext = context;
        mInputMethodService = ims;
    }

    /**
     * Create and return the view hierarchy used for the input area in wide screen mode. This method
     * will inflate the templates with the inputView provided.
     *
     * @param inputView view of the keyboard created by application.
     * @return view to be used by {@link InputMethodService}.
     */
    public View createWideScreenImeView(@NonNull View inputView) {
        if (!isWideScreenMode()) {
            return inputView;
        }
        mRootView = View.inflate(mContext, R.layout.car_ui_ims_wide_screen_input_view, null);

        mInputFrame = mRootView.requireViewById(R.id.car_ui_wideScreenInputArea);
        mInputFrame.addView(inputView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mAllowAppToHideContentArea =
                mContext.getResources().getBoolean(
                        R.bool.car_ui_ime_wide_screen_allow_app_hide_content_area);

        mContentAreaSurfaceView = mRootView.requireViewById(R.id.car_ui_ime_surface);
        mContentAreaSurfaceView.setZOrderOnTop(true);
        mWideScreenDescriptionTitle =
                mRootView.requireViewById(R.id.car_ui_wideScreenDescriptionTitle);
        mWideScreenDescription = mRootView.requireViewById(R.id.car_ui_wideScreenDescription);
        mExtractActionAutomotive =
                mRootView.findViewById(R.id.car_ui_inputExtractActionAutomotive);
        mContentAreaAutomotive = mRootView.requireViewById(R.id.car_ui_contentAreaAutomotive);
        mRecyclerView = mRootView.requireViewById(R.id.car_ui_wideScreenSearchResultList);
        mWideScreenErrorMessage = mRootView.requireViewById(R.id.car_ui_wideScreenErrorMessage);
        mWideScreenExtractedTextIcon =
                mRootView.findViewById(R.id.car_ui_wideScreenExtractedTextIcon);
        mWideScreenErrorImage = mRootView.requireViewById(R.id.car_ui_wideScreenError);
        mWideScreenClearData = mRootView.requireViewById(R.id.car_ui_wideScreenClearData);
        mFullscreenArea = mRootView.requireViewById(R.id.car_ui_fullscreenArea);
        mInputExtractEditTextContainer = mRootView.requireViewById(
                R.id.car_ui_inputExtractEditTextContainer);
        mWideScreenClearData.setOnClickListener(
                v -> {
                    // notify the app to clear the data.
                    mInputConnection.performPrivateCommand(WIDE_SCREEN_CLEAR_DATA_ACTION, null);
                });
        mExtractViewHidden = false;

        return mRootView;
    }

    /**
     * Compute the interesting insets into your UI. When the content view is shown the default
     * touchable insets are {@link InputMethodService.Insets#TOUCHABLE_INSETS_FRAME}. When content
     * view is hidden then that area of the application is interactable by user.
     *
     * @param outInsets Fill in with the current UI insets.
     */
    public void onComputeInsets(@NonNull InputMethodService.Insets outInsets) {
        if (!isWideScreenMode()) {
            return;
        }
        Rect tempRect = new Rect();
        int[] tempLocation = new int[2];
        outInsets.contentTopInsets = outInsets.visibleTopInsets =
                mInputMethodService.getWindow().getWindow().getDecorView().getHeight();
        if (mImeRendersAllContent) {
            outInsets.touchableRegion.setEmpty();
            outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_FRAME;
        } else {
            mInputFrame.getLocationOnScreen(tempLocation);
            tempRect.set(/* left= */0, /* top= */ 0,
                    tempLocation[0] + mInputFrame.getWidth(),
                    tempLocation[1] + mInputFrame.getHeight());
            outInsets.touchableRegion.set(tempRect);
            outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION;
        }
    }

    /**
     * Actions passed by the application must be "automotive_wide_screen" with the corresponding
     * data that application wants to display. See the comments associated with each bundle key to
     * know what view is rendered.
     *
     * <p> Each bundle key renders or updates/controls a particular view in the template. For
     * example, if application rendered the description title and later also wanted to render an
     * actual description with it then application should use both "add_desc_title_to_content_area"
     * and "add_desc_to_content_area" to provide the data. Sending action with only
     * "add_desc_to_content_area" bundle key will not add an extra view but will display only the
     * description and not the title.
     *
     * When the IME window is closed all the views are reset. For the default view visibility see
     * {@link #resetAutomotiveWideScreenViews()}.
     *
     * @param action Name of the command to be performed.
     * @param data   Any data to include with the command.
     */
    public void onAppPrivateCommand(String action, Bundle data) {
        if (!isWideScreenMode()) {
            return;
        }
        resetAutomotiveWideScreenViews();
        if (data == null) {
            return;
        }
        if (mAllowAppToHideContentArea || (mInputEditorInfo != null && isPackageAuthorized(
                mInputEditorInfo.packageName))) {
            mImeRendersAllContent = data.getBoolean(REQUEST_RENDER_CONTENT_AREA, true);
            if (!mImeRendersAllContent) {
                mContentAreaAutomotive.setVisibility(View.GONE);
            } else {
                mContentAreaAutomotive.setVisibility(View.VISIBLE);
            }
        }

        if (data.getParcelable(CONTENT_AREA_SURFACE_PACKAGE) != null
                && Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            SurfacePackage surfacePackage = (SurfacePackage) data.getParcelable(
                    CONTENT_AREA_SURFACE_PACKAGE);
            mContentAreaSurfaceView.setChildSurfacePackage(surfacePackage);
            mContentAreaSurfaceView.setVisibility(View.VISIBLE);
            mContentAreaAutomotive.setVisibility(View.GONE);
        }

        String discTitle = data.getString(ADD_DESC_TITLE_TO_CONTENT_AREA);
        if (!TextUtils.isEmpty(discTitle)) {
            mWideScreenDescriptionTitle.setText(discTitle);
            mWideScreenDescriptionTitle.setVisibility(View.VISIBLE);
            mContentAreaAutomotive.setBackground(
                    mContext.getDrawable(R.drawable.car_ui_ime_wide_screen_background));
        }

        String disc = data.getString(ADD_DESC_TO_CONTENT_AREA);
        if (!TextUtils.isEmpty(disc)) {
            mWideScreenDescription.setText(disc);
            mWideScreenDescription.setVisibility(View.VISIBLE);
            mContentAreaAutomotive.setBackground(
                    mContext.getDrawable(R.drawable.car_ui_ime_wide_screen_background));
        }

        String errorMessage = data.getString(ADD_ERROR_DESC_TO_INPUT_AREA);
        if (!TextUtils.isEmpty(errorMessage)) {
            mWideScreenErrorMessage.setVisibility(View.VISIBLE);
            mWideScreenClearData.setVisibility(View.GONE);
            mWideScreenErrorImage.setVisibility(View.VISIBLE);
            setExtractedEditTextBackground(
                    R.drawable.car_ui_ime_wide_screen_input_area_tint_error_color);
            mWideScreenErrorMessage.setText(errorMessage);
            mContentAreaAutomotive.setBackground(
                    mContext.getDrawable(R.drawable.car_ui_ime_wide_screen_background));
        }

        if (TextUtils.isEmpty(errorMessage)) {
            mWideScreenErrorMessage.setVisibility(View.INVISIBLE);
            mWideScreenErrorMessage.setText("");
            mWideScreenClearData.setVisibility(View.VISIBLE);
            mWideScreenErrorImage.setVisibility(View.GONE);
            setExtractedEditTextBackground(
                    R.drawable.car_ui_ime_wide_screen_input_area_tint_color);
        }

        int extractedTextIcon = data.getInt(WIDE_SCREEN_EXTRACTED_TEXT_ICON_RES_ID);
        if (extractedTextIcon != 0) {
            setWideScreenExtractedIcon(extractedTextIcon);
        }

        if (WIDE_SCREEN_SEARCH_RESULTS.equals(action)) {
            loadSearchItems();
        }

        if (mExtractActionAutomotive != null) {
            mExtractActionAutomotive.setVisibility(View.VISIBLE);
        }
        if (mAutomotiveSearchItems != null) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            mRecyclerView.setVerticalScrollBarEnabled(true);
            mRecyclerView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
            mRecyclerView.setVisibility(View.VISIBLE);
            mRecyclerView.setAdapter(new CarUiListItemAdapter(mAutomotiveSearchItems));
            mContentAreaAutomotive.setBackground(
                    mContext.getDrawable(R.drawable.car_ui_ime_wide_screen_background));
            if (mExtractActionAutomotive != null) {
                mExtractActionAutomotive.setVisibility(View.GONE);
            }
        }
    }

    private void loadSearchItems() {
        if (mInputEditorInfo == null) {
            Log.w(TAG, "Result can't be loaded, input InputEditorInfo not available ");
            return;
        }
        String url = CONTENT + mInputEditorInfo.packageName + SEARCH_RESULTS_PROVIDER;
        Uri contentUrl = Uri.parse(url);
        ContentResolver cr = mContext.getContentResolver();
        Cursor c = cr.query(contentUrl, null, null, null, null);
        mAutomotiveSearchItems = new ArrayList<>();
        if (c != null && c.moveToFirst()) {
            do {
                CarUiContentListItem searchItem = new CarUiContentListItem(
                        CarUiContentListItem.Action.ICON);
                searchItem.setOnItemClickedListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(SEARCH_RESULT_ITEM_ID_LIST,
                            c.getString(c.getColumnIndex(SearchResultsProvider.ITEM_ID)));
                    mInputConnection.performPrivateCommand(WIDE_SCREEN_ACTION, bundle);
                });
                searchItem.setTitle(c.getString(c.getColumnIndex(SearchResultsProvider.TITLE)));
                searchItem.setBody(c.getString(c.getColumnIndex(SearchResultsProvider.SUBTITLE)));
                searchItem.setPrimaryIconType(CarUiContentListItem.IconType.CONTENT);
                byte[] primaryBlob = c.getBlob(
                        c.getColumnIndex(SearchResultsProvider.PRIMARY_IMAGE_BLOB));
                if (primaryBlob != null) {
                    Bitmap primaryBitmap = Bitmap.CREATOR.createFromParcel(
                            byteArrayToParcel(primaryBlob));
                    searchItem.setIcon(
                            new BitmapDrawable(mContext.getResources(), primaryBitmap));
                }
                byte[] secondaryBlob = c.getBlob(
                        c.getColumnIndex(SearchResultsProvider.SECONDARY_IMAGE_BLOB));

                if (secondaryBlob != null) {
                    Bitmap secondaryBitmap = Bitmap.CREATOR.createFromParcel(
                            byteArrayToParcel(secondaryBlob));
                    searchItem.setSupplementalIcon(
                            new BitmapDrawable(mContext.getResources(), secondaryBitmap), v -> {
                                Bundle bundle = new Bundle();
                                bundle.putString(SEARCH_RESULT_SUPPLEMENTAL_ICON_ID_LIST,
                                        c.getString(c.getColumnIndex(
                                                SearchResultsProvider.SECONDARY_IMAGE_ID)));
                                mInputConnection.performPrivateCommand(WIDE_SCREEN_ACTION, bundle);
                            });
                }
                mAutomotiveSearchItems.add(searchItem);
            } while (c.moveToNext());
        }
        // delete the results.
        cr.delete(contentUrl, null, null);
    }

    private static Parcel byteArrayToParcel(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return parcel;
    }

    /**
     * Evaluate if IME should launch in a fullscreen mode. In wide screen mode IME should always
     * launch in a fullscreen mode so that {@link ExtractEditText} is inflated. Later the controller
     * will detach the {@link ExtractEditText} from its original parent and inflate into the
     * appropriate container in wide screen.
     *
     * @param isFullScreen value evaluated to be in fullscreen mode or not by the app.
     */
    public boolean onEvaluateFullscreenMode(boolean isFullScreen) {
        return isWideScreenMode() || isFullScreen;
    }

    /**
     * Initialize the view in the wide screen template based on the data provided by the app through
     * {@link #onAppPrivateCommand(String, Bundle)}
     */
    public void onStartInputView(@NonNull EditorInfo editorInfo,
            @Nullable InputConnection inputConnection, @Nullable CharSequence textForImeAction) {
        if (!isWideScreenMode()) {
            return;
        }
        mInputEditorInfo = editorInfo;
        mInputConnection = inputConnection;
        View header = mRootView.requireViewById(R.id.car_ui_imeWideScreenInputArea);

        header.setVisibility(View.VISIBLE);
        if (mExtractViewHidden) {
            mFullscreenArea.setVisibility(View.INVISIBLE);
        } else {
            mFullscreenArea.setVisibility(View.VISIBLE);
        }

        // This view is rendered by the framework when IME is in full screen mode. For more info
        // see {@link #onEvaluateFullscreenMode}
        mExtractEditText = mRootView.getRootView().requireViewById(
                android.R.id.inputExtractEditText);

        mExtractEditText.setPadding(
                mContext.getResources().getDimensionPixelSize(
                        R.dimen.car_ui_ime_wide_screen_input_edit_text_padding_left),
                /* top= */0,
                mContext.getResources().getDimensionPixelSize(
                        R.dimen.car_ui_ime_wide_screen_input_edit_text_padding_right),
                /* bottom= */0);
        mExtractEditText.setTextSize(mContext.getResources().getDimensionPixelSize(
                R.dimen.car_ui_ime_wide_screen_input_edit_text_size));
        mExtractEditText.setGravity(Gravity.START | Gravity.CENTER);

        ViewGroup parent = (ViewGroup) mExtractEditText.getParent();
        parent.removeViewInLayout(mExtractEditText);

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mInputExtractEditTextContainer.addView(mExtractEditText, params);

        ImageView close = mRootView.findViewById(R.id.car_ui_closeKeyboard);
        if (close != null) {
            close.setOnClickListener(
                    (v) -> {
                        mInputMethodService.requestHideSelf(0);
                    });
        }

        if (!mIsExtractIconProvidedByApp) {
            setWideScreenExtractedIcon(/* iconResId= */0);
        }

        boolean hasAction = (mInputEditorInfo.imeOptions & EditorInfo.IME_MASK_ACTION)
                != EditorInfo.IME_ACTION_NONE;
        boolean hasInputType = mInputEditorInfo.inputType != InputType.TYPE_NULL;
        boolean hasNoAccessoryAction =
                (mInputEditorInfo.imeOptions & EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION) == 0;

        boolean hasLabel =
                mInputEditorInfo.actionLabel != null || (hasAction && hasNoAccessoryAction
                        && hasInputType);

        if (hasLabel) {
            intiExtractAction(textForImeAction);
        }

        sendSurfaceInfo();
    }

    /**
     * Sends the information for surface view to the application on which they can draw on. This
     * information will ONLY be sent if OEM allows an application to hide the content area and let
     * it draw its own content.
     */
    private void sendSurfaceInfo() {
        if (!mAllowAppToHideContentArea && mContentAreaSurfaceView.getDisplay() == null
                && !(mInputEditorInfo != null
                && isPackageAuthorized(mInputEditorInfo.packageName))) {
            return;
        }
        int displayId = mContentAreaSurfaceView.getDisplay().getDisplayId();
        IBinder hostToken = mContentAreaSurfaceView.getHostToken();

        Bundle bundle = new Bundle();
        bundle.putBinder(CONTENT_AREA_SURFACE_HOST_TOKEN, hostToken);
        bundle.putInt(CONTENT_AREA_SURFACE_DISPLAY_ID, displayId);
        bundle.putInt(CONTENT_AREA_SURFACE_HEIGHT,
                mContentAreaSurfaceView.getHeight() + getNavBarHeight());
        bundle.putInt(CONTENT_AREA_SURFACE_WIDTH, mContentAreaSurfaceView.getWidth());

        mInputConnection.performPrivateCommand(WIDE_SCREEN_ACTION, bundle);
    }

    private boolean isPackageAuthorized(String packageName) {
        String[] packages = mContext.getResources()
                .getStringArray(R.array.car_ui_ime_wide_screen_allowed_package_list);

        PackageInfo packageInfo = getPackageInfo(mContext, packageName);
        // Checks if the application of the given context is installed in the system image. I.e.
        // if it's a bundled app.
        if (packageInfo != null && (packageInfo.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM
                | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
            return true;
        }

        for (String pattern : packages) {
            String regex = createRegexFromGlob(pattern);
            if (packageName.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the package info for a particular package.
     */
    @Nullable
    private static PackageInfo getPackageInfo(Context context,
            String packageName) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(
                    packageName, /* flags= */ 0);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "package not found: " + packageName);
        }
        return packageInfo;
    }

    private static String createRegexFromGlob(String glob) {
        Pattern reg = Pattern.compile(NOT_ASTERISK_OR_CAPTURED_ASTERISK);
        Matcher m = reg.matcher(glob);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            if (m.group(1) != null) {
                m.appendReplacement(b, ".*");
            } else {
                m.appendReplacement(b, Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(b);
        return b.toString();
    }

    private int getNavBarHeight() {
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * To support wide screen mode, IME should always call
     * {@link InputMethodService#setExtractViewShown} with false and pass the flag to this method.
     *
     * For example, within the IMS service call
     * <pre>
     *   @Override
     *   public void setExtractViewShown(boolean shown) {
     *     if (!carUiImeWideScreenController.isWideScreenMode()) {
     *         super.setExtractViewShown(shown);
     *         return;
     *     }
     *     super.setExtractViewShown(false);
     *     mImeWideScreenController.setExtractViewShown(shown);
     *   }
     * </pre>
     *
     * This is required as IMS checks for ExtractViewIsShown and if that is true then set the
     * touchable insets to the entire screen rather than a region. If an app hides the content area
     * in that case we want the user to be able to interact with the application.
     */
    public void setExtractViewShown(boolean shown) {
        if (!isWideScreenMode()) {
            return;
        }
        if (mExtractViewHidden == !shown) {
            return;
        }
        mExtractViewHidden = !shown;
        if (mExtractViewHidden) {
            mFullscreenArea.setVisibility(View.INVISIBLE);
        } else {
            mFullscreenArea.setVisibility(View.VISIBLE);
        }
    }

    private void intiExtractAction(CharSequence textForImeAction) {
        if (mExtractActionAutomotive == null) {
            return;
        }
        if (mInputEditorInfo.actionLabel != null) {
            ((TextView) mExtractActionAutomotive).setText(mInputEditorInfo.actionLabel);
        } else {
            ((TextView) mExtractActionAutomotive).setText(textForImeAction);
        }

        // click listener for the action button shown in the content area.
        mExtractActionAutomotive.setOnClickListener(v -> {
            final EditorInfo editorInfo = mInputEditorInfo;
            final InputConnection inputConnection = mInputConnection;
            if (editorInfo == null || inputConnection == null) {
                return;
            }
            if (editorInfo.actionId != 0) {
                inputConnection.performEditorAction(editorInfo.actionId);
            } else if ((editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION)
                    != EditorInfo.IME_ACTION_NONE) {
                inputConnection.performEditorAction(
                        editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION);
            }
        });
    }

    private void setExtractedEditTextBackground(int drawableResId) {
        mExtractEditText.setBackgroundTintList(mContext.getColorStateList(drawableResId));
    }

    @VisibleForTesting
    void setExtractEditText(ExtractEditText editText) {
        mExtractEditText = editText;
    }

    /**
     * Sets the icon in the input area. If the icon resource Id is not provided by the application
     * then application icon will be used instead.
     *
     * @param iconResId icon resource id for the image drawable to load.
     */
    private void setWideScreenExtractedIcon(@DrawableRes int iconResId) {
        if (mInputEditorInfo == null || mWideScreenExtractedTextIcon == null) {
            return;
        }
        try {
            if (iconResId == 0) {
                mWideScreenExtractedTextIcon.setImageDrawable(
                        mContext.getPackageManager().getApplicationIcon(
                                mInputEditorInfo.packageName));
            } else {
                mIsExtractIconProvidedByApp = true;
                mWideScreenExtractedTextIcon.setImageDrawable(
                        mContext.createPackageContext(mInputEditorInfo.packageName, 0).getDrawable(
                                iconResId));
            }
            mWideScreenExtractedTextIcon.setVisibility(View.VISIBLE);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(TAG, "setWideScreenExtractedIcon: package name not found ", ex);
            mWideScreenExtractedTextIcon.setVisibility(View.GONE);
        } catch (Resources.NotFoundException ex) {
            Log.w(TAG, "setWideScreenExtractedIcon: resource not found with id " + iconResId, ex);
            mWideScreenExtractedTextIcon.setVisibility(View.GONE);
        }
    }

    /**
     * Called when IME window closes. Reset all the views once that happens.
     */
    public void onFinishInputView() {
        if (!isWideScreenMode()) {
            return;
        }
        resetAutomotiveWideScreenViews();
    }

    private void resetAutomotiveWideScreenViews() {
        mWideScreenDescriptionTitle.setVisibility(View.GONE);
        mContentAreaSurfaceView.setVisibility(View.GONE);
        mWideScreenErrorMessage.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.GONE);
        mWideScreenDescription.setVisibility(View.GONE);
        mFullscreenArea.setVisibility(View.VISIBLE);
        if (mWideScreenExtractedTextIcon != null) {
            mWideScreenExtractedTextIcon.setVisibility(View.VISIBLE);
        }
        mWideScreenClearData.setVisibility(View.VISIBLE);
        mWideScreenErrorImage.setVisibility(View.GONE);
        if (mExtractActionAutomotive != null) {
            mExtractActionAutomotive.setVisibility(View.GONE);
        }
        mContentAreaAutomotive.setVisibility(View.VISIBLE);
        mContentAreaAutomotive.setBackground(
                mContext.getDrawable(R.drawable.car_ui_ime_wide_screen_no_content_background));
        setExtractedEditTextBackground(R.drawable.car_ui_ime_wide_screen_input_area_tint_color);
        mImeRendersAllContent = true;
        mIsExtractIconProvidedByApp = false;
        mExtractViewHidden = false;
        mAutomotiveSearchItems = null;
    }

    /**
     * Returns whether or not system is running in a wide screen mode.
     */
    public boolean isWideScreenMode() {
        return CarUiUtils.getBooleanSystemProperty(mContext.getResources(),
                R.string.car_ui_ime_wide_screen_system_property_name, false);
    }

    private Drawable loadDrawableFromPackage(int resId) {
        try {
            if (mInputEditorInfo != null) {
                return mContext.createPackageContext(mInputEditorInfo.packageName, 0)
                        .getDrawable(resId);
            }
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "loadDrawableFromPackage: package name not found: ", ex);
        } catch (Resources.NotFoundException ex) {
            Log.w(TAG, "loadDrawableFromPackage: resource not found with id " + resId, ex);
        }
        return null;
    }
}
