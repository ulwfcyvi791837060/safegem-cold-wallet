package com.bankledger.safecold.ui.fragment;

import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.ui.activity.UnlockActivity;
import com.bankledger.safecold.utils.DialogUtil;

import java.util.List;

/**
 * @author bankledger
 * @time 2018/9/20 14:31
 */
public class VerifyGestureFragment extends BaseFragment implements PatternLockViewListener {
    private static final int MAX_ERROR_COUNT = 5;
    private PatternLockView plvGesture;
    private TextView tvExplain;

    @Override
    public int setContentView() {
        return R.layout.fragment_verify_gesture;
    }

    @Override
    public void initView() {
        plvGesture = findViewById(R.id.plv_gesture);
        plvGesture.addPatternLockListener(this);
        tvExplain = findViewById(R.id.tv_explain);
        tvExplain.setText(null);
        findViewById(R.id.tv_to_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof ForgetGestureListener) {
                    ((ForgetGestureListener) getActivity()).onForgetGesture();
                }
            }
        });
    }

    @Override
    public void initData() {

    }

    @Override
    public void onStarted() {

    }

    @Override
    public void onProgress(List<PatternLockView.Dot> progressPattern) {

    }

    @Override
    public void onComplete(List<PatternLockView.Dot> pattern) {
        String password = SafeColdApplication.appSharedPreferenceUtil.get(Constants.KEY_SP_GESTURE_PASSWORD, "");
        String gesture = PatternLockUtils.patternToString(plvGesture, pattern);

        if (!password.equals(gesture)) {
            tvExplain.setText(String.format(getString(R.string.gesture_error_count), MAX_ERROR_COUNT - ((UnlockActivity) getActivity()).gestureErrorCount));
            startShakeByViewAnim(tvExplain);
            if (((UnlockActivity) getActivity()).gestureErrorCount >= MAX_ERROR_COUNT) {
                DialogUtil.showTextDialog(getContext(), R.string.gesture_error2, R.string.gesture_error_hint2, new DialogUtil.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String content) {
                        //连续错五次，清空手势密码
                        SafeColdApplication.appSharedPreferenceUtil.put(Constants.KEY_SP_GESTURE_SWITCH, false);
                        SafeColdApplication.appSharedPreferenceUtil.put(Constants.KEY_SP_GESTURE_PASSWORD, "");
                        if (getActivity() instanceof ForgetGestureListener) {
                            ((ForgetGestureListener) getActivity()).onForgetGesture();
                        }
                    }
                });
                return;
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    plvGesture.clearPattern();
                }
            }, 800);
            ((UnlockActivity) getActivity()).gestureErrorCount++;
        } else {
            getBaseActivity().finish();
        }

    }

    @Override
    public void onCleared() {

    }

    private void startShakeByViewAnim(View view) {
        if (view == null) {
            return;
        }
        //由小变大
        Animation scaleAnim = new ScaleAnimation(0.9F, 1.1F, 0.9F, 1.1F);
        //从左向右
        Animation rotateAnim = new RotateAnimation(-0.2F, 0.2F, Animation.RELATIVE_TO_SELF, 0.2f, Animation.RELATIVE_TO_SELF, 0.2f);

        scaleAnim.setDuration(100);
        rotateAnim.setDuration(100 / 8);
        rotateAnim.setRepeatMode(Animation.REVERSE);
        rotateAnim.setRepeatCount(8);

        AnimationSet smallAnimationSet = new AnimationSet(false);
        smallAnimationSet.addAnimation(scaleAnim);
        smallAnimationSet.addAnimation(rotateAnim);

        view.startAnimation(smallAnimationSet);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        plvGesture.removePatternLockListener(this);
    }

    public interface ForgetGestureListener {
        void onForgetGesture();
    }
}
