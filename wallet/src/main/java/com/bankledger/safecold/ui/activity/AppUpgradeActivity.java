package com.bankledger.safecold.ui.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bankledger.FileEncrypt;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.bluetooth.BluetoothSPP;
import com.bankledger.safecold.bluetooth.BluetoothState;
import com.bankledger.safecold.ui.widget.CommonDialog;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.FileUtil;
import com.bankledger.safecold.utils.MD5Utils;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.RingManager;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.utils.Base64;
import com.bankledger.utils.IoUtils;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AppUpgradeActivity extends ToolbarBaseActivity {

    private LinearLayout llBluetooth;
    private LinearLayout llQrcode;

    private ImageView imgBlue;
    private TextView tvState;
    private TextView tvPlan;
    private ProgressBar pbPlan;

    private ImageView bluetoothMacAddr;
    private ProgressBar bluetoothLoading;
    private TextView bluetoothHint;

    private AnimationDrawable bluAnim;
    private BluetoothSPP bluetooth;
    private String md5;
    private int cursor = 0;
    private long fileSize;
    private String mBeginFlag = "HEADER:";
    private String mEndFlag = "END";
    private String mFileFinish = "FILE_FINISH";
    private File saveFile;
    private FileOutputStream outputStream;

    public static final boolean TEST_XIAOMI = false; //是否小米手机蓝牙地址来测试

    @Override
    protected int setContentLayout() {
        return R.layout.activity_app_upgrade;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.cold_wallet_update);
        setDefaultNavigation();
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        llBluetooth = findViewById(R.id.ll_bluetooth);

        llQrcode = findViewById(R.id.ll_qrcode);

        imgBlue = findViewById(R.id.img_blue);
        bluAnim = (AnimationDrawable) imgBlue.getDrawable();
        bluAnim.start();

        tvState = findViewById(R.id.tv_state);
        pbPlan = findViewById(R.id.pb_plan);
        tvPlan = findViewById(R.id.tv_plan);

        bluetoothMacAddr = findViewById(R.id.img_bluetooth_mac_addr);
        bluetoothLoading = findViewById(R.id.pb_bluetooth_loading);
        bluetoothHint = findViewById(R.id.tv_bluetooth_hint);

        showBluetoothMacAddr(false);

    }

    private void showBluetoothMacAddr(boolean show) {
        if (show) {
            bluetoothMacAddr.setVisibility(View.VISIBLE);
            bluetoothLoading.setVisibility(View.GONE);
            bluetoothHint.setVisibility(View.VISIBLE);
            bluetoothHint.setText(R.string.cold_wallet_safegem_scan);
            int progress = 0;
            pbPlan.setProgress(progress);
            int showProgress = (int) ((pbPlan.getProgress() * 1.0f / pbPlan.getMax()) * 100);
            tvPlan.setText(showProgress + "%");
        } else {
            bluetoothMacAddr.setVisibility(View.GONE);
            bluetoothLoading.setVisibility(View.VISIBLE);
            bluetoothHint.setVisibility(View.VISIBLE);
            bluetoothHint.setText(R.string.cold_wallet_opening_bluetooth);
        }
    }

    @Override
    public void initData() {
        super.initData();
        MainWalletActivity.beAppUpgrade = true;

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(blueStateReceiver, intentFilter);
        bluetooth = new BluetoothSPP(this);
        if (!bluetooth.isBluetoothAvailable()) {
            finish();
        }

        String mBluetooth = "*BLUETOOTH:" + getBluetoothAddress() + "|" + WalletInfoManager.getCodeWalletVersionCode();
        try {
            Bitmap bitmap = QRCodeEncoderUtils.encodeAsBitmap(AppUpgradeActivity.this, mBluetooth);
            bluetoothMacAddr.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
        }

        saveFile = FileUtil.createFile();
        bluetooth.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                receiveData(message);
            }
        });

        bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceDisconnected() {
                if (saveFile.exists()) {
                    try {
                        if (outputStream != null) {
                            outputStream.flush();
                            outputStream.close();
                            outputStream = null;
                        }
                    } catch (Exception e) {
                    }
                    saveFile.delete();
                }
                cursor = 0;
                pbPlan.setProgress(0);
                int showProgress = (int) ((pbPlan.getProgress() * 1.0f / pbPlan.getMax()) * 100);
                tvPlan.setText(showProgress + "%");
                llBluetooth.setVisibility(View.GONE);
                llQrcode.setVisibility(View.VISIBLE);
                bluetoothHint.setText(R.string.cold_wallet_retry_update);
            }

            public void onDeviceConnectionFailed() {
                tvState.setText(R.string.cold_wallet_bluetooth_connection_failure);
                tvState.setTextColor(getColor(R.color.text_color_red));
            }

            public void onDeviceConnected(String name, String address) {
                llBluetooth.setVisibility(View.VISIBLE);
                llQrcode.setVisibility(View.GONE);
                tvState.setText(R.string.cold_wallet_bluetooth_connected);
                tvState.setTextColor(getColor(R.color.text_color));
            }

        });

    }

    private void receiveData(String message) {
        if (message.startsWith(mBeginFlag)) {
            int beginIndex = mBeginFlag.length();
            String beginHeader = message.substring(beginIndex, message.length());
            String[] headers = beginHeader.split("\\|");
            if (headers.length == 2) {
                md5 = headers[0];
                fileSize = Long.valueOf(headers[1]);
            }
        } else if (message.equals(mEndFlag)) {
            if (outputStream != null) {
                IoUtils.closeQuietly(outputStream);
            }
            String path = FileEncrypt.decodeFile(saveFile.getPath(), FileUtil.fileName, md5);
            if (!TextUtils.isEmpty(path)) {
                bluetooth.send(mFileFinish, true);
                RingManager.getInstance().playCrystal();
                FileUtil.deleteTempFile();
                CommonUtils.showProgressDialog(AppUpgradeActivity.this, getString(R.string.cold_wallet_auto_update), true);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent("action.receive.download");
                        intent.setPackage("com.android.settings");
                        sendBroadcast(intent);
                    }
                }, 1200);
            } else {
                if (saveFile.exists()) {
                    saveFile.delete();
                }
                ToastUtil.showToast(R.string.cold_wallet_md5_not_correct);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        go2Activity(AppUpgradeActivity.class);
                        finish();
                    }
                }, 2000);
            }
        } else {
            try {
                if (outputStream == null) {
                    outputStream = new FileOutputStream(saveFile, true);
                }
                byte[] decodeData = Base64.decode(message, Base64.DEFAULT);
                cursor += decodeData.length;
                int progress = (int) ((cursor * 1.0f / fileSize) * 100);
                if (progress <= 0) {
                    progress = 1;
                }
                pbPlan.setProgress(progress);
                int showProgress = (int) ((pbPlan.getProgress() * 1.0f / pbPlan.getMax()) * 100);
                tvPlan.setText(showProgress + "%");

                outputStream.write(decodeData, 0, decodeData.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onStart() {
        super.onStart();
        if (!bluetooth.isBluetoothEnabled()) {
            bluetooth.enable();
        } else {
            if (!bluetooth.isServiceAvailable()) {
                bluetooth.setupService();
                bluetooth.startService(BluetoothState.DEVICE_ANDROID);
            }
            showBluetoothMacAddr(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainWalletActivity.beAppUpgrade = false;
        bluetooth.stopService();
        unregisterReceiver(blueStateReceiver);
    }

    @Override
    public void onBackPressed() {
        CommonDialog dialog = new CommonDialog(AppUpgradeActivity.this);
        dialog.setTitle(R.string.tip);
        dialog.setContentText(getString(R.string.cold_wallet_exit));
        dialog.setOnClickCancelListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        dialog.setOnClickConfirmListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        dialog.show();
    }

    private BroadcastReceiver blueStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                if (state == BluetoothAdapter.STATE_ON) {
                    bluetooth.setupService();
                    bluetooth.startService(BluetoothState.DEVICE_ANDROID);
                    showBluetoothMacAddr(true);
                }
            }
        }
    };

    /**
     * 这个需要用Base64.DEFAULT
     *
     * @return
     */
    public String getBluetoothAddress() {
        String address = android.provider.Settings.Secure.getString(SafeColdApplication.mContext.getContentResolver(), "bluetooth_address");
        if(TEST_XIAOMI){
            address = "20:47:DA:FE:35:F3";
        }
        return Base64.encodeToString(address.getBytes(), Base64.DEFAULT);
    }

}
