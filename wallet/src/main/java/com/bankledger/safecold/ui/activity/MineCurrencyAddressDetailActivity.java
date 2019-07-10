package com.bankledger.safecold.ui.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
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
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.HDAddressProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.StringUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.WriterException;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bankledger
 * @time 2018/8/7 16:21
 */
public class MineCurrencyAddressDetailActivity extends ToolbarBaseActivity {

    public static final int START_TYPE_NORMAL = 0;
    public static final int START_TYPE_SELECT = 1;

    private Currency mCurrency;
    private RecyclerView rvList;
    private CommonAdapter<HDAddress> mAdapter;
    private Map<HDAddress, String> hdAddressBalanceMap = new HashMap<>();
    private int startType;

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
        mAdapter = new CommonAdapter<HDAddress>(R.layout.listitem_mine_address_detail) {
            HDAddress selectHDAddress;

            @Override
            protected void convert(ViewHolder viewHolder, final HDAddress item, int position) {
                TextView tvPosition = viewHolder.findViewById(R.id.tv_position);
                tvPosition.setText(Integer.toString(++position));
                TextView tvAlias = viewHolder.findViewById(R.id.tv_alias);
                if (TextUtils.isEmpty(item.getAlias())) {
                    tvAlias.setText(R.string.no_alias);
                } else {
                    tvAlias.setText(item.getAlias());
                }

                //编辑备注
                viewHolder.findViewById(R.id.iv_edit_alias).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        replaceAddressAlias(item);
                    }
                });

                TextView tvAmount = viewHolder.findViewById(R.id.tv_amount);
                if(mCurrency.isUsdt()){
                    String balance = StringUtil.subZeroAndDot(hdAddressBalanceMap.get(item));
                    tvAmount.setText(balance);
                } else {
                    String balance = BigDecimalUtils.formatSatoshi2Btc(hdAddressBalanceMap.get(item));
                    tvAmount.setText(balance);
                }

                TextView tvAddress = viewHolder.findViewById(R.id.tv_address);
                tvAddress.setText(item.getAddress());

                //显示地址二维码
                viewHolder.findViewById(R.id.iv_qr_code).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Map<String, String> params = new HashMap<>();
                        UriDecode uri = new UriDecode(mCurrency.fullName, item.getAddress(), params);
                        showQrCode(QRCodeUtil.encodeUri(uri));
                    }
                });

                //监控地址
                viewHolder.findViewById(R.id.tv_monitor).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        List<Address> addressList = new ArrayList<>(2);
                        if (item.isUsdt()) {
                            addressList.add(new Address(item.getCoin(), item.getAddress(), 6, item.getAddress()));
                        } else {
                            addressList.add(new Address(item.getCoin(), item.getAddress(), 0, item.getAddress()));
                            addressList.add(new Address(item.getCoin(), mCurrency.selectAddress, 0, mCurrency.selectAddress));
                        }
                        String content = ProtoUtils.encodeMonitor(
                                new TransMulAddress(WalletInfoManager.getWalletNumber(), addressList)
                        );
                        Bundle args = new Bundle();
                        args.putInt(Constants.INTENT_KEY1, QrCodePageActivity.START_TYPE_MONITOR);
                        args.putString(Constants.INTENT_KEY2, content);
                        go2Activity(QrCodePageActivity.class, args);
                    }
                });

                //查看私钥
                viewHolder.findViewById(R.id.tv_priv_key).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkPrivKey(item);
                    }
                });

                //删除
                TextView tvDelete = viewHolder.findViewById(R.id.tv_delete);
                if (item.getAddress().equalsIgnoreCase(mCurrency.selectAddress)) {
                    tvDelete.setVisibility(View.GONE);
                    selectHDAddress = item;
                } else {
                    tvDelete.setVisibility(View.VISIBLE);
                    tvDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteAddress(item.getAddress());
                        }
                    });
                }

                if (startType == START_TYPE_SELECT) {
                    viewHolder.findViewById(R.id.ll_content).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent result = new Intent();
                            result.putExtra(Constants.INTENT_KEY1, item);
                            setResult(Constants.RESULT_SUCCESS, result);
                            finish();
                        }
                    });
                }
            }

            @Override
            protected void onItemClick(View view, HDAddress item, int position) {

            }
        };
        rvList.setAdapter(mAdapter);
    }

    //查看私钥
    private void checkPrivKey(final HDAddress hdAddress) {
        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, String content) {
                if (which == Dialog.BUTTON_POSITIVE)
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        checkPrivKey(hdAddress);
                    } else {
                        new CommonAsyncTask.Builder<String, Void, String>()
                                .setIPreExecute(new IPreExecute() {
                                    @Override
                                    public void onPreExecute() {
                                        CommonUtils.showProgressDialog(MineCurrencyAddressDetailActivity.this);
                                    }
                                })
                                .setIDoInBackground(new IDoInBackground<String, Void, String>() {
                                    @Override
                                    public String doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                        return HDAddressManager.getInstance().getHDAccount().getAddressPrivKey(hdAddress, strings[0]);
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
                                                DialogUtil.showImageDialog(MineCurrencyAddressDetailActivity.this, QRCodeEncoderUtils.encodeAsBitmap(MineCurrencyAddressDetailActivity.this, s), s);
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

    private void deleteAddress(String address) {
        new CommonAsyncTask.Builder<String, Void, Void>()
                .setIDoInBackground(new IDoInBackground<String, Void, Void>() {
                    @Override
                    public Void doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        HDAddressProvider.getInstance().setHDAddressAvailable(mCurrency.coin, strings[0], HDAddress.AvailableState.STATE_DISABLED);
                        return null;
                    }
                })
                .setIPostExecute(new IPostExecute<Void>() {
                    @Override
                    public void onPostExecute(Void aVoid) {
                        getCurrencyAddress();
                        EventBus.getDefault().post(new EventMessage(EventMessage.TYPE_MINE_ADDRESS_CHANGED));
                    }
                })
                .start(address);
    }

    private void replaceAddressAlias(final HDAddress address) {
        DialogUtil.showEditDialog(this, R.string.edit_alias, R.string.remark, address.getAlias(), Constants.ADDRESS_ALIS_LENGTH_MAX, new DialogUtil.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, String content) {
                if (which == Dialog.BUTTON_POSITIVE) {
                    content = content.replace("\n", " ").replace("\r", " ");
                    new CommonAsyncTask.Builder<String, Void, Void>()
                            .setIDoInBackground(new IDoInBackground<String, Void, Void>() {
                                @Override
                                public Void doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                    HDAddressProvider.getInstance().replaceAddressAlias(strings[0], strings[1], strings[2]);
                                    return null;
                                }
                            })
                            .setIPostExecute(new IPostExecute<Void>() {
                                @Override
                                public void onPostExecute(Void aVoid) {
                                    getCurrencyAddress();
                                }
                            })
                            .start(mCurrency.coin, address.getAddress(), content);
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
                            return QRCodeEncoderUtils.encodeAsBitmap(MineCurrencyAddressDetailActivity.this, strings[0]);
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
                            DialogUtil.showImageDialog(MineCurrencyAddressDetailActivity.this, bitmap, content);
                    }
                })
                .start(content);
    }

    @Override
    public void initData() {
        super.initData();
        mCurrency = (Currency) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        startType = getIntent().getIntExtra(Constants.INTENT_KEY2, START_TYPE_NORMAL);
        if (startType == START_TYPE_NORMAL) {
            setTitle(String.format(getString(R.string.my_address_title), CurrencyNameUtil.getCurrencyName(mCurrency)));
        } else {
            setTitle(String.format(getString(R.string.my_address_select), CurrencyNameUtil.getCurrencyName(mCurrency)));
        }

        getCurrencyAddress();
    }

    public void getCurrencyAddress() {
        new CommonAsyncTask.Builder<Currency, Void, List<HDAddress>>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(MineCurrencyAddressDetailActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<Currency, Void, List<HDAddress>>() {
                    @Override
                    public List<HDAddress> doInBackground(IPublishProgress<Void> publishProgress, Currency... currency) {
                        return AbstractDb.hdAddressProvider.getHDAddressListWithAvailable(currency[0], HDAddress.AvailableState.STATE_AVAILABLE);
                    }
                })
                .setIPostExecute(new IPostExecute<List<HDAddress>>() {
                    @Override
                    public void onPostExecute(List<HDAddress> hdAddresses) {
                        checkAddressBalance(hdAddresses);
                    }
                })
                .start(mCurrency);
    }

    private void checkAddressBalance(final List<HDAddress> hdAddresses) {
        HDAddress[] arrHDAddress = new HDAddress[hdAddresses.size()];
        hdAddresses.toArray(arrHDAddress);
        new CommonAsyncTask.Builder<HDAddress, Void, Map<HDAddress, String>>()
                .setIDoInBackground(new IDoInBackground<HDAddress, Void, Map<HDAddress, String>>() {
                    @Override
                    public Map<HDAddress, String> doInBackground(IPublishProgress<Void> publishProgress, HDAddress... addresses) {
                        Map<HDAddress, String> addressMap = new HashMap<>(addresses.length);
                        for (HDAddress hdAddress : addresses) {
                            if(mCurrency.isUsdt()){
                                addressMap.put(hdAddress, EosUsdtBalanceProvider.getInstance().getUsdtBalance());
                            } else {
                                addressMap.put(hdAddress, Long.toString(OutProvider.getInstance().getBalanceWithAddress(hdAddress.getAddress())));
                            }
                        }
                        return addressMap;
                    }
                })
                .setIPostExecute(new IPostExecute<Map<HDAddress, String>>() {
                    @Override
                    public void onPostExecute(Map<HDAddress, String> addressMap) {
                        CommonUtils.dismissProgressDialog();
                        hdAddressBalanceMap.clear();
                        hdAddressBalanceMap.putAll(addressMap);
                        mAdapter.replaceAll(hdAddresses);
                    }
                })
                .start(arrHDAddress);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (startType == START_TYPE_NORMAL && !mCurrency.isUsdt()) {
            getMenuInflater().inflate(R.menu.add_menu, menu);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            if (CommonUtils.isRepeatClick()) {
                if (mAdapter.getItemCount() < SafeColdSettings.MAX_HDADDRESS_COUNT) {
                    createAddress();
                } else {
                    ToastUtil.showToast(String.format(getString(R.string.max_hdaddress_count), SafeColdSettings.MAX_HDADDRESS_COUNT));
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void createAddress() {
        DialogUtil.showEditDialog(this, R.string.add_address, R.string.remark, Constants.ADDRESS_ALIS_LENGTH_MAX, new DialogUtil.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, String content) {
                if (which == Dialog.BUTTON_POSITIVE) {
                    if (mAdapter.getItemCount() >= SafeColdSettings.MAX_HDADDRESS_COUNT) {
                        ToastUtil.showToast(String.format(getString(R.string.max_hdaddress_count), SafeColdSettings.MAX_HDADDRESS_COUNT));
                        return;
                    }
                    content = content.replace("\n", " ");
                    content = content.replace("\r", " ");
                    new CommonAsyncTask.Builder<String, Void, Void>()
                            .setIPreExecute(new IPreExecute() {
                                @Override
                                public void onPreExecute() {
                                    CommonUtils.showProgressDialog(MineCurrencyAddressDetailActivity.this);
                                }
                            })
                            .setIDoInBackground(new IDoInBackground<String, Void, Void>() {
                                @Override
                                public Void doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                    HDAddressProvider hdAddressProvider = HDAddressProvider.getInstance();
                                    List<HDAddress> disabledHDAddresses = hdAddressProvider.getHDAddressListWithAvailable(mCurrency, HDAddress.AvailableState.STATE_DISABLED);
                                    if (disabledHDAddresses.size() == 0) {
                                        HDAddressManager.getInstance().getHDAccount().createHDAddress(mCurrency, strings[0]);
                                    } else {
                                        hdAddressProvider.setHDAddressAvailable(mCurrency.coin, disabledHDAddresses.get(0).getAddress(), HDAddress.AvailableState.STATE_AVAILABLE);
                                        hdAddressProvider.replaceAddressAlias(mCurrency.coin, disabledHDAddresses.get(0).getAddress(), strings[0]);
                                    }
                                    return null;
                                }
                            })
                            .setIPostExecute(new IPostExecute<Void>() {
                                @Override
                                public void onPostExecute(Void aVoid) {
                                    CommonUtils.dismissProgressDialog();
                                    EventBus.getDefault().post(new EventMessage(EventMessage.TYPE_MINE_ADDRESS_CHANGED));
                                    getCurrencyAddress();
                                }
                            })
                            .start(content);
                }
            }
        });
    }
}
