package com.bankledger.safecold.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import com.bankledger.safecold.utils.ToastUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zm on 2018/6/22.
 */

public abstract class BaseActivity extends AppCompatActivity {


    private static final int VERIFY_CODE = 0x01; //申请权限代码
    private PermissionCallBack callBack;
    public static final Logger log = LoggerFactory.getLogger(BaseActivity.class);
    protected Handler mHandler = new Handler();
    private InputMethodManager imm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        initView();
        initData();
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public void initView() {

    }

    public void initData() {

    }

    public void go2Activity(Intent target) {
        startActivity(target);
    }

    public void go2ActivityNotAnim(Class target) {
        go2Activity(target, null);
        overridePendingTransition(0, 0);
    }

    public void go2Activity(Class target) {
        go2Activity(target, null);
    }

    public void go2Activity(Class target, Bundle argument) {
        Intent startActivityIntent = new Intent(this, target);
        if (argument != null)
            startActivityIntent.putExtras(argument);
        startActivity(startActivityIntent);
    }

    public void go2ActivityForResult(Class target, int requestCode) {
        go2ActivityForResult(target, requestCode, null);
    }

    public void go2ActivityForResult(Class target, int requestCode, Bundle argument) {
        Intent startActivityIntent = new Intent(this, target);
        if (argument != null)
            startActivityIntent.putExtras(argument);
        startActivityForResult(startActivityIntent, requestCode);
    }


    //点击编辑框外隐藏键盘
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (this.getCurrentFocus() != null) {
                if (this.getCurrentFocus().getWindowToken() != null) {
                    imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 申请权限
     *
     * @param requestPermissions
     * @param callBack
     */
    public void verifyPermissions(String requestPermissions, PermissionCallBack callBack) {
        this.callBack = callBack;
        int checkPermission = ActivityCompat.checkSelfPermission(this, requestPermissions);
        if (checkPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    BaseActivity.this,
                    new String[]{requestPermissions},
                    VERIFY_CODE
            );
        } else {
            callBack.onGranted();
        }
    }

    /**
     * 权限结果
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (VERIFY_CODE == requestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                callBack.onGranted();
            } else {
                // Permission Denied
                callBack.onDenied();
            }
        }
    }

    /**
     * 权限返回
     */
    public interface PermissionCallBack {
        void onGranted();

        void onDenied();
    }

    /**
     * @return 判断当前Activity是否处于活跃状态
     */
    public boolean isViewActive() {
        return !(isFinishing() || isDestroyed());
    }

    @Override
    protected void onPause() {
        ToastUtil.cancel();
        super.onPause();
    }
}
