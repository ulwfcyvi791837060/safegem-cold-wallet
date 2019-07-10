package com.bankledger.safecold.ui.activity.eos;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bankledger.protobuf.bean.Address;
import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.protobuf.bean.TransMulAddress;
import com.bankledger.protobuf.utils.ProtoUtils;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.ui.activity.QrCodePageActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.bankledger.safecoldj.utils.Utils;
import com.bankledger.utils.AesUtil;
import com.google.zxing.WriterException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zm on 2018/11/13.
 */
public class MineEosAddressDetailActivity extends ToolbarBaseActivity {

    private RecyclerView rvList;
    private CommonAdapter<EosAddressAlias> mAdapter;

    @Override
    protected int setContentLayout() {
        return R.layout.recyclerview_list;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();

        rvList = findViewById(R.id.rv_list);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL));
        mAdapter = new CommonAdapter<EosAddressAlias>(R.layout.listitem_mine_address_detail) {

            @Override
            protected void convert(ViewHolder viewHolder, final EosAddressAlias item, final int position) {
                TextView tvPosition = viewHolder.findViewById(R.id.tv_position);
                tvPosition.setText(Integer.toString(position+1));
                TextView tvAlias = viewHolder.findViewById(R.id.tv_alias);
                tvAlias.setText(item.alias);

                viewHolder.findViewById(R.id.iv_edit_alias).setVisibility(View.GONE);

                viewHolder.findViewById(R.id.tv_amount).setVisibility(View.GONE);

                TextView tvAddress = viewHolder.findViewById(R.id.tv_address);
                tvAddress.setText(item.address);

                //显示地址二维码
                viewHolder.findViewById(R.id.iv_qr_code).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (position == 0) {
                            Map<String, String> params = new HashMap<>();
                            UriDecode uri = new UriDecode(SafeColdSettings.EOS_FULL_NAME, item.address, params);
                            showQrCode(QRCodeUtil.encodeUri(uri));
                        }
                    }
                });

                //监控地址
                viewHolder.findViewById(R.id.tv_monitor).setVisibility(View.GONE);

                //查看私钥
                View tvPrivKey = viewHolder.findViewById(R.id.tv_priv_key);
                tvPrivKey.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
                tvPrivKey.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkPrivKey();
                    }
                });
                viewHolder.findViewById(R.id.tv_delete).setVisibility(View.GONE);
            }

            @Override
            protected void onItemClick(View view, EosAddressAlias item, int position) {

            }
        };
        rvList.setAdapter(mAdapter);
    }

    //查看私钥
    private void checkPrivKey() {
        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, final String content) {
                if (which == Dialog.BUTTON_POSITIVE)
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        checkPrivKey();
                    } else {
                        new CommonAsyncTask.Builder<String, Void, String>()
                                .setIPreExecute(new IPreExecute() {
                                    @Override
                                    public void onPreExecute() {
                                        CommonUtils.showProgressDialog(MineEosAddressDetailActivity.this);
                                    }
                                })
                                .setIDoInBackground(new IDoInBackground<String, Void, String>() {
                                    @Override
                                    public String doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                        EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                                        if(eosAccount.getOpType() == EosAccount.OpType.TYPE_IMPORT){
                                            String encodePrivKey = eosAccount.getActivePrivKey();
                                            byte[] encodeBytes = Utils.hexStringToBytes(encodePrivKey);
                                            byte[] decryptBytes = new byte[0];
                                            try {
                                                decryptBytes = AesUtil.decrypt(Utils.addZeroForNum(content, 16), Utils.addZeroForNum(content, 16), encodeBytes);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            return new String(decryptBytes);
                                        } else {
                                            return HDAddressManager.getInstance().getHDAccount().getEosPrivKey(strings[0]);
                                        }
                                    }
                                })
                                .setIPostExecute(new IPostExecute<String>() {
                                    @Override
                                    public void onPostExecute(String s) {
                                        CommonUtils.dismissProgressDialog();
                                        if (s == null) {
                                            ToastUtil.showToast(R.string.password_error);
                                        } else {
                                            try {
                                                DialogUtil.showImageDialog(MineEosAddressDetailActivity.this, QRCodeEncoderUtils.encodeAsBitmap(MineEosAddressDetailActivity.this, s), s);
                                            } catch (WriterException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }).start(content);
                    }
            }
        });
    }

    private void showQrCode(final String content) {
        new CommonAsyncTask.Builder<String, Void, Bitmap>()
                .setIDoInBackground(new IDoInBackground<String, Void, Bitmap>() {
                    @Override
                    public Bitmap doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        try {
                            return QRCodeEncoderUtils.encodeAsBitmap(MineEosAddressDetailActivity.this, strings[0]);
                        } catch (WriterException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<Bitmap>() {
                    @Override
                    public void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null)
                            DialogUtil.showImageDialog(MineEosAddressDetailActivity.this, bitmap, content);
                    }
                })
                .start(content);
    }

    @Override
    public void initData() {
        super.initData();
        setTitle(R.string.eos_account);
        getETHTokenAddress();
    }

    private void getETHTokenAddress() {
        new CommonAsyncTask.Builder<Void, Void, List<EosAddressAlias>>()
                .setIDoInBackground(new IDoInBackground<Void, Void, List<EosAddressAlias>>() {
                    @Override
                    public List<EosAddressAlias> doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        List<EosAddressAlias> addressAliasList = new ArrayList<>();
                        EosAccount balance = EosAccountProvider.getInstance().queryAvailableEosAccount();

                        EosAddressAlias addressAlias0 = new EosAddressAlias();
                        addressAlias0.address = balance.getAccountName();
                        addressAlias0.alias = "ACCOUNT";
                        addressAliasList.add(addressAlias0);

                        EosAddressAlias addressAlias1 = new EosAddressAlias();
                        addressAlias1.address = balance.getActivePubKey();
                        addressAlias1.alias = "ACTIVE";
                        addressAliasList.add(addressAlias1);

                        EosAddressAlias addressAlias2 = new EosAddressAlias();
                        addressAlias2.address = balance.getOwnerPubKey();
                        addressAlias2.alias = "OWNER";
                        addressAliasList.add(addressAlias2);

                        return addressAliasList;
                    }
                })
                .setIPostExecute(new IPostExecute<List<EosAddressAlias>>() {
                    @Override
                    public void onPostExecute(List<EosAddressAlias> eosAddressAliasList) {
                        mAdapter.replaceAll(eosAddressAliasList);
                    }
                })
                .start();
    }

    class EosAddressAlias {
        String address;
        String alias;
    }
}
