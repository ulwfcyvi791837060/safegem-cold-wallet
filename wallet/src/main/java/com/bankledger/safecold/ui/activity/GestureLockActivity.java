package com.bankledger.safecold.ui.activity;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.ui.widget.CommonTextWidget;
import com.bankledger.safecold.utils.SharedPreferencesUtil;

/**
 * @author bankledger
 * @time 2018/8/14 10:02
 */
public class GestureLockActivity extends ToolbarBaseActivity {
    private String gesturePassword;
    private boolean gestureSwitch;
    private ViewStub vsNotSet;
    private ViewStub vsIsSet;
    private CommonTextWidget ctwGestureSwitch;
    private Switch sFingerprint;
    private Button btChangeGesture;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_gesture_lock;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.gesture_lock);
        vsNotSet = findViewById(R.id.vs_not_set);
        vsIsSet = findViewById(R.id.vs_is_set);
    }

    @Override
    public void initData() {
        super.initData();
        final SharedPreferencesUtil sp = SafeColdApplication.appSharedPreferenceUtil;
        gesturePassword = sp.get(Constants.KEY_SP_GESTURE_PASSWORD, "");
        gestureSwitch = sp.get(Constants.KEY_SP_GESTURE_SWITCH, false);
        if (TextUtils.isEmpty(gesturePassword)) {
            View view = vsNotSet.inflate();
            view.findViewById(R.id.bt_create_gesture).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    go2Activity(SetGestureActivity.class);
                }
            });
        } else {
            View view = vsIsSet.inflate();
            ctwGestureSwitch = view.findViewById(R.id.ctw_gesture_switch);
            ctwGestureSwitch.setLeftText(R.string.gesture_setting);
            sFingerprint = new Switch(this);
            sFingerprint.setChecked(gestureSwitch);
            ctwGestureSwitch.setRightView(sFingerprint);

            btChangeGesture = view.findViewById(R.id.bt_change_gesture);
            btChangeGesture.setVisibility(gestureSwitch ? View.VISIBLE : View.GONE);
            btChangeGesture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    go2Activity(SetGestureActivity.class);
                }
            });

            sFingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    sp.put(Constants.KEY_SP_GESTURE_SWITCH, isChecked);
                    btChangeGesture.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });


        }
    }
}
