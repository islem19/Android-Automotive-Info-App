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

package com.android.car.ui.core;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * Content provider for displaying search results. The url looks like:
 * "content://${applicationId}.SearchResultsProvide/search_results"
 */
public class SearchResultsProvider extends ContentProvider {
    public static final String ITEM_ID = "primaryId";
    public static final String SECONDARY_IMAGE_ID = "secondary";
    public static final String PRIMARY_IMAGE_BLOB = "primary_image";
    public static final String SECONDARY_IMAGE_BLOB = "secondary_image";
    public static final String TITLE = "title";
    public static final String SUBTITLE = "subtitle";
    public static final String CONTENT = "content://";
    public static final String SEARCH_RESULTS_PROVIDER = ".SearchResultsProvider";

    private Uri mContentUri;
    private List<ContentValues> mSearchResults = new ArrayList<>();

    /**
     * Database specific constant declarations
     */
    public static final String SEARCH_RESULTS_TABLE_NAME = "search_results";

    @Override
    public boolean onCreate() {
        String url = CONTENT + getProviderName() + "/" + SEARCH_RESULTS_TABLE_NAME;
        mContentUri = Uri.parse(url);

        return true;
    }

    private String getProviderName() {
        return getContext().getPackageName() + SEARCH_RESULTS_PROVIDER;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        mSearchResults.add(values);

        Uri contentUri = ContentUris.withAppendedId(mContentUri, mSearchResults.size() - 1);
        getContext().getContentResolver().notifyChange(contentUri, null);
        return contentUri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                ITEM_ID,
                SECONDARY_IMAGE_ID,
                PRIMARY_IMAGE_BLOB,
                SECONDARY_IMAGE_BLOB,
                TITLE,
                SUBTITLE
        });

        for (ContentValues values : mSearchResults) {
            cursor.addRow(new Object[]{
                    values.get(ITEM_ID),
                    values.get(SECONDARY_IMAGE_ID),
                    values.get(PRIMARY_IMAGE_BLOB),
                    values.get(SECONDARY_IMAGE_BLOB),
                    values.get(TITLE),
                    values.get(SUBTITLE),
            });
        }
        return cursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        getContext().getContentResolver().notifyChange(uri, null);
        mSearchResults.clear();
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values,
            String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}
