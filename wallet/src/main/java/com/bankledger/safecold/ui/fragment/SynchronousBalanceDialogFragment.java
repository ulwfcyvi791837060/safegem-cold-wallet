package com.bankledger.safecold.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bankledger.protobuf.bean.Address;
import com.bankledger.protobuf.bean.TransAddress;
import com.bankledger.protobuf.bean.TransDate;
import com.bankledger.protobuf.utils.ProtoUtils;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.adapter.ViewPagerStateAdapter;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.activity.BaseActivity;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bankledger
 * @time 2018/8/13 15:39
 */
public class SynchronousBalanceDialogFragment extends DialogFragment {
    private final static String KEY_CONVERT = "key_convert";
    private final static String KEY_TYPE = "key_type";
    private final static int KEY_TYPE_ETHTOKEN = 0;
    private final static int KEY_TYPE_HDADDRESS = 1;

    private int type;
    private HDAddress hdAddress;
    private ETHToken ethToken;

    private Button mBtnBalance;
    private ViewPager vpView;
    private ViewPagerStateAdapter adapter;
    private List<Fragment> mFgtList = new ArrayList<>();
    private TextView tvPage;
    private View ivArrowLeft;
    private View ivArrowRight;
    private BaseActivity mActivity;
    private View view;

    public static SynchronousBalanceDialogFragment newInstance(HDAddress hdAddress) {
        SynchronousBalanceDialogFragment fragment = new SynchronousBalanceDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_CONVERT, hdAddress);
        args.putInt(KEY_TYPE, KEY_TYPE_HDADDRESS);
        fragment.setArguments(args);
        return fragment;
    }

    public static SynchronousBalanceDialogFragment newInstance(ETHToken ethToken) {
        SynchronousBalanceDialogFragment fragment = new SynchronousBalanceDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_CONVERT, ethToken);
        args.putInt(KEY_TYPE, KEY_TYPE_ETHTOKEN);
        fragment.setArguments(args);
        return fragment;
    }

    public SynchronousBalanceDialogFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = (BaseActivity) activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().setCancelable(false);
        getDialog().setCanceledOnTouchOutside(false);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CommonDialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_synchronous_balance, null, false);
        initView(view);
        initData();
        return view;
    }


    private void initView(View view) {
        view.findViewById(R.id.iv_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        vpView = view.findViewById(R.id.vp_view);
        tvPage = view.findViewById(R.id.tv_page);

        mBtnBalance = view.findViewById(R.id.btn_balance);
        mBtnBalance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.verifyPermissions(Manifest.permission.CAMERA, new BaseActivity.PermissionCallBack() {
                    @Override
                    public void onGranted() {
                        mActivity.go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE1);
                    }

                    @Override
                    public void onDenied() {

                    }
                });
            }
        });

        ivArrowLeft = view.findViewById(R.id.iv_arrow_left);
        ivArrowLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = vpView.getCurrentItem();
                vpView.setCurrentItem(--position);
            }
        });

        ivArrowRight = view.findViewById(R.id.iv_arrow_right);
        ivArrowRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = vpView.getCurrentItem();
                vpView.setCurrentItem(++position);
            }
        });
    }

    public void initData() {
        type = getArguments().getInt(KEY_TYPE);
        if (type == KEY_TYPE_ETHTOKEN) {
            ethToken = (ETHToken) getArguments().get(KEY_CONVERT);
        } else if (type == KEY_TYPE_HDADDRESS) {
            hdAddress = (HDAddress) getArguments().get(KEY_CONVERT);
        } else {
            dismiss();
            return;
        }
        showView();
    }

    private void showView() {
        String content = "";
        if (type == KEY_TYPE_ETHTOKEN) {
            int isToken = ethToken.isEth() ? 1 : ethToken.isErc20() ? 2 : 3;
            content = ProtoUtils.encodeAddress(
                    new TransAddress(WalletInfoManager.getWalletNumber(),
                            new Address(ethToken.name, ethToken.ethAddress, isToken, ethToken.contractsAddress)));
        } else if (type == KEY_TYPE_HDADDRESS) {
            int coinType = hdAddress.isUsdt() ? 6 : 0;
            content = ProtoUtils.encodeAddress(
                    new TransAddress(WalletInfoManager.getWalletNumber(),
                            new Address(hdAddress.getCoin(), hdAddress.getAddress(), coinType, hdAddress.getAddress())));
        }

        adapter = new ViewPagerStateAdapter(getChildFragmentManager());
        vpView.setAdapter(adapter);
        List<String> mList = QRCodeUtil.encodePage(content);
        ivArrowLeft.setVisibility(mList.size() > 1 ? View.VISIBLE : View.INVISIBLE);
        ivArrowRight.setVisibility(mList.size() > 1 ? View.VISIBLE : View.INVISIBLE);
        for (String item : mList) {
            QrCodeFragment fragment = new QrCodeFragment();
            Bundle args = new Bundle();
            args.putString(Constants.INTENT_KEY1, item);
            fragment.setArguments(args);
            mFgtList.add(fragment);
        }
        adapter.setFragment(mFgtList);
        setTvPage(vpView.getCurrentItem());
        vpView.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setTvPage(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setTvPage(int position) {
        tvPage.setText(++position + "/" + mFgtList.size());
    }
}
