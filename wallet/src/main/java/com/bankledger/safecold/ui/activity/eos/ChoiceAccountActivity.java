package com.bankledger.safecold.ui.activity.eos;

import android.os.Bundle;
import android.view.View;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecoldj.SafeColdSettings;


/**
 * Created by zm on 2018/11/15.
 */

public class ChoiceAccountActivity extends ToolbarBaseActivity implements View.OnClickListener{


    @Override
    protected int setContentLayout() {
        return R.layout.activity_eos_choice_account;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(SafeColdSettings.EOS);
    }

    @Override
    public void initData() {
        super.initData();
        findViewById(R.id.bt_import).setOnClickListener(this);
        findViewById(R.id.bt_create).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_import:
                Bundle args = new Bundle();
                args.putBoolean(Constants.INTENT_KEY1, false);
                go2Activity(NewAccountActivity.class, args);
                break;
            case R.id.bt_create:
                args = new Bundle();
                args.putBoolean(Constants.INTENT_KEY1, true);
                go2Activity(NewAccountActivity.class, args);
                break;
        }
    }
}
