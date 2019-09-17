package com.bankledger.safecold.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.utils.CrashHandler;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.WriterException;

import java.io.File;
import java.util.List;

/**
 * @author bankledger
 * @time 2018/8/23 11:01
 */
public class WalletInfoActivity extends ToolbarBaseActivity {

    private ImageView ivQrWalletInfo;
    private TextView tvWalletName;
    private TextView tvWalletNumber;
    private TextView tvVersionName;
    private int clickCount = 0;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_wallet_info;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.wallet_info);
        setDefaultNavigation();
        tvWalletName = findViewById(R.id.tv_wallet_name);
        tvWalletNumber = findViewById(R.id.tv_wallet_number);
        ivQrWalletInfo = findViewById(R.id.iv_qr_wallet_info);
        tvVersionName = findViewById(R.id.tv_version_name);
    }

    @Override
    public void initData() {
        super.initData();
        loadWalletInfo();
        tvWalletName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (++clickCount >= 8) {
                    showCrashLogDialog();
                    clickCount = 0;
                }
            }
        });
    }

    private void loadWalletInfo() {
        new CommonAsyncTask.Builder<Void, Void, String[]>()
                .setIDoInBackground(new IDoInBackground<Void, Void, String[]>() {
                    @Override
                    public String[] doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        String walletName = WalletInfoManager.getWalletName();
                        String walletNumber = WalletInfoManager.getWalletNumber();
                        String walletVersionName = WalletInfoManager.getCodeWalletVersionName();
                        return new String[]{walletName, walletNumber, walletVersionName};
                    }
                })
                .setIPostExecute(new IPostExecute<String[]>() {
                    @Override
                    public void onPostExecute(String[] strings) {
                        String walletName = strings[0];
                        String walletNumber = strings[1];
                        String walletVersionName = strings[2];
                        tvWalletName.setText(String.format(getString(R.string.wallet_name), walletName));
                        tvWalletNumber.setText(String.format(getString(R.string.wallet_number), walletNumber.substring(walletNumber.length() - 8)));
                        tvVersionName.setText(String.format(getString(R.string.wallet_version_name), walletVersionName));
                        showQrCode(walletName, walletNumber);
                    }
                })
                .start();
    }

    private void showQrCode(final String walletName, final String walletNumber) {
        new CommonAsyncTask.Builder<Void, Void, Bitmap>()
                .setIDoInBackground(new IDoInBackground<Void, Void, Bitmap>() {
                    @Override
                    public Bitmap doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        try {
                            String bluetoothAddress = WalletInfoManager.getBluetoothAddress();
                            String content = QRCodeUtil.encodeWalletInfo(walletName, walletNumber,bluetoothAddress);
                            return QRCodeEncoderUtils.encodeAsBitmap(WalletInfoActivity.this, content);
                        } catch (WriterException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<Bitmap>() {
                    @Override
                    public void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null) {
                            ivQrWalletInfo.setImageBitmap(bitmap);
                        }
                    }
                })
                .start();
    }

    private void showCrashLogDialog() {
        List<File> logFileList = CrashHandler.getInstance().getCrashLogFileList();
        if (logFileList == null || logFileList.size() == 0) {
            ToastUtil.showToast("haven't crash log");
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.recyclerview_list, null);
        RecyclerView rvList = view.findViewById(R.id.rv_list);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL));
        CommonAdapter<File> mAdapter = new CommonAdapter<File>(R.layout.listitem_crash_log) {
            @Override
            protected void convert(ViewHolder viewHolder, File item, int position) {
                TextView tv = viewHolder.findViewById(R.id.tv_file_name);
                tv.setText((position + 1) + "、 " + item.getName());
            }

            @Override
            protected void onItemClick(View view, File item, int position) {
                Bundle args = new Bundle();
                args.putSerializable(Constants.INTENT_KEY1, item);
                go2Activity(CrashLogActivity.class, args);
            }
        };
        rvList.setAdapter(mAdapter);
        mAdapter.addAll(logFileList);
        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.close, null)
                .show();
    }

}
