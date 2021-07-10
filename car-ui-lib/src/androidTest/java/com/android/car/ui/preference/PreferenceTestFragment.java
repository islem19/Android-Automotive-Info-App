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

package com.android.car.ui.preference;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;

import com.android.car.ui.test.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test Fragment to load test preferences.
 */
public class PreferenceTestFragment extends PreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setPreferenceDataStore(new TestDataStore());
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.test_preferences, rootKey);
    }

    public void setOnPreferenceChangeListener(
            String key, Preference.OnPreferenceChangeListener listener) {
        findPreference(key).setOnPreferenceChangeListener(listener);
    }

    public void addPreference(Preference preference) {
        getPreferenceManager().getPreferenceScreen().addPreference(preference);
    }

    /**
     * Custom data store to be used for testing as SharedPreferences for instrumentation tests
     * are not initialized as expected.
     */
    public static class TestDataStore extends PreferenceDataStore {

        private Map<String, String> mStringStore = new HashMap<>();
        private Map<String, Boolean> mBooleanStore = new HashMap<>();
        private Map<String, Set<String>> mStringSetStore = new HashMap<>();

        @Override
        public void putString(String key, @Nullable String value) {
            mStringStore.put(key, value);
        }

        @Override
        public void putStringSet(String key, @Nullable Set<String> values) {
            mStringSetStore.put(key, values);
        }

        @Override
        @Nullable
        public String getString(String key, @Nullable String defValue) {
            return mStringStore.getOrDefault(key, defValue);
        }

        @Override
        @Nullable
        public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
            // Workaround for NullPointerException caused by null string set from custom data store.
            if (defValues == null) {
                defValues = new HashSet<>();
            }
            return mStringSetStore.getOrDefault(key, defValues);
        }

        @Override
        public void putBoolean(String key, boolean value) {
            mBooleanStore.put(key, value);
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return mBooleanStore.getOrDefault(key, defValue);
        }
    }
}
