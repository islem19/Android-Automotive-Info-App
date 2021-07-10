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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.DialogPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.FocusArea;
import com.android.car.ui.R;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.toolbar.ToolbarController;
import com.android.car.ui.utils.CarUiUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A PreferenceFragmentCompat is the entry point to using the Preference library.
 *
 * <p>Using this fragment will replace regular Preferences with CarUi equivalents. Because of this,
 * certain properties that cannot be read out of Preferences will be lost upon calling
 * {@link #setPreferenceScreen(PreferenceScreen)}. These include the preference viewId,
 * defaultValue, and enabled state.
 */
public abstract class PreferenceFragment extends PreferenceFragmentCompat implements
        InsetsChangedListener {

    private static final String TAG = "CarUiPreferenceFragment";
    private static final String DIALOG_FRAGMENT_TAG =
            "com.android.car.ui.PreferenceFragment.DIALOG";

    /**
     * This method can be overridden to indicate whether or not this fragment covers the
     * whole screen. When it returns false, the preference fragment will not attempt to change
     * the CarUi base layout toolbar (but will still have its own toolbar and change it when using
     * non-baselayout toolbars), and will also not take into account CarUi insets.
     *
     * @return Whether to PreferenceFragment takes up the whole app's space. Defaults to true.
     */
    protected boolean isFullScreenFragment() {
        return true;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (isFullScreenFragment()) {
            ToolbarController baseLayoutToolbar = CarUi.getToolbar(getActivity());
            if (baseLayoutToolbar != null) {
                baseLayoutToolbar.setState(Toolbar.State.SUBPAGE);
                if (getPreferenceScreen() != null) {
                    baseLayoutToolbar.setTitle(getPreferenceScreen().getTitle());
                }
            }
        }

        // TODO(b/150230923) remove the code for the old toolbar height change when apps are ready
        final RecyclerView recyclerView = CarUiUtils.findViewByRefId(view, R.id.recycler_view);
        final Toolbar toolbar = CarUiUtils.findViewByRefId(view, R.id.toolbar);
        if (recyclerView == null || toolbar == null) {
            return;
        }

        recyclerView.setPadding(0, toolbar.getHeight(), 0, 0);
        toolbar.registerToolbarHeightChangeListener(newHeight -> {
            if (recyclerView.getPaddingTop() == newHeight) {
                return;
            }

            int oldHeight = recyclerView.getPaddingTop();
            recyclerView.setPadding(0, newHeight, 0, 0);
            recyclerView.scrollBy(0, oldHeight - newHeight);

            FocusArea focusArea = CarUiUtils.requireViewByRefId(view, R.id.car_ui_focus_area);
            focusArea.setHighlightPadding(0, newHeight, 0, 0);
            focusArea.setBoundsOffset(0, newHeight, 0, 0);
        });

        recyclerView.setClipToPadding(false);
        if (getPreferenceScreen() != null) {
            toolbar.setTitle(getPreferenceScreen().getTitle());
        }
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
    public void onCarUiInsetsChanged(@NonNull Insets insets) {
        if (!isFullScreenFragment()) {
            return;
        }

        View view = requireView();
        FocusArea focusArea = CarUiUtils.requireViewByRefId(view, R.id.car_ui_focus_area);
        focusArea.setHighlightPadding(0, insets.getTop(), 0, insets.getBottom());
        focusArea.setBoundsOffset(0, insets.getTop(), 0, insets.getBottom());
        CarUiUtils.requireViewByRefId(view, R.id.recycler_view)
                .setPadding(0, insets.getTop(), 0, insets.getBottom());
        view.setPadding(insets.getLeft(), 0, insets.getRight(), 0);
    }

    /**
     * Called when a preference in the tree requests to display a dialog. Subclasses should override
     * this method to display custom dialogs or to handle dialogs for custom preference classes.
     *
     * <p>Note: this is borrowed as-is from androidx.preference.PreferenceFragmentCompat with
     * updates to launch Car UI library {@link DialogFragment} instead of the ones in the
     * support library.
     *
     * @param preference The {@link Preference} object requesting the dialog
     */
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {

        if (getActivity() instanceof OnPreferenceDisplayDialogCallback
                && ((OnPreferenceDisplayDialogCallback) getActivity())
                .onPreferenceDisplayDialog(this, preference)) {
            return;
        }

        // check if dialog is already showing
        if (requireFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }

        final Fragment f;
        if (preference instanceof EditTextPreference) {
            f = EditTextPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof ListPreference) {
            f = ListPreferenceFragment.newInstance(preference.getKey(), isFullScreenFragment());
        } else if (preference instanceof MultiSelectListPreference) {
            f = MultiSelectListPreferenceFragment
                    .newInstance(preference.getKey(), isFullScreenFragment());
        } else if (preference instanceof CarUiSeekBarDialogPreference) {
            f = SeekbarPreferenceDialogFragment.newInstance(preference.getKey());
        } else {
            throw new IllegalArgumentException(
                    "Cannot display dialog for an unknown Preference type: "
                            + preference.getClass().getSimpleName()
                            + ". Make sure to implement onPreferenceDisplayDialog() to handle "
                            + "displaying a custom dialog for this Preference.");
        }

        f.setTargetFragment(this, 0);

        if (f instanceof DialogFragment) {
            ((DialogFragment) f).show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            if (getActivity() == null) {
                throw new IllegalStateException(
                        "Preference fragment is not attached to an Activity.");
            }

            if (getView() == null) {
                throw new IllegalStateException(
                        "Preference fragment must have a layout.");
            }

            Context context = getContext();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            CarUiUtils.getAttrResourceId(context,
                                    android.R.attr.fragmentOpenEnterAnimation),
                            CarUiUtils.getAttrResourceId(context,
                                    android.R.attr.fragmentOpenExitAnimation),
                            CarUiUtils.getAttrResourceId(context,
                                    android.R.attr.fragmentCloseEnterAnimation),
                            CarUiUtils.getAttrResourceId(context,
                                    android.R.attr.fragmentCloseExitAnimation))
                    .replace(((ViewGroup) getView().getParent()).getId(), f)
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * This override of setPreferenceScreen replaces preferences with their CarUi versions first.
     */
    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        // We do a search of the tree and every time we see a PreferenceGroup we remove
        // all it's children, replace them with CarUi versions, and then re-add them

        Map<Preference, String> dependencies = new HashMap<>();
        List<Preference> children = new ArrayList<>();

        // Stack of preferences to process
        Deque<Preference> stack = new ArrayDeque<>();
        stack.addFirst(preferenceScreen);

        while (!stack.isEmpty()) {
            Preference preference = stack.removeFirst();

            if (preference instanceof PreferenceGroup) {
                PreferenceGroup pg = (PreferenceGroup) preference;

                children.clear();
                for (int i = 0; i < pg.getPreferenceCount(); i++) {
                    children.add(pg.getPreference(i));
                }

                pg.removeAll();

                for (Preference child : children) {
                    Preference replacement = getReplacementFor(child);

                    dependencies.put(replacement, child.getDependency());
                    pg.addPreference(replacement);
                    stack.addFirst(replacement);
                }
            }
        }

        super.setPreferenceScreen(preferenceScreen);

        // Set the dependencies after all the swapping has been done and they've been
        // associated with this fragment, or we could potentially fail to find preferences
        // or use the wrong preferenceManager
        for (Map.Entry<Preference, String> entry : dependencies.entrySet()) {
            entry.getKey().setDependency(entry.getValue());
        }
    }

    // Mapping from regular preferences to CarUi preferences.
    // Order is important, subclasses must come before their base classes
    private static final List<Pair<Class<? extends Preference>, Class<? extends Preference>>>
            sPreferenceMapping = Arrays.asList(
            new Pair<>(DropDownPreference.class, CarUiDropDownPreference.class),
            new Pair<>(ListPreference.class, CarUiListPreference.class),
            new Pair<>(MultiSelectListPreference.class, CarUiMultiSelectListPreference.class),
            new Pair<>(EditTextPreference.class, CarUiEditTextPreference.class),
            new Pair<>(SwitchPreference.class, CarUiSwitchPreference.class),
            new Pair<>(Preference.class, CarUiPreference.class)
    );

    /**
     * Gets the CarUi version of the passed in preference. If there is no suitable replacement, this
     * method will return it's input.
     *
     * <p>When given a Preference that extends a replaceable preference, we log a warning instead
     * of replacing it so that we don't remove any functionality.
     */
    private static Preference getReplacementFor(Preference preference) {
        Class<? extends Preference> clazz = preference.getClass();

        for (Pair<Class<? extends Preference>, Class<? extends Preference>> replacement
                : sPreferenceMapping) {
            Class<? extends Preference> source = replacement.first;
            Class<? extends Preference> target = replacement.second;
            if (source.isAssignableFrom(clazz)) {
                if (clazz == source) {
                    try {
                        return copyPreference(preference, (Preference) target
                                .getDeclaredConstructor(Context.class)
                                .newInstance(preference.getContext()));
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                } else if (clazz == target || source == Preference.class) {
                    // Don't warn about subclasses of Preference because there are many legitimate
                    // uses for non-carui Preference subclasses, like Preference groups.
                    return preference;
                } else {
                    Log.w(TAG, "Subclass of " + source.getSimpleName() + " was used, "
                            + "preventing us from substituting it with " + target.getSimpleName());
                    return preference;
                }
            }
        }

        return preference;
    }

    /**
     * Copies all the properties of one preference to another.
     *
     * @return the {@code to} parameter
     */
    private static Preference copyPreference(Preference from, Preference to) {
        // viewId and defaultValue don't have getters
        // isEnabled() is not completely symmetrical with setEnabled(), so we can't use it.
        to.setTitle(from.getTitle());
        to.setOnPreferenceClickListener(from.getOnPreferenceClickListener());
        to.setOnPreferenceChangeListener(from.getOnPreferenceChangeListener());
        to.setIcon(from.getIcon());
        to.setFragment(from.getFragment());
        to.setIntent(from.getIntent());
        to.setKey(from.getKey());
        to.setOrder(from.getOrder());
        to.setSelectable(from.isSelectable());
        to.setPersistent(from.isPersistent());
        to.setIconSpaceReserved(from.isIconSpaceReserved());
        to.setWidgetLayoutResource(from.getWidgetLayoutResource());
        to.setPreferenceDataStore(from.getPreferenceDataStore());
        to.setShouldDisableView(from.getShouldDisableView());
        to.setSingleLineTitle(from.isSingleLineTitle());
        to.setVisible(from.isVisible());
        to.setLayoutResource(from.getLayoutResource());
        to.setCopyingEnabled(from.isCopyingEnabled());

        if (from.getSummaryProvider() != null) {
            to.setSummaryProvider(from.getSummaryProvider());
        } else {
            to.setSummary(from.getSummary());
        }

        if (from.peekExtras() != null) {
            to.getExtras().putAll(from.peekExtras());
        }

        if (from instanceof DialogPreference) {
            DialogPreference fromDialog = (DialogPreference) from;
            DialogPreference toDialog = (DialogPreference) to;
            toDialog.setDialogTitle(fromDialog.getDialogTitle());
            toDialog.setDialogIcon(fromDialog.getDialogIcon());
            toDialog.setDialogMessage(fromDialog.getDialogMessage());
            toDialog.setDialogLayoutResource(fromDialog.getDialogLayoutResource());
            toDialog.setNegativeButtonText(fromDialog.getNegativeButtonText());
            toDialog.setPositiveButtonText(fromDialog.getPositiveButtonText());
        }

        // DropDownPreference extends ListPreference and doesn't add any extra api surface,
        // so we don't need a case for it
        if (from instanceof ListPreference) {
            ListPreference fromList = (ListPreference) from;
            ListPreference toList = (ListPreference) to;
            toList.setEntries(fromList.getEntries());
            toList.setEntryValues(fromList.getEntryValues());
            toList.setValue(fromList.getValue());
        } else if (from instanceof EditTextPreference) {
            EditTextPreference fromText = (EditTextPreference) from;
            EditTextPreference toText = (EditTextPreference) to;
            toText.setText(fromText.getText());
        } else if (from instanceof MultiSelectListPreference) {
            MultiSelectListPreference fromMulti = (MultiSelectListPreference) from;
            MultiSelectListPreference toMulti = (MultiSelectListPreference) to;
            toMulti.setEntries(fromMulti.getEntries());
            toMulti.setEntryValues(fromMulti.getEntryValues());
            toMulti.setValues(fromMulti.getValues());
        } else if (from instanceof TwoStatePreference) {
            TwoStatePreference fromTwoState = (TwoStatePreference) from;
            TwoStatePreference toTwoState = (TwoStatePreference) to;
            toTwoState.setSummaryOn(fromTwoState.getSummaryOn());
            toTwoState.setSummaryOff(fromTwoState.getSummaryOff());

            if (from instanceof SwitchPreference) {
                SwitchPreference fromSwitch = (SwitchPreference) from;
                SwitchPreference toSwitch = (SwitchPreference) to;
                toSwitch.setSwitchTextOn(fromSwitch.getSwitchTextOn());
                toSwitch.setSwitchTextOff(fromSwitch.getSwitchTextOff());
            }
        }

        // We don't need to add checks for things that we will never replace,
        // like PreferenceGroup or CheckBoxPreference

        return to;
    }
}
