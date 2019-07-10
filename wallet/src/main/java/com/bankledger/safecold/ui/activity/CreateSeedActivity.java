package com.bankledger.safecold.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.ui.widget.CircleProgressBar;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.xrandom.IUEntropySource;
import com.bankledger.safecold.xrandom.UEntropyCamera;
import com.bankledger.safecold.xrandom.UEntropyCollector;
import com.bankledger.safecold.xrandom.XRandom;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicCode;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class CreateSeedActivity extends ToolbarBaseActivity{
    private ArrayList<String> words = new ArrayList<>(12);
    private int[] wordIds = new int[]{R.id.tv_seed01, R.id.tv_seed02, R.id.tv_seed03,
            R.id.tv_seed04, R.id.tv_seed05, R.id.tv_seed06, R.id.tv_seed07, R.id.tv_seed08,
            R.id.tv_seed09, R.id.tv_seed10, R.id.tv_seed11, R.id.tv_seed12};
    private TextView[] tvWords = new TextView[wordIds.length];
    private RadioGroup rgLanguage;

    private byte[] seedByte;

    private int seedType = MnemonicCode.CODE_TYPE_ENGLISH;
    private UEntropyCollector entropyCollector;
    private XRandom xRandom;
    private RadioButton rbEnglish;
    private View llSeed;
    private CircleProgressBar pb01;
    private CircleProgressBar pb02;
    private CircleProgressBar pb03;
    private CircleProgressBar pb04;
    private CircleProgressBar pb05;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_seed_create;
    }

    @Override
    public void initView() {
        super.initView();

        pb01 = findViewById(R.id.pb_01);
        pb02 = findViewById(R.id.pb_02);
        pb03 = findViewById(R.id.pb_03);
        pb04 = findViewById(R.id.pb_04);
        pb05 = findViewById(R.id.pb_05);
        llSeed = findViewById(R.id.ll_seed);

        for (int i = 0; i < wordIds.length; i++) {
            tvWords[i] = findViewById(wordIds[i]);
        }

        rgLanguage = findViewById(R.id.rg_language);
        rgLanguage.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                seedType = checkedId == R.id.rb_english ? MnemonicCode.CODE_TYPE_ENGLISH : MnemonicCode.CODE_TYPE_CHINESE;
                if (seedByte == null) {
                    createSeedPassword();
                } else {
                    showSeed();
                }
            }
        });
        rbEnglish = findViewById(R.id.rb_english);

        findViewById(R.id.tv_change_seed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtils.isRepeatClick()) {
                    createSeedPassword();
                }
            }
        });

        findViewById(R.id.bt_check_seed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle args = new Bundle();
                args.putStringArrayList(Constants.INTENT_KEY1, words);
                go2Activity(SeedCheckActivity.class, args);
            }
        });
    }

    @Override
    public void initData() {
        entropyCollector = new UEntropyCollector(new UEntropyCamera((SurfaceView) findViewById(R.id.v_camera), entropyCollector),null);
        xRandom = new XRandom(entropyCollector);
        animSurface();
    }

    private void animSurface() {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            int progress = 0;

            public void run() {
                pb01.setProgress(progress);
                pb02.setProgress(progress - 100);
                pb03.setProgress(progress - 200);
                pb04.setProgress(progress - 300);
                pb05.setProgress(progress - 400);
                if (progress == 200) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            rbEnglish.setChecked(true);
                        }
                    });
                }
                if (progress == 500) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            llSeed.setVisibility(View.VISIBLE);
                            setTitle(R.string.seed_password);
                            setNavigation(R.drawable.ic_keyboard_arrow_left_white_30dp, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    SafeColdApplication.clearData();
                                    finish();
                                }
                            });
                        }
                    });
                    timer.cancel();
                }

                progress++;
            }
        }, 10, 10);
    }

    @Override
    protected void onResume() {
        super.onResume();
        entropyCollector.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        entropyCollector.onPause();
    }

    //随机种子密码
    private void createSeedPassword() {
        new CommonAsyncTask.Builder<Void, Void, byte[]>()
                .setIDoInBackground(new IDoInBackground<Void, Void, byte[]>() {
                    @Override
                    public byte[] doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        return HDAccount.randomSeedByte(xRandom);
                    }
                })
                .setIPostExecute(new IPostExecute<byte[]>() {
                    @Override
                    public void onPostExecute(byte[] seedBytes) {
                        seedByte = seedBytes;
                        showSeed();
                    }
                })
                .start();
    }

    private void showSeed() {
        words.clear();
        words.addAll(HDAccount.seedByte2Seed(seedByte, seedType));
        for (int i = 0; i < words.size(); i++) {
            tvWords[i].setText(words.get(i));
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
