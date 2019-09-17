package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.adapter.ViewPagerStateAdapter;
import com.bankledger.safecold.ui.fragment.QrCodeFragment;
import com.bankledger.safecold.utils.RingManager;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zm on 2018/7/4.
 */

public class QrCodePageActivity extends ToolbarBaseActivity {
    public static final int START_TYPE_MONITOR = 1;
    public static final int START_TYPE_SEND_CURRENCY = 2;

    private ViewPager mVpView;
    private TextView mTvPage;
    private Button mBtnPrev;
    private Button mBtnNext;

    private List<Fragment> mFgtList = new ArrayList<>();
    private ViewPagerStateAdapter adapter;
    private TextView tvExplain;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_qr_code_page;
    }

    @Override
    public void initView() {
        super.initView();
        mVpView = findViewById(R.id.vp_view);
        mTvPage = findViewById(R.id.tv_page);
        mBtnPrev = findViewById(R.id.btn_prev);
        mBtnNext = findViewById(R.id.btn_next);
    }

    @Override
    public void initData() {
        super.initData();
        setDefaultNavigation();

        tvExplain = findViewById(R.id.tv_explain);

        Intent intent = getIntent();
        int startType = intent.getIntExtra(Constants.INTENT_KEY1, -1);
        if (startType == START_TYPE_MONITOR) {
            setTitle(R.string.export_qr_code);
            tvExplain.setText(R.string.scan_monitor);
        } else if (startType == START_TYPE_SEND_CURRENCY) {
            setTitle(R.string.wallet_transfer);
            tvExplain.setText(R.string.scan_send);
        }

        String content = intent.getStringExtra(Constants.INTENT_KEY2);
        adapter = new ViewPagerStateAdapter(getSupportFragmentManager());
        mVpView.setAdapter(adapter);
        List<String> mList = QRCodeUtil.encodePage(content);
        for (String item : mList) {
            QrCodeFragment fragment = new QrCodeFragment();
            Bundle args = new Bundle();
            args.putString(Constants.INTENT_KEY1, item);
            fragment.setArguments(args);
            mFgtList.add(fragment);
        }
        adapter.setFragment(mFgtList);
        setTvPage(mVpView.getCurrentItem());
        mVpView.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                RingManager.getInstance().playCrystal();
                setTvPage(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mBtnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = mVpView.getCurrentItem();
                mVpView.setCurrentItem(--position);
            }
        });

        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = mVpView.getCurrentItem();
                mVpView.setCurrentItem(++position);
            }
        });
    }

    public void setTvPage(int position) {
        mTvPage.setText(++position + "/" + mFgtList.size());
    }

}
