/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.ui.preference;

import static com.android.car.ui.preference.PreferenceDialogFragment.ARG_KEY;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import com.android.car.ui.FocusArea;
import com.android.car.ui.R;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItem;
import com.android.car.ui.recyclerview.CarUiListItemAdapter;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.toolbar.ToolbarController;
import com.android.car.ui.utils.CarUiUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A fragment that provides a layout with a list of options associated with a {@link
 * CarUiMultiSelectListPreference}.
 */
public class MultiSelectListPreferenceFragment extends Fragment implements InsetsChangedListener {

    private static final String ARG_FULLSCREEN = "fullscreen";

    private CarUiMultiSelectListPreference mPreference;
    private Set<String> mNewValues;
    private ToolbarController mToolbar;
    private boolean mFullScreen;

    /**
     * Returns a new instance of {@link MultiSelectListPreferenceFragment} for the {@link
     * CarUiMultiSelectListPreference} with the given {@code key}.
     */
    @NonNull
    static MultiSelectListPreferenceFragment newInstance(String key, boolean fullScreen) {
        MultiSelectListPreferenceFragment fragment = new MultiSelectListPreferenceFragment();
        Bundle b = new Bundle(/* capacity= */ 1);
        b.putString(ARG_KEY, key);
        b.putBoolean(ARG_FULLSCREEN, fullScreen);
        fragment.setArguments(b);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (CarUi.getToolbar(getActivity()) == null) {
            return inflater.inflate(R.layout.car_ui_list_preference_with_toolbar, container, false);
        } else {
            return inflater.inflate(R.layout.car_ui_list_preference, container, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final CarUiRecyclerView recyclerView = CarUiUtils.requireViewByRefId(view, R.id.list);
        mFullScreen = requireArguments().getBoolean(ARG_FULLSCREEN, true);
        mToolbar = mFullScreen ? CarUi.getToolbar(requireActivity()) : null;

        // TODO(b/150230923) remove the code for the old toolbar height change when apps are ready
        if (mToolbar == null) {
            Toolbar toolbarView = CarUiUtils.findViewByRefId(view, R.id.toolbar);
            mToolbar = toolbarView;

            if (toolbarView != null) {
                recyclerView.setPadding(0, toolbarView.getHeight(), 0, 0);
                toolbarView.registerToolbarHeightChangeListener(newHeight -> {
                    if (recyclerView.getPaddingTop() == newHeight) {
                        return;
                    }

                    int oldHeight = recyclerView.getPaddingTop();
                    recyclerView.setPadding(0, newHeight, 0, 0);
                    recyclerView.scrollBy(0, oldHeight - newHeight);

                    FocusArea focusArea = view.findViewById(R.id.car_ui_focus_area);
                    if (focusArea != null) {
                        focusArea.setHighlightPadding(0, newHeight, 0, 0);
                        focusArea.setBoundsOffset(0, newHeight, 0, 0);
                    }
                });
            }
        }

        mPreference = getPreference();

        recyclerView.setClipToPadding(false);
        if (mToolbar != null) {
            mToolbar.setTitle(mPreference.getTitle());
            mToolbar.setState(Toolbar.State.SUBPAGE);
        }

        mNewValues = new HashSet<>(mPreference.getValues());
        CharSequence[] entries = mPreference.getEntries();
        CharSequence[] entryValues = mPreference.getEntryValues();

        if (entries == null || entryValues == null) {
            throw new IllegalStateException(
                    "MultiSelectListPreference requires an entries array and an entryValues array"
                            + ".");
        }

        if (entries.length != entryValues.length) {
            throw new IllegalStateException(
                    "MultiSelectListPreference entries array length does not match entryValues "
                            + "array length.");
        }

        List<CarUiListItem> listItems = new ArrayList<>();
        boolean[] selectedItems = mPreference.getSelectedItems();

        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i].toString();
            String entryValue = entryValues[i].toString();
            CarUiContentListItem item = new CarUiContentListItem(
                    CarUiContentListItem.Action.CHECK_BOX);
            item.setTitle(entry);
            item.setChecked(selectedItems[i]);
            item.setOnCheckedChangeListener((listItem, isChecked) -> {
                if (isChecked) {
                    mNewValues.add(entryValue);
                } else {
                    mNewValues.remove(entryValue);
                }
            });

            listItems.add(item);
        }

        CarUiListItemAdapter adapter = new CarUiListItemAdapter(listItems);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Insets insets = CarUi.getInsets(getActivity());
        if (insets != null) {
            onCarUiInsetsChanged(insets);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        updatePreference();
    }

    private void updatePreference() {
        if (mPreference.callChangeListener(mNewValues)) {
            mPreference.setValues(mNewValues);
        }
    }

    private CarUiMultiSelectListPreference getPreference() {
        if (getArguments() == null) {
            throw new IllegalStateException("Preference arguments cannot be null");
        }

        String key = getArguments().getString(ARG_KEY);
        DialogPreference.TargetFragment fragment =
                (DialogPreference.TargetFragment) getTargetFragment();

        if (key == null) {
            throw new IllegalStateException(
                    "MultiSelectListPreference key not found in Fragment arguments");
        }

        if (fragment == null) {
            throw new IllegalStateException(
                    "Target fragment must be registered before displaying "
                            + "MultiSelectListPreference screen.");
        }

        Preference preference = fragment.findPreference(key);

        if (!(preference instanceof CarUiMultiSelectListPreference)) {
            throw new IllegalStateException(
                    "Cannot use MultiSelectListPreferenceFragment with a preference that is "
                            + "not of type CarUiMultiSelectListPreference");
        }

        return (CarUiMultiSelectListPreference) preference;
    }

    @Override
    public void onCarUiInsetsChanged(@NonNull Insets insets) {
        if (!mFullScreen) {
            return;
        }
        View view = requireView();
        CarUiUtils.requireViewByRefId(view, R.id.list)
                .setPadding(0, insets.getTop(), 0, insets.getBottom());
        view.setPadding(insets.getLeft(), 0, insets.getRight(), 0);
        FocusArea focusArea = view.findViewById(R.id.car_ui_focus_area);
        if (focusArea != null) {
            focusArea.setHighlightPadding(0, insets.getTop(), 0, insets.getBottom());
            focusArea.setBoundsOffset(0, insets.getTop(), 0, insets.getBottom());
        }
    }
}
