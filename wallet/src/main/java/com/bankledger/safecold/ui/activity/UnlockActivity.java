package com.bankledger.safecold.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.ui.fragment.LockScreenFragment;
import com.bankledger.safecold.ui.fragment.VerifyGestureFragment;
import com.bankledger.safecold.ui.fragment.VerifyPasswordFragment;
import com.bankledger.safecold.utils.ToastUtil;


/**
 * @author bankledger
 * @time 2018/8/17 14:07
 */
public class UnlockActivity extends BaseActivity implements LockScreenFragment.UnlockScreenListener, VerifyGestureFragment.ForgetGestureListener {

    private int failedCount = 0;

    public int gestureErrorCount = 1;

    private FingerprintManagerCompat fingerprintManager;
    private CancellationSignal mCancellationSignal;
    private FragmentManager fragmentManager;
    private UnlockBroadcastReceiver unlockBroadcastReceiver;


    private FingerprintManagerCompat.AuthenticationCallback authCallback = new FingerprintManagerCompat.AuthenticationCallback() {

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            super.onAuthenticationError(errMsgId, errString);
            //mCancellationSignal.cancel() 时会回调 errMsgId==5 这种情况不做处理
            //连续 fail 5次后会回调 errMsgId==7
            //连续 fail 5次后新打开监听指纹页面会首先进入onAuthenticationError回调，这里判断如果未进入onAuthenticationFailed前 不做处理
            if (errMsgId == 7 && failedCount > 0) {
                ToastUtil.showToast(errString.toString());
            }
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            if (!isFinishing() && !isDestroyed()) {
                finish();
            }
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            failedCount++;
            ToastUtil.showToast(getString(R.string.fingerprint_error));
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        go2Activity(UnlockActivity.class);
        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);
    }

    @Override
    public void initView() {
        super.initView();
        findViewById(R.id.fl_content);
    }

    @Override
    public void initData() {
        super.initData();
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().add(R.id.fl_content, new LockScreenFragment()).commit();
        unlockBroadcastReceiver = new UnlockBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("action.fingerprint.customverify");
        registerReceiver(unlockBroadcastReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyFingerprint();
    }

    protected void verifyFingerprint() {
        if (SafeColdApplication.appSharedPreferenceUtil.get(Constants.KEY_SP_FINGERPRINT_SWITCH, false)) {
            fingerprintManager = FingerprintManagerCompat.from(this);
            mCancellationSignal = new CancellationSignal();
            fingerprintManager.authenticate(null, 0, mCancellationSignal, authCallback, null);
        }
    }

    @Override
    public void onUnlockScreen() {
        if (SafeColdApplication.appSharedPreferenceUtil.get(Constants.KEY_SP_GESTURE_SWITCH, false)) {
            fragmentManager.beginTransaction().replace(R.id.fl_content, new VerifyGestureFragment()).commit();
        } else {
            fragmentManager.beginTransaction().replace(R.id.fl_content, new VerifyPasswordFragment()).commit();
        }
    }

    @Override
    public void onForgetGesture() {
        fragmentManager.beginTransaction().replace(R.id.fl_content, new VerifyPasswordFragment()).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(unlockBroadcastReceiver);
        if (mCancellationSignal != null)
            mCancellationSignal.cancel();

    }

    private class UnlockBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SafeColdApplication.appSharedPreferenceUtil.get(Constants.KEY_SP_FINGERPRINT_SWITCH, false) && !isFinishing() && !isDestroyed()) {
                finish();
            }
        }
    }
}
