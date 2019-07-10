package com.bankledger.safecold.ui.fragment;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.bankledger.safecold.R;
import com.bankledger.safecold.adapter.ViewPagerAdapter;
import com.bankledger.safecold.ui.activity.AddContactsAddressActivity;
import com.bankledger.safecold.ui.activity.AddressSearchActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zm on 2018/6/22.
 */

public class AddressBookFragment extends BaseFragment {

    private View ivAdd;

    private ViewPager vpAddressBook;
    private ViewPagerAdapter addressAdapter;
    private TabLayout tabLayout;

    @Override
    public int setContentView() {
        return R.layout.fragment_address_book;
    }

    @Override
    public void initView() {
        findViewById(R.id.tv_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2Activity(AddressSearchActivity.class);
            }
        });

        ivAdd = findViewById(R.id.iv_add);
        ivAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2Activity(AddContactsAddressActivity.class);
            }
        });

        vpAddressBook = findViewById(R.id.vp_address_book);

        addressAdapter = new ViewPagerAdapter(getChildFragmentManager());
        List<String> titleList = new ArrayList<>(2);
        titleList.add(getString(R.string.my_address));
        titleList.add(getString(R.string.contact_address));
        addressAdapter.mTitleList = titleList;
        List<Fragment> fragmentList = new ArrayList<>(2);
        fragmentList.add(new MineAddressFragment());
        fragmentList.add(new ContactsAddressFragment());
        addressAdapter.mFragmentList = fragmentList;
        vpAddressBook.setAdapter(addressAdapter);
        vpAddressBook.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                ivAdd.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabLayout = findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(vpAddressBook);
    }

    @Override
    public void initData() {

    }
}
