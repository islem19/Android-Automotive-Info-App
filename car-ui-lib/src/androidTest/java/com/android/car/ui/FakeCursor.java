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

import android.database.AbstractCursor;

import java.util.List;

public class FakeCursor extends AbstractCursor {

    private List<String> mRows;
    private String mColumnName;

    public FakeCursor(List<String> rows, String columnName) {
        mRows = rows;
        mColumnName = columnName;
    }

    @Override
    public int getCount() {
        return mRows.size();
    }

    @Override
    public String[] getColumnNames() {
        return new String[] { mColumnName };
    }

    @Override
    public String getString(int column) {
        return mRows.get(getPosition());
    }

    @Override
    public short getShort(int column) {
        return 0;
    }

    @Override
    public int getInt(int column) {
        return 0;
    }

    @Override
    public long getLong(int column) {
        return 0;
    }

    @Override
    public float getFloat(int column) {
        return 0;
    }

    @Override
    public double getDouble(int column) {
        return 0;
    }

    @Override
    public boolean isNull(int column) {
        return mRows.get(getPosition()) == null;
    }
}
