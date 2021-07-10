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

package com.android.car.ui.recyclerview;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import com.android.car.ui.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class CarUiListItemTest {

    private CarUiRecyclerView mListView;
    private Context mContext;

    @Mock
    CarUiContentListItem.OnCheckedChangeListener mOnCheckedChangeListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mListView = new CarUiRecyclerView(mContext);
    }

    private CarUiListItemAdapter.ListItemViewHolder getListItemViewHolderAtPosition(int position) {
        return (CarUiListItemAdapter.ListItemViewHolder) mListView.findViewHolderForAdapterPosition(
                position);
    }

    private View getListItemTitleAtPosition(int position) {
        return getListItemViewHolderAtPosition(position).itemView.findViewById(R.id.title);
    }

    private View getListItemBodyAtPosition(int position) {
        return getListItemViewHolderAtPosition(position).itemView.findViewById(R.id.body);
    }

    private View getListItemIconContainerAtPosition(int position) {
        return getListItemViewHolderAtPosition(position).itemView.findViewById(R.id.icon_container);
    }

    private View getListItemActionContainerAtPosition(int position) {
        return getListItemViewHolderAtPosition(position)
                .itemView.findViewById(R.id.action_container);
    }

    private Switch getListItemSwitchAtPosition(int position) {
        return getListItemViewHolderAtPosition(position).itemView.findViewById(R.id.switch_widget);
    }

    private CheckBox getListItemCheckBoxAtPosition(int position) {
        return getListItemViewHolderAtPosition(position)
                .itemView.findViewById(R.id.checkbox_widget);
    }

    private View getListItemIconAtPosition(int position) {
        return getListItemViewHolderAtPosition(position).itemView.findViewById(R.id.icon);
    }

    private CarUiListItemAdapter.HeaderViewHolder getHeaderViewHolderAtPosition(int position) {
        return (CarUiListItemAdapter.HeaderViewHolder) mListView.findViewHolderForAdapterPosition(
                position);
    }

    private TextView getHeaderViewHolderTitleAtPosition(int position) {
        return getHeaderViewHolderAtPosition(position).itemView.findViewById(R.id.title);
    }

    private TextView getHeaderViewHolderBodyAtPosition(int position) {
        return getHeaderViewHolderAtPosition(position).itemView.findViewById(R.id.body);
    }

    private void updateRecyclerViewAdapter(CarUiListItemAdapter adapter) {
        mListView.setAdapter(adapter);

        // Force CarUiRecyclerView and the nested RecyclerView to be laid out.
        mListView.measure(0, 0);
        mListView.layout(0, 0, 100, 10000);

        if (mListView != null) {
            mListView.measure(0, 0);
            mListView.layout(0, 0, 100, 10000);
        }

        // Required to init nested RecyclerView
        mListView.getViewTreeObserver().dispatchOnGlobalLayout();
    }

    @Test
    public void testItemVisibility_withTitle() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setTitle("Test title");
        items.add(item);

        updateRecyclerViewAdapter(new CarUiListItemAdapter(items));

        assertThat(getListItemTitleAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemBodyAtPosition(0).getVisibility()).isNotEqualTo(View.VISIBLE);
        assertThat(getListItemIconContainerAtPosition(0).getVisibility())
                .isNotEqualTo(View.VISIBLE);
        assertThat(getListItemActionContainerAtPosition(0).getVisibility())
                .isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void testItemVisibility_withTitle_withBody() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setTitle("Test title");
        item.setBody("Test body");
        items.add(item);

        updateRecyclerViewAdapter(new CarUiListItemAdapter(items));

        assertThat(getListItemTitleAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemBodyAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemIconContainerAtPosition(0).getVisibility())
                .isNotEqualTo(View.VISIBLE);
        assertThat(getListItemActionContainerAtPosition(0).getVisibility())
                .isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void testItemVisibility_withTitle_withIcon() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.NONE);
        item.setTitle("Test title");
        item.setIcon(mContext.getDrawable(R.drawable.car_ui_icon_close));
        items.add(item);

        updateRecyclerViewAdapter(new CarUiListItemAdapter(items));

        assertThat(getListItemTitleAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemBodyAtPosition(0).getVisibility()).isNotEqualTo(View.VISIBLE);
        assertThat(getListItemIconContainerAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemIconAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemActionContainerAtPosition(0).getVisibility())
                .isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void testItemVisibility_withTitle_withCheckbox() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.CHECK_BOX);
        item.setTitle("Test title");
        items.add(item);

        updateRecyclerViewAdapter(new CarUiListItemAdapter(items));

        assertThat(getListItemTitleAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemBodyAtPosition(0).getVisibility()).isNotEqualTo(View.VISIBLE);
        assertThat(getListItemIconContainerAtPosition(0).getVisibility())
                .isNotEqualTo(View.VISIBLE);
        assertThat(getListItemActionContainerAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemSwitchAtPosition(0).getVisibility()).isNotEqualTo(View.VISIBLE);
        assertThat(getListItemCheckBoxAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemCheckBoxAtPosition(0).isChecked()).isEqualTo(false);
    }

    @Test
    public void testItemVisibility_withTitle_withBody_withSwitch() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.SWITCH);
        item.setTitle("Test title");
        item.setBody("Body text");
        items.add(item);

        updateRecyclerViewAdapter(new CarUiListItemAdapter(items));

        assertThat(getListItemTitleAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemBodyAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemIconContainerAtPosition(0).getVisibility())
                .isNotEqualTo(View.VISIBLE);
        assertThat(getListItemActionContainerAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemSwitchAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getListItemSwitchAtPosition(0).isChecked()).isEqualTo(false);
        assertThat(getListItemCheckBoxAtPosition(0).getVisibility()).isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void testCheckedState_switch() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.SWITCH);
        item.setTitle("Test title");
        item.setOnCheckedChangeListener(mOnCheckedChangeListener);
        item.setChecked(true);
        items.add(item);

        updateRecyclerViewAdapter(new CarUiListItemAdapter(items));

        Switch switchWidget = getListItemSwitchAtPosition(0);

        assertThat(switchWidget.isChecked()).isEqualTo(true);
        switchWidget.performClick();
        assertThat(switchWidget.isChecked()).isEqualTo(false);
        verify(mOnCheckedChangeListener, times(1))
                .onCheckedChanged(item, false);
    }

    @Test
    public void testCheckedState_checkbox() {
        List<CarUiListItem> items = new ArrayList<>();

        CarUiContentListItem item = new CarUiContentListItem(CarUiContentListItem.Action.CHECK_BOX);
        item.setTitle("Test title");
        item.setOnCheckedChangeListener(mOnCheckedChangeListener);
        items.add(item);

        updateRecyclerViewAdapter(new CarUiListItemAdapter(items));

        CheckBox checkBox = getListItemCheckBoxAtPosition(0);

        assertThat(checkBox.isChecked()).isEqualTo(false);
        checkBox.performClick();
        assertThat(checkBox.isChecked()).isEqualTo(true);
        verify(mOnCheckedChangeListener, times(1))
                .onCheckedChanged(item, true);
    }

    @Test
    public void testHeader_onlyTitle() {
        List<CarUiListItem> items = new ArrayList<>();

        CharSequence title = "Test header";
        CarUiHeaderListItem header = new CarUiHeaderListItem(title);
        items.add(header);

        updateRecyclerViewAdapter(new CarUiListItemAdapter(items));

        assertThat(getHeaderViewHolderTitleAtPosition(0).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getHeaderViewHolderTitleAtPosition(0).getText()).isEqualTo(title);
        assertThat(getHeaderViewHolderBodyAtPosition(0).getVisibility()).isNotEqualTo(View.VISIBLE);
    }

    @Test
    public void testHeader_titleAndBody() {
        List<CarUiListItem> items = new ArrayList<>();

        CharSequence title = "Test header";
        CharSequence body = "With body text";

        CarUiHeaderListItem header = new CarUiHeaderListItem(title, body);
        items.add(header);

        updateRecyclerViewAdapter(new CarUiListItemAdapter(items));

        TextView titleView = getHeaderViewHolderTitleAtPosition(0);
        TextView bodyView = getHeaderViewHolderBodyAtPosition(0);

        assertThat(titleView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(titleView.getText()).isEqualTo(title);
        assertThat(bodyView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(bodyView.getText()).isEqualTo(body);
    }
}
