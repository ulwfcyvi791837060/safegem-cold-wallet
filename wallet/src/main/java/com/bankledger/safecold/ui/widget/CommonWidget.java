package com.bankledger.safecold.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bankledger.safecold.R;
import com.bankledger.safecold.utils.DPUtils;

/**
 * 公用组件
 *
 * @author zm
 */
public abstract class CommonWidget extends LinearLayout {

    public CommonWidget(Context context) {
        super(context);
        initView();
    }

    public CommonWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();

        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.CommonWidget);

        Drawable leftImage = mTypedArray
                .getDrawable(R.styleable.CommonWidget_left_image);

        Drawable rightImage = mTypedArray
                .getDrawable(R.styleable.CommonWidget_right_image);


        context.obtainStyledAttributes(attrs,
                R.styleable.CommonWidget);

        if (leftImage != null) {
            setLeftImageDrawable(leftImage);
        }
        if (rightImage != null) {
            setRightImageDrawable(rightImage);
        }

        mTypedArray.recycle();
    }

    public void initView() {
        inflate(getContext(), R.layout.common_widget, this);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        createView();
        int padding = DPUtils.dip2px(getContext(), 10);
        setPaddingLeft(padding);
        setPaddingRight(padding);
    }

    public abstract void createView();

    public void setLeftView(View v) {
        setView(R.id.common_ll_left, v);
    }

    public void setLeftView(int layoutResId) {
        setView(R.id.common_ll_left, layoutResId);
    }

    public void setMiddleView(View v) {
        setView(R.id.common_ll_middle, v);
    }

    public void setMiddleView(int layoutResId) {
        setView(R.id.common_ll_middle, layoutResId);
    }

    public void setRightView(View v) {
        setView(R.id.common_ll_right, v);
    }

    public void setRightView(int layoutResId) {
        setView(R.id.common_ll_right, layoutResId);
    }

    private void setView(int rootId, View v) {
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        LinearLayout ll = (LinearLayout) findViewById(rootId);
        ll.removeAllViews();
        ll.addView(v, lp);
    }

    private void setView(int rootId, int layoutId) {
        LinearLayout ll = (LinearLayout) findViewById(rootId);
        ll.removeAllViews();
        inflate(getContext(), layoutId, ll);
    }

    public void setLeftImageResource(int resId) {
        View view = findViewById(R.id.common_img_left);
        if (view != null && view instanceof ImageView) {
            ImageView image = (ImageView) view;
            if (resId != 0) {
                image.setVisibility(View.VISIBLE);
                image.setImageResource(resId);
            } else {
                image.setVisibility(View.GONE);
            }
        }
    }

    public void setRightImageResource(int resId) {
        View view = findViewById(R.id.common_img_right);
        if (view != null && view instanceof ImageView) {
            ImageView image = (ImageView) view;
            if (resId != 0) {
                image.setVisibility(View.VISIBLE);
                image.setImageResource(resId);
            } else {
                image.setVisibility(View.GONE);
            }
        }
    }

    public void setLeftImageDrawable(Drawable drawable) {
        View view = findViewById(R.id.common_img_left);
        if (view != null && view instanceof ImageView) {
            ImageView image = (ImageView) view;
            if (drawable != null) {
                image.setVisibility(View.VISIBLE);
                image.setImageDrawable(drawable);
            } else {
                image.setVisibility(View.GONE);
            }
        }
    }

    public void setRightImageDrawable(Drawable drawable) {
        View view = findViewById(R.id.common_img_right);
        if (view != null && view instanceof ImageView) {
            ImageView image = (ImageView) view;
            if (drawable != null) {
                image.setVisibility(View.VISIBLE);
                image.setImageDrawable(drawable);
            } else {
                image.setVisibility(View.GONE);
            }
        }
    }

    public void setPaddingLeft(int left) {
        View view = findViewById(R.id.common_ll_left);
        view.setPadding(left, 0, 0, 0);
    }

    public void setPaddingRight(int right) {
        View view = findViewById(R.id.common_ll_right);
        view.setPadding(0, 0, right, 0);
    }

    public void setPaddingCenter(int left, int top, int right, int bottom) {
        View view = findViewById(R.id.common_ll_middle);
        view.setPadding(left, top, right, bottom);
    }

    @Override
    public void setBackgroundColor(int color) {
        findViewById(R.id.common_ll_root).setBackgroundColor(color);
    }

    @Override
    public void setBackgroundResource(int resId) {
        findViewById(R.id.common_ll_root).setBackgroundResource(resId);
    }

    public ImageView getRightImageView() {
        return findViewById(R.id.common_img_right);
    }

    public ImageView getLeftImageView() {
        return findViewById(R.id.common_img_left);
    }
}
