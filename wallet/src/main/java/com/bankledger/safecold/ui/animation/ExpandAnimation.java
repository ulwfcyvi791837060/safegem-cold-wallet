package com.bankledger.safecold.ui.animation;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * @author bankledger
 * @time 2018/7/20 17:31
 */
public class ExpandAnimation extends Animation {
    private View mAnimatedView;
    private LinearLayout.LayoutParams mViewLayoutParams;
    private int mMarginStart, mMarginEnd;
    private boolean mIsVisibleAfter;
    private boolean mWasEndedAlready = false;

    private ImageView arrow;

    /**
     * Initialize the animation
     *
     * @param view     The layout we want to animate
     * @param duration The duration of the animation, in ms
     */
    public ExpandAnimation(View open, View view, int duration) {

        setDuration(duration);
        arrow = (ImageView) open;
        mAnimatedView = view;
        mViewLayoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();

        // if the bottom margin is 0,
        // then after the animation will end it'll be negative, and invisible.
        mIsVisibleAfter = (mViewLayoutParams.bottomMargin == 0);

        //根据状态获取动画起始参数
        mMarginStart = mIsVisibleAfter ? 0 : (0 - view.getHeight());
        mMarginEnd = (mMarginStart == 0 ? (0 - view.getHeight()) : 0);

        mAnimatedView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);

        if (interpolatedTime < 1.0f) {

            // Calculating the new bottom margin, and setting it
            mViewLayoutParams.bottomMargin = mMarginStart
                    + (int) ((mMarginEnd - mMarginStart) * interpolatedTime);

            // Invalidating the layout, making us seeing the changes we made
            mAnimatedView.requestLayout();
            arrow.setRotation(mIsVisibleAfter ? -90 * (interpolatedTime) : -90 * (1-interpolatedTime));
            // Making sure we didn't run the ending before (it happens!)
        } else if (!mWasEndedAlready) {
            mViewLayoutParams.bottomMargin = mMarginEnd;
            mAnimatedView.requestLayout();
            if (mIsVisibleAfter) {
                mAnimatedView.setVisibility(View.GONE);
                arrow.setRotation(-90);
            }
            mWasEndedAlready = true;
        }
    }

}
