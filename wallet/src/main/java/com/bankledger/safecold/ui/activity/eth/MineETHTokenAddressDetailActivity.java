package com.bankledger.safecold.ui.activity.eth;

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
import com.bankledger.protobuf.bean.TransMulAddress;
import com.bankledger.protobuf.utils.ProtoUtils;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.ui.activity.QrCodePageActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.EthUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.WriterException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bankledger
 * @time 2018/8/7 16:21
 */
public class MineETHTokenAddressDetailActivity extends ToolbarBaseActivity {

    private ETHToken ethToken;
    private RecyclerView rvList;
    private CommonAdapter<ETHToken> mAdapter;

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
        mAdapter = new CommonAdapter<ETHToken>(R.layout.listitem_mine_address_detail) {

            @Override
            protected void convert(ViewHolder viewHolder, final ETHToken item, int position) {
                TextView tvPosition = viewHolder.findViewById(R.id.tv_position);
                tvPosition.setText(Integer.toString(++position));
                TextView tvAlias = viewHolder.findViewById(R.id.tv_alias);
                if (TextUtils.isEmpty(item.alias)) {
                    tvAlias.setText(R.string.no_alias);
                } else {
                    tvAlias.setText(item.alias);
                }

                //编辑备注
                viewHolder.findViewById(R.id.iv_edit_alias).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        replaceAddressAlias(item);
                    }
                });

                TextView tvAmount = viewHolder.findViewById(R.id.tv_amount);
                tvAmount.setText(EthUtil.formatAmount(item));

                TextView tvAddress = viewHolder.findViewById(R.id.tv_address);
                tvAddress.setText(item.ethAddress);

                //显示地址二维码
                viewHolder.findViewById(R.id.iv_qr_code).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String coin;
                        if (ethToken.isEtc()) {
                            coin = SafeColdSettings.ETC_FULL_NAME;
                        } else {
                            coin = SafeColdSettings.ETH_FULL_NAME;
                        }
                        Map<String, String> params = new HashMap<>();
                        if(item.isErc20()){
                            params.put("token", item.symbol);
                        }
                        UriDecode uri = new UriDecode(coin, item.ethAddress, params);
                        showQrCode(QRCodeUtil.encodeUri(uri));
                    }
                });

                //监控地址
                TextView tvMonitor = viewHolder.findViewById(R.id.tv_monitor);
                if (item.isErc20()) {
                    tvMonitor.setVisibility(View.GONE);
                } else {
                    tvMonitor.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            List<Address> addressList = new ArrayList<>(1);
                            int isToken = ethToken.isEth() ? 1 : ethToken.isErc20() ? 2 : 3;
                            addressList.add(new Address(item.name, item.ethAddress, isToken, item.contractsAddress));
                            String content = ProtoUtils.encodeMonitor(
                                    new TransMulAddress(WalletInfoManager.getWalletNumber(), addressList)
                            );
                            Bundle args = new Bundle();
                            args.putInt(Constants.INTENT_KEY1, QrCodePageActivity.START_TYPE_MONITOR);
                            args.putString(Constants.INTENT_KEY2, content);
                            go2Activity(QrCodePageActivity.class, args);
                        }
                    });
                }

                //查看私钥
                viewHolder.findViewById(R.id.tv_priv_key).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkPrivKey(item);
                    }
                });
                viewHolder.findViewById(R.id.tv_delete).setVisibility(View.GONE);
            }

            @Override
            protected void onItemClick(View view, ETHToken item, int position) {

            }
        };
        rvList.setAdapter(mAdapter);
    }

    //查看私钥
    private void checkPrivKey(final ETHToken ethToken) {
        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, String content) {
                if (which == Dialog.BUTTON_POSITIVE)
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        checkPrivKey(ethToken);
                    } else {
                        new CommonAsyncTask.Builder<String, Void, String>()
                                .setIPreExecute(new IPreExecute() {
                                    @Override
                                    public void onPreExecute() {
                                        CommonUtils.showProgressDialog(MineETHTokenAddressDetailActivity.this);
                                    }
                                })
                                .setIDoInBackground(new IDoInBackground<String, Void, String>() {
                                    @Override
                                    public String doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                        return HDAddressManager.getInstance().getHDAccount().getAddressPrivKey(ethToken, strings[0]);
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
                                                DialogUtil.showImageDialog(MineETHTokenAddressDetailActivity.this, QRCodeEncoderUtils.encodeAsBitmap(MineETHTokenAddressDetailActivity.this, s), s);
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

    private void replaceAddressAlias(final ETHToken ethToken) {
        DialogUtil.showEditDialog(this, R.string.edit_alias, R.string.remark, Constants.ADDRESS_ALIS_LENGTH_MAX, new DialogUtil.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, String content) {
                if (which == Dialog.BUTTON_POSITIVE) {
                    new CommonAsyncTask.Builder<String, Void, Void>()
                            .setIDoInBackground(new IDoInBackground<String, Void, Void>() {
                                @Override
                                public Void doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                    ETHTokenProvider.getInstance().setETHTokenAlias(strings[0], strings[1]);
                                    return null;
                                }
                            })
                            .setIPostExecute(new IPostExecute<Void>() {
                                @Override
                                public void onPostExecute(Void aVoid) {
                                    getETHTokenAddress();
                                }
                            })
                            .start(ethToken.contractsAddress, content);
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
                            return QRCodeEncoderUtils.encodeAsBitmap(MineETHTokenAddressDetailActivity.this, strings[0]);
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
                            DialogUtil.showImageDialog(MineETHTokenAddressDetailActivity.this, bitmap, content);
                    }
                })
                .start(content);
    }

    @Override
    public void initData() {
        super.initData();
        ethToken = (ETHToken) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        setTitle(String.format(getString(R.string.my_address_title), ethToken.isErc20() ? ethToken.symbol : ethToken.name));

        getETHTokenAddress();
    }

    private void getETHTokenAddress() {
        new CommonAsyncTask.Builder<ETHToken, Void, ETHToken>()
                .setIDoInBackground(new IDoInBackground<ETHToken, Void, ETHToken>() {
                    @Override
                    public ETHToken doInBackground(IPublishProgress<Void> publishProgress, ETHToken... ethTokens) {
                        return ETHTokenProvider.getInstance().queryETHTokenWithContractAddress(ethTokens[0].contractsAddress);
                    }
                })
                .setIPostExecute(new IPostExecute<ETHToken>() {
                    @Override
                    public void onPostExecute(ETHToken ethToken) {
                        List<ETHToken> ethTokenList = new ArrayList<>();
                        ethTokenList.add(ethToken);
                        mAdapter.replaceAll(ethTokenList);
                    }
                })
                .start(ethToken);
    }
}
