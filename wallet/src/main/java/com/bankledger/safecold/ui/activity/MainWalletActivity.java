package com.bankledger.safecold.ui.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.bankledger.protobuf.bean.TransColdWalletInfo;
import com.bankledger.protobuf.utils.ProtoUtils;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.adapter.ViewPagerAdapter;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.ui.fragment.AddressBookFragment;
import com.bankledger.safecold.ui.fragment.MineFragment;
import com.bankledger.safecold.ui.fragment.WalletFragment;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.WriterException;

public class MainWalletActivity extends BaseActivity {

    private ViewPager mVpView;
    private BottomNavigationView mBnvView;

    private MenuItem prevMenuItem;
    private ViewPagerAdapter vpAdapter;
    private ScreenBroadcastReceiver mScreenReceiver;

    public static boolean beAppUpgrade = false;//当前是否在app升级页面
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wallet);
        startScreenBroadcastReceiver();
    }

    @Override
    public void initView() {
        mVpView = findViewById(R.id.vp_view);
        mBnvView = findViewById(R.id.bnv_view);
        mBnvView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.bnv_asset:
                                mVpView.setCurrentItem(0);
                                break;
                            case R.id.bnv_address_book:
                                mVpView.setCurrentItem(1);
                                break;
                            case R.id.bnv_mine:
                                mVpView.setCurrentItem(2);
                                break;
                        }
                        return false;
                    }
                });
        vpAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        vpAdapter.addFragment(new WalletFragment());
        vpAdapter.addFragment(new AddressBookFragment());
        vpAdapter.addFragment(new MineFragment());
        mVpView.setOffscreenPageLimit(4);
        mVpView.setAdapter(vpAdapter);
        mVpView.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                } else {
                    mBnvView.getMenu().getItem(0).setChecked(false);
                }
                mBnvView.getMenu().getItem(position).setChecked(true);
                prevMenuItem = mBnvView.getMenu().getItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void initData() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SafeColdApplication.mainWalletActivity = this;
        boolean needGuide = SafeColdApplication.appSharedPreferenceUtil.get(Constants.KEY_SP_START_GUIDE, false);
        if (needGuide) {
            downloadHotWallet();
        }
    }

    private void downloadHotWallet() {
        try {
            Bitmap bitmap = QRCodeEncoderUtils.encodeAsBitmap(this, Constants.HOT_WALLET_DOWNLOAD_URL);
            DialogUtil.showGuide(MainWalletActivity.this, R.string.download_hot_wallet, bitmap, R.string.download_hot_wallet_hint, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showGuide();
                }
            });
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void showGuide() {
        new CommonAsyncTask.Builder<Void, Void, Bitmap>()
                .setIDoInBackground(new IDoInBackground<Void, Void, Bitmap>() {
                    @Override
                    public Bitmap doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        String walletName = WalletInfoManager.getWalletName();
                        String walletNumber = WalletInfoManager.getWalletNumber();
                        String bluetoothAddress = WalletInfoManager.getBluetoothAddress();
                        String content = ProtoUtils.encodeColdWalletInfo(
                                new TransColdWalletInfo(walletNumber, walletName, bluetoothAddress));
                        try {
                            return QRCodeEncoderUtils.encodeAsBitmap(MainWalletActivity.this, content);
                        } catch (WriterException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<Bitmap>() {
                    @Override
                    public void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null) {
                            SafeColdApplication.appSharedPreferenceUtil.put(Constants.KEY_SP_START_GUIDE, false);
                            DialogUtil.showGuide(MainWalletActivity.this, R.string.bind_wallet, bitmap, R.string.guide_hint,
                                    new View.OnClickListener(){
                                        @Override
                                        public void onClick(View v) {
                                            downloadHotWallet();
                                        }
                                    },
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            go2Activity(MonitorAddressActivity.class);
                                        }
                                    });
                        }
                    }
                })
                .start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenReceiver);
    }


    private class ScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                go2ActivityNotAnim(UnlockActivity.class);
                if (!beAppUpgrade && mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.disable();
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
            }
        }
    }

    private void startScreenBroadcastReceiver() {
        mScreenReceiver = new ScreenBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceiver, filter);
    }

}
