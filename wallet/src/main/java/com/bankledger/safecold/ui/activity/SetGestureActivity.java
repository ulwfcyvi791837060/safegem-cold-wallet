package com.bankledger.safecold.ui.activity;

import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.utils.Sha256Hash;

import java.util.List;

/**
 * @author bankledger
 * @time 2018/8/14 10:51
 */
public class SetGestureActivity extends ToolbarBaseActivity implements PatternLockViewListener {
    private PatternLockView plvGesture;
    private TextView tvExplain;

    private String tempGesture;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_set_gesture;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.set_gesture);
        tvExplain = findViewById(R.id.tv_explain);
        tvExplain.setText(R.string.draw_gesture);
        plvGesture = findViewById(R.id.plv_gesture);
        plvGesture.addPatternLockListener(this);
    }

    @Override
    public void initData() {
        super.initData();
    }

    @Override
    public void onStarted() {

    }

    @Override
    public void onProgress(List<PatternLockView.Dot> progressPattern) {

    }

    @Override
    public void onComplete(List<PatternLockView.Dot> pattern) {
        String gesture = PatternLockUtils.patternToString(plvGesture, pattern);
        if (gesture.length() < 4) {
            ToastUtil.showToast(R.string.gesture_dot_number);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    plvGesture.clearPattern();
                }
            }, 800);
            return;
        }

        if (TextUtils.isEmpty(tempGesture)) {
            tempGesture = gesture;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    plvGesture.clearPattern();
                    tvExplain.setText(R.string.submit_gesture);
                }
            }, 500);
        } else {
            if (!tempGesture.equals(gesture)) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        plvGesture.clearPattern();
                    }
                }, 500);
                ToastUtil.showToast(R.string.gesture_error);
            } else {
                ToastUtil.showToast(R.string.gesture_set_success);
                SafeColdApplication.appSharedPreferenceUtil.put(Constants.KEY_SP_GESTURE_PASSWORD, tempGesture);
                SafeColdApplication.appSharedPreferenceUtil.put(Constants.KEY_SP_GESTURE_SWITCH, true);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        go2Activity(SecuritySettingActivity.class);
                    }
                }, 500);
            }
        }
    }

    @Override
    public void onCleared() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        plvGesture.removePatternLockListener(this);
    }
}
