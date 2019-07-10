package com.bankledger.safecold.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.TimeUtils;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.bankledger.protobuf.bean.TransDate;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.utils.DateTimeUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;

import java.util.Calendar;
import java.util.Date;

public class AddWalletActivity extends BaseActivity {

    private Button mBtnNewWallet;
    private Button mBtnBackupWallet;
    private TextView mProtocol;
    private CheckBox cbProtocol;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wallet);
    }

    @Override
    public void initView() {
        mBtnNewWallet = findViewById(R.id.btn_new_wallet);
        mBtnBackupWallet = findViewById(R.id.btn_backup_wallet);
        mBtnNewWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cbProtocol.isChecked()) {
                    verifyPermissions(Manifest.permission.CAMERA, new BaseActivity.PermissionCallBack() {
                        @Override
                        public void onGranted() {
                            go2Activity(CreateSeedActivity.class);
                        }

                        @Override
                        public void onDenied() {
                        }
                    });
                } else {
                    ToastUtil.showToast(R.string.agree_protocol);
                }
            }
        });
        mBtnBackupWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cbProtocol.isChecked()) {
                    go2Activity(SeedRestoreActivity.class);
                } else {
                    ToastUtil.showToast(R.string.agree_protocol);
                }
            }
        });

        cbProtocol = findViewById(R.id.cb_protocol);

        mProtocol = findViewById(R.id.protocol);
        mProtocol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                go2Activity(WebViewActivity.class);
            }
        });
    }

    @Override
    public void onBackPressed() {

    }

}
