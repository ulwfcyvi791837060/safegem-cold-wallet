package com.bankledger.safecold.ui.activity;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bankledger.safecold.R;
import com.bankledger.safecold.utils.EncryptionChipManagerV2;

/**
 * @author bankledger
 * @time 2018/9/26 17:32
 */
public class EncryptionChipTestActivity extends ToolbarBaseActivity {

    private TextView info;

    private final long ALL_RIGHTS = 0x000000FF;// FF：所有人权限

    private String DEFAULT_PIN = "safewallet";
    private String NEW_PIN = "111111";


    private EncryptionChipManagerV2 encryptionChipManager;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_encryption_chip_test;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle("EncryptionChipTestActivity");
        setDefaultNavigation();
        info = findViewById(R.id.info);

        findViewById(R.id.oldWrite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long ret = encryptionChipManager.writeData("content123456", DEFAULT_PIN, encryptionChipManager.APP_NAME, encryptionChipManager.FILE_NAME, ALL_RIGHTS);
                if (ret == 0) {
                    info.setText("old_save:Success");
                } else {
                    info.setText("old_save:Fail:" + ret);
                }
            }
        });

        findViewById(R.id.oldRead).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                info.setText(encryptionChipManager.readData(encryptionChipManager.APP_NAME, encryptionChipManager.FILE_NAME));
            }
        });

        findViewById(R.id.oldDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long ret = encryptionChipManager.deleteApp(encryptionChipManager.APP_NAME);
                if (ret == 0) {
                    info.setText("old_del:Success");
                } else {
                    info.setText("old_del:Fail");
                }
            }
        });

        //新的接口
        findViewById(R.id.newWrite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long ret = encryptionChipManager.writeNewData("new_content123456", NEW_PIN, encryptionChipManager.NEW_APP_NAME, encryptionChipManager.NEW_FILE_NAME, ALL_RIGHTS);
                if (ret == 0) {
                    info.setText("save:Success");
                } else {
                    info.setText("save:Fail:" + ret);
                }
            }
        });

        findViewById(R.id.newRead).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                info.setText(encryptionChipManager.readNewData(encryptionChipManager.NEW_APP_NAME, encryptionChipManager.NEW_FILE_NAME, NEW_PIN));
            }
        });

        findViewById(R.id.fromOldToNew).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldData = encryptionChipManager.readData(encryptionChipManager.APP_NAME, encryptionChipManager.FILE_NAME);
                if (!TextUtils.isEmpty(oldData)) {
                    encryptionChipManager.deleteApp(encryptionChipManager.APP_NAME);
                    encryptionChipManager.writeNewData(oldData, NEW_PIN, encryptionChipManager.NEW_APP_NAME, encryptionChipManager.NEW_FILE_NAME, ALL_RIGHTS);
                }
                info.setText("bak:Success");
            }
        });

        findViewById(R.id.updatePWD).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptionChipManager.deleteApp(encryptionChipManager.APP_NAME);
                encryptionChipManager.deleteApp(encryptionChipManager.NEW_APP_NAME);
                long ret = encryptionChipManager.writeNewData("我是最新的", NEW_PIN, encryptionChipManager.NEW_APP_NAME, encryptionChipManager.NEW_FILE_NAME, ALL_RIGHTS);
                if (ret == 0) {
                    info.setText("update:Success");
                } else {
                    info.setText("update:Fail:" + ret);
                }
            }
        });


        findViewById(R.id.newDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long ret = encryptionChipManager.deleteApp(encryptionChipManager.NEW_APP_NAME);
                if (ret == 0) {
                    info.setText("del:Success");
                } else {
                    info.setText("del:Fail:" + ret);
                }
            }
        });

        findViewById(R.id.hasWritedFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean ret = encryptionChipManager.hasMnemonicSeed();
                info.setText("has: " + ret);
            }
        });

    }

    @Override
    public void initData() {
        super.initData();
        encryptionChipManager = EncryptionChipManagerV2.getInstance();
    }

}
