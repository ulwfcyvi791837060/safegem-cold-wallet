package com.bankledger.safecold.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bankledger.safecold.ui.activity.BaseActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基础Fragment
 * @author zhangmiao
 */
public abstract class BaseFragment extends Fragment {

    private View rootView = null;
    private Context context;

    public View getRootView() {
        return rootView;
    }

    private Handler mHandler = new Handler();

    public static final Logger log = LoggerFactory.getLogger(BaseFragment.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(setContentView(), container, false);
        rootView.setClickable(true);
        initView();
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void go2Activity(Class target) {
        go2Activity(target, null);
    }

    public void go2Activity(Class target, Bundle argument) {
        Intent startActivityIntent = new Intent(context, target);
        if(argument!=null)
        startActivityIntent.putExtras(argument);
        startActivity(startActivityIntent);
    }

    public void go2ActivityForResult(Class target, int requestCode) {
        go2ActivityForResult(target, requestCode, null);
    }

    public void go2ActivityForResult(Class target, int requestCode, Bundle argument) {
        Intent startActivityIntent = new Intent(context, target);
        if (argument != null)
            startActivityIntent.putExtras(argument);
        startActivityForResult(startActivityIntent, requestCode);
    }


    /**
     * 界面布局resId
     *
     * @return 布局resId
     */
    public abstract int setContentView();

    /**
     * 初始化界面
     */
    public abstract void initView();

    /**
     * 初始化界面数据
     */
    public abstract void initData();


    /**
     * 通过Id查找View， 为了和Activity统一。
     *
     * @param id
     * @return view
     */
    public <T extends View> T findViewById(int id) {
        if (getRootView() == null) {
            return null;
        }
        return rootView.findViewById(id);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public boolean onBackPressed() {
        return false;
    }

    public BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

}

