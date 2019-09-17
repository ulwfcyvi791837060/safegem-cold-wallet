package com.bankledger.safecold.ui.activity;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.transition.ChangeClipBounds;
import android.support.transition.TransitionManager;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.utils.RingManager;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.core.HDAddressManager;

/**
 * @author bankledger
 * @time 2018/7/19 10:52
 */
public class StartActivity extends BaseActivity {

    private final long OVER_TIME = 2000;
    private ViewGroup rootLayout;
    private ImageView appLogo;
    private TextView mWalletName, mWalletNameBlue;
    private ChangeClipBounds clip;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RingManager.getInstance().playStart();
        setContentView(R.layout.activity_start);
    }

    @Override
    public void initView() {
        super.initView();
        rootLayout = findViewById(R.id.root_layout);
        appLogo = findViewById(R.id.app_logo);
        mWalletNameBlue = findViewById(R.id.tv_wallet_name_blue);
        mWalletName = findViewById(R.id.tv_wallet_name);
    }

    int appLogoHeight = 0;

    @Override
    public void initData() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toOther();
            }
        }, OVER_TIME);

        String walletName = WalletInfoManager.getWalletName();
        mWalletName.setText(walletName);
        mWalletNameBlue.setText(walletName);

        appLogo.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                appLogo.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                appLogoHeight = (appLogoHeight == 0) ? appLogo.getHeight() : appLogoHeight;
                ooTransition();
            }
        });
    }

    private void ooTransition() {
        TransitionManager.endTransitions(rootLayout);
        appLogo.setClipBounds(new Rect(0, 0, appLogoHeight, appLogoHeight));
        mWalletName.setClipBounds(new Rect(0, 0, mWalletName.getWidth(), mWalletName.getHeight()));
        TransitionManager.beginDelayedTransition(rootLayout, clip);
        appLogo.setClipBounds(new Rect(0, 0, appLogoHeight, 0));
        mWalletName.setClipBounds(new Rect(0, 0, mWalletName.getWidth(), 0));

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        clip = new ChangeClipBounds();
        clip.addTarget(appLogo);
        clip.addTarget(mWalletName);
        clip.setDuration(OVER_TIME);
    }


    private void toOther() {
        new CommonAsyncTask.Builder<Void, Void, Boolean>()
                .setIDoInBackground(new IDoInBackground<Void, Void, Boolean>() {
                    @Override
                    public Boolean doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        return HDAddressManager.getInstance().hasMnemonicSeed();
                    }
                })
                .setIPostExecute(new IPostExecute<Boolean>() {
                    @Override
                    public void onPostExecute(Boolean aBoolean) {
                        if (aBoolean) {
                            go2Activity(PasswordUnlockActivity.class);
                        } else {
                            go2Activity(SelectLanguageActivity.class);
                        }
                        finish();
                    }
                })
                .start();
    }
}
