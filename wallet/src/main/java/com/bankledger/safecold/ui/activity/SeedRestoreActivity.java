package com.bankledger.safecold.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.widget.WipeSpaceTextWatcher;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicCode;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeedRestoreActivity extends ToolbarBaseActivity {

    private Button mBtnRestore;
    private ArrayList<String> wordList = new ArrayList<>(12);

    private int[] wordIds = new int[]{R.id.et_seed01, R.id.et_seed02, R.id.et_seed03,
            R.id.et_seed04, R.id.et_seed05, R.id.et_seed06, R.id.et_seed07, R.id.et_seed08,
            R.id.et_seed09, R.id.et_seed10, R.id.et_seed11, R.id.et_seed12};
    private EditText[] etWords = new EditText[wordIds.length];


    private Map<EditText, TextWatcher> watcherMap = new HashMap<>(12);

    private boolean clicked = false;
    private Button btnScan;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_seed_restore;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.restore_wallet);

        for (int i = 0; i < wordIds.length; i++) {
            etWords[i] = findViewById(wordIds[i]);
            WipeSpaceTextWatcher wipeSpaceTextWatcher = new WipeSpaceTextWatcher(etWords[i]);
            watcherMap.put(etWords[i], wipeSpaceTextWatcher);
            etWords[i].addTextChangedListener(wipeSpaceTextWatcher);

            //用filter去空格时会对中文手写造成影响
//            etWords[i].setFilters(new InputFilter[]{new WipeSpaceInputFilter()});
        }

        mBtnRestore = findViewById(R.id.btn_restore);

        btnScan = findViewById(R.id.btn_scan);
        btnScan.setVisibility(View.GONE);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPermissions(Manifest.permission.CAMERA, new BaseActivity.PermissionCallBack() {
                    @Override
                    public void onGranted() {
                        go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE1);
                    }

                    @Override
                    public void onDenied() {
                    }
                });
            }
        });

        mBtnRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (clicked) {
                    return;
                }
                wordList.clear();
                for (int i = 0; i < etWords.length; i++) {
                    if (TextUtils.isEmpty(etWords[i].getText().toString())) {
                        ToastUtil.showToast(R.string.input_all_seed);
                        return;
                    }
                    wordList.add(etWords[i].getText().toString().trim());
                }
                checkWords();
            }
        });
    }

    private void checkWords() {
        clicked = true;
        new CommonAsyncTask.Builder<Void, Void, Boolean>()
                .setIDoInBackground(new IDoInBackground<Void, Void, Boolean>() {
                    @Override
                    public Boolean doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        try {
                            MnemonicCode.instance().toEntropy(wordList);
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<Boolean>() {
                    @Override
                    public void onPostExecute(Boolean aBoolean) {
                        clicked = false;
                        if (aBoolean) {
                            Bundle args = new Bundle();
                            args.putStringArrayList(Constants.INTENT_KEY1, wordList);
                            go2Activity(CreatePasswordActivity.class, args);
                        } else {
                            ToastUtil.showToast(R.string.seed_error);
                        }
                    }
                })
                .start();
    }


    @Override
    public void initData() {
        super.initData();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE1 && resultCode == Constants.RESULT_SUCCESS) {
            String result = data.getStringExtra(Constants.INTENT_KEY1);
            if (result.startsWith("*SEED:")) {
                try {
                    List<String> seedWords = QRCodeUtil.decodeSeedPassword(result);
                    if (seedWords.size() == etWords.length) {
                        for (int i = 0; i < seedWords.size(); i++) {
                            etWords[i].setText(seedWords.get(i));
                        }
                    } else {
                        ToastUtil.showToast(R.string.unidentifiable_qr_code);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtil.showToast(R.string.unidentifiable_qr_code);
                }
            } else {
                ToastUtil.showToast(R.string.unidentifiable_qr_code);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (EditText et : watcherMap.keySet()) {
            et.removeTextChangedListener(watcherMap.get(et));
        }
    }
}
