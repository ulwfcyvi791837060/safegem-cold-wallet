package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.AsyncTaskResult;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.crypto.SecureCharSequence;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicCode;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicException;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.WriterException;

import java.util.ArrayList;
import java.util.List;

public class SeedBackupActivity extends ToolbarBaseActivity {

    private List<String> words;
    private int[] wordIds = new int[]{R.id.tv_seed01, R.id.tv_seed02, R.id.tv_seed03,
            R.id.tv_seed04, R.id.tv_seed05, R.id.tv_seed06, R.id.tv_seed07, R.id.tv_seed08,
            R.id.tv_seed09, R.id.tv_seed10, R.id.tv_seed11, R.id.tv_seed12};
    private TextView[] tvWords = new TextView[wordIds.length];

    private RadioGroup rgLanguage;
    private int seedType = MnemonicCode.CODE_TYPE_ENGLISH;
    private byte[] mnemonicSeed;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_seed_backup;
    }

    @Override
    public void initView() {
        super.initView();

        setDefaultNavigation();
        setTitle(R.string.backups_seed);
        for (int i = 0; i < wordIds.length; i++) {
            tvWords[i] = findViewById(wordIds[i]);
        }

        rgLanguage = findViewById(R.id.rg_language);
        rgLanguage.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                seedType = checkedId == R.id.rb_english ? MnemonicCode.CODE_TYPE_ENGLISH : MnemonicCode.CODE_TYPE_CHINESE;
                getSeedPassword();
            }
        });

        findViewById(R.id.bt_seed_qr_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CommonUtils.isRepeatClick()) {
                    showSeedQrCode();
                }
            }
        });
    }

    @Override
    public void initData() {
        super.initData();
        Intent intent = getIntent();
        mnemonicSeed = intent.getByteArrayExtra(Constants.INTENT_KEY1);
        rgLanguage.check(R.id.rb_english);
    }

    private void getSeedPassword() {
        new CommonAsyncTask.Builder<Void, Void, AsyncTaskResult<List<String>>>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(SeedBackupActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<Void, Void, AsyncTaskResult<List<String>>>() {
                    @Override
                    public AsyncTaskResult<List<String>> doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        try {
                            return new AsyncTaskResult<>(MnemonicCode.instance().toMnemonic(mnemonicSeed, seedType));
                        } catch (MnemonicException.MnemonicLengthException e) {
                            e.printStackTrace();
                            return new AsyncTaskResult<>(e);
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<AsyncTaskResult<List<String>>>() {
                    @Override
                    public void onPostExecute(AsyncTaskResult<List<String>> result) {
                        CommonUtils.dismissProgressDialog();
                        if (result.isSuccess()) {
                            words = result.getResult();
                            for (int i = 0; i < words.size(); i++) {
                                tvWords[i].setText(words.get(i));
                            }
                        } else {
                            finish();
                        }
                    }
                })
                .start();
    }

    /**
     * 显示种子二维码
     */
    private void showSeedQrCode() {
        StringBuffer sbWord = new StringBuffer();
        sbWord.append("*SEED:");
        for (int i = 0; i < words.size(); i++) {
            sbWord.append(words.get(i));
            sbWord.append(" ");
        }
        sbWord.deleteCharAt(sbWord.length() - 1);
        new CommonAsyncTask.Builder<String, Void, Bitmap>()
                .setIDoInBackground(new IDoInBackground<String, Void, Bitmap>() {
                    @Override
                    public Bitmap doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        try {
                            return QRCodeEncoderUtils.encodeAsBitmap(SeedBackupActivity.this, strings[0]);
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
                            DialogUtil.showImageDialog(SeedBackupActivity.this, bitmap, "");
                        }
                    }
                }).start(sbWord.toString());
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
