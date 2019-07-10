package com.bankledger.safecold.ui.fragment;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.safecold.R;
import com.bankledger.safecold.utils.WalletInfoManager;

/**
 * @author bankledger
 * @time 2018/9/20 14:27
 */
public class LockScreenFragment extends BaseFragment implements View.OnTouchListener {
    private View rlContent;
    private TextView mWalletName;
    private ImageView ivLess;
    private float downY;
    private float downX;
    private int contentHeight;
    private int contentWidth;
    private float factor;

    @Override
    public int setContentView() {
        return R.layout.fragment_lock_screen;
    }

    @Override
    public void initView() {
        rlContent = findViewById(R.id.rl_content);
        mWalletName = findViewById(R.id.tv_wallet_name);
        ivLess = findViewById(R.id.iv_less);
        rlContent.setOnTouchListener(this);
    }

    @Override
    public void initData() {
        String walletName = WalletInfoManager.getWalletName();
        mWalletName.setText(walletName);
        AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(getBaseActivity(), R.anim.anim_icon_up_unlock);
        ivLess.startAnimation(animationSet);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = event.getRawY();
                downX = event.getRawX();
                contentHeight = rlContent.getHeight();
                contentWidth = rlContent.getWidth();
                break;
            case MotionEvent.ACTION_MOVE:
                float mY = Math.abs(event.getRawY() - downY);
                float mX = Math.abs(event.getRawX() - downX);
                float factorX = mX / contentWidth;
                float factorY = mY / contentHeight;
                factor = factorX > factorY ? factorX : factorY;
                rlContent.setAlpha(1 - factor * 1.2F);
                rlContent.setScaleX(1 - factor * 0.5F);
                rlContent.setScaleY(1 - factor * 0.5F);
                break;
            case MotionEvent.ACTION_UP:
                if (factor > 0.2F) {
                    if (getActivity() instanceof UnlockScreenListener) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (getActivity() != null) {
                                    ((UnlockScreenListener) getActivity()).onUnlockScreen();
                                }
                            }
                        }, 100);
                    }
                } else {
                    rlContent.setAlpha(1);
                    rlContent.setScaleX(1);
                    rlContent.setScaleY(1);
                }
                break;
        }

        return true;
    }


    public interface UnlockScreenListener {
        void onUnlockScreen();
    }
}
