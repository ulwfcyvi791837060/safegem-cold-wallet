package com.bankledger.safecold.ui.activity;

import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.utils.CrashHandler;

import java.io.File;

/**
 * 崩溃日志
 * @author bankledger
 * @time 2018/9/25 13:33
 */
public class CrashLogActivity extends ToolbarBaseActivity {
    private TextView tvLog;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_crash_log;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle("CRASH_LOG");
        setDefaultNavigation();
        tvLog = findViewById(R.id.tv_log);
    }

    @Override
    public void initData() {
        super.initData();
        File logFile = (File) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        tvLog.setText(CrashHandler.getInstance().getCrashLog(logFile));
    }
}
