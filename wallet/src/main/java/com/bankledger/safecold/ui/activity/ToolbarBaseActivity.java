package com.bankledger.safecold.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bankledger.safecold.R;

/**
 * @author bankledger
 * @time 2018/7/26 16:02
 */
public abstract class ToolbarBaseActivity extends BaseActivity {
    private Toolbar toolbar;
    private TextView tvTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar_base);
    }

    @Override
    public void initView() {
        super.initView();
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setVisibility(View.GONE);

        tvTitle = findViewById(R.id.tv_title);

        ViewGroup llContent = findViewById(R.id.ll_content);
        LayoutInflater.from(this).inflate(setContentLayout(), llContent, true);
    }

    protected abstract int setContentLayout();

    public void setTitle(String title) {
        toolbar.setVisibility(View.VISIBLE);
        tvTitle.setText(title);
    }

    public void setTitle(int titleId) {
        toolbar.setVisibility(View.VISIBLE);
        tvTitle.setText(titleId);
    }

    protected void setDefaultNavigation() {
        toolbar.setVisibility(View.VISIBLE);
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_left_white_30dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void setNavigation(int iconId, View.OnClickListener onNavigationClickListener) {
        toolbar.setVisibility(View.VISIBLE);
        toolbar.setNavigationIcon(iconId);
        toolbar.setNavigationOnClickListener(onNavigationClickListener);
    }

    public Toolbar getToolbar(){
        return toolbar;
    }
}
