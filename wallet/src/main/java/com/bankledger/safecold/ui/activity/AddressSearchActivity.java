package com.bankledger.safecold.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.db.ContactsAddressProvider;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.HDAddressProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.SpanUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.entity.Address;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.WriterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 地址搜索
 * @author bankledger
 * @time 2018/8/22 16:44
 */
public class AddressSearchActivity extends BaseActivity {
    private SearchView svSearch;
    private RecyclerView rvList;
    private CommonAdapter<Address> mAdapter;

    private ArrayList<Address> addressList = new ArrayList<>();

    private String matchStr;
    private View includeNotData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_search);
    }

    @Override
    public void initView() {
        super.initView();

        includeNotData = findViewById(R.id.include_not_data);

        findViewById(R.id.tv_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        svSearch = findViewById(R.id.sv_search);
        //去除searchView默认下划线
        View plateView = svSearch.findViewById(android.support.v7.appcompat.R.id.search_plate);
        if (plateView != null) {
            plateView.setBackgroundColor(Color.TRANSPARENT);
        }
        TextView textView = svSearch.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        textView.setTextSize(14);
        svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (svSearch != null) {
                    InputMethodManager imm = (InputMethodManager) AddressSearchActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(svSearch.getWindowToken(), 0);
                        svSearch.clearFocus();
                    }
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    includeNotData.setVisibility(View.VISIBLE);
                    rvList.setVisibility(View.GONE);
                    addressList.clear();
                    mAdapter.notifyView();
                } else {
                    search(newText);
                    matchStr = newText;
                }
                return true;
            }
        });

        rvList = findViewById(R.id.rv_list);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL));
        mAdapter = new CommonAdapter<Address>(-1, addressList) {

            @Override
            public int getItemViewType(int position) {
                Address address = data.get(position);
                if (position == 0) {
                    return address.isContactsAddress() ? 2 : 0;
                } else {
                    if (!data.get(position - 1).isContactsAddress() && address.isContactsAddress()) {
                        return 2;
                    } else {
                        return 1;
                    }
                }
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                int layoutResource = viewType == 1 ? R.layout.listitem_find_address : R.layout.listitem_find_address_with_head;
                View view = LayoutInflater.from(parent.getContext()).inflate(layoutResource, parent, false);
                return new ViewHolder(view);
            }

            @Override
            protected void convert(ViewHolder viewHolder, final Address item, int position) {
                int itemType = getItemViewType(position);
                if (itemType == 0) {
                    TextView tvExplain = viewHolder.findViewById(R.id.tv_explain);
                    tvExplain.setText(R.string.my_address);
                    tvExplain.setOnClickListener(null);
                } else if (itemType == 2) {
                    TextView tvExplain = viewHolder.findViewById(R.id.tv_explain);
                    tvExplain.setText(R.string.contact_address);
                    tvExplain.setOnClickListener(null);
                }

                ImageView ivCurrencyIcon = viewHolder.findViewById(R.id.iv_currency_icon);
                TextView tvCurrencyName = viewHolder.findViewById(R.id.tv_currency_name);
                TextView tvAlias = viewHolder.findViewById(R.id.tv_address_count);
                TextView tvSelectAddress = viewHolder.findViewById(R.id.tv_select_address);

                String coin = "";
                String address = "";
                if (item.isHDAddress()) {
                    coin = item.hdAddress.getCoin();
                    address = item.hdAddress.getAddress();
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(coin, ConvertViewBean.TYPE_CURRENCY));
                    tvCurrencyName.setText(CurrencyNameUtil.getCurrencyName(item.hdAddress));
                    tvAlias.setText(item.hdAddress.getAlias());
                    tvSelectAddress.setText(address);
                } else if (item.isETHToken()) {
                    coin = item.ethToken.isErc20() ? SafeColdSettings.ETH : item.ethToken.name;
                    address = item.ethToken.ethAddress;
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(CurrencyNameUtil.getEthTokenName(item.ethToken), ConvertViewBean.TYPE_ETH_TOKEN));
                    tvCurrencyName.setText(CurrencyNameUtil.getEthTokenName(item.ethToken));
                    tvAlias.setText(item.ethToken.alias);
                    tvSelectAddress.setText(address);
                } else if (item.isEos()) {
                    coin = SafeColdSettings.EOS;
                    address = item.eosAccount.getAccountName();
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(coin, ConvertViewBean.TYPE_EOS_COIN));
                    tvCurrencyName.setText(coin);
                    tvSelectAddress.setText(address);
                } else if (item.isContactsAddress()) {
                    int coinType;
                    if (item.contactsAddress.isCurrencyAddress()) {
                        coin = item.contactsAddress.getCoin();
                        coinType = ConvertViewBean.TYPE_CURRENCY;
                    } else if (item.contactsAddress.isETHAddress() || item.contactsAddress.isETCAddress() || item.contactsAddress.isTokenAddress()) {
                        coin = item.contactsAddress.getCoin();
                        coinType = ConvertViewBean.TYPE_ETH_TOKEN;
                    } else if (item.contactsAddress.isEosAddress()) {
                        coin = item.contactsAddress.getCoin();
                        coinType = ConvertViewBean.TYPE_EOS_COIN;
                    } else {
                        coin = item.contactsAddress.getCoin();
                        coinType = ConvertViewBean.TYPE_SAFE_ASSET;
                    }
                    address = item.contactsAddress.getAddress();
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(item.contactsAddress.getCoin(), coinType));
                    tvCurrencyName.setText(CurrencyNameUtil.getContactsCoinName(item.contactsAddress));
                    tvAlias.setText(item.contactsAddress.getAlias());
                    tvSelectAddress.setText(address);
                }

                //显示地址二维码
                final String finalCoin = coin;
                final String finalAddress = address;
                viewHolder.findViewById(R.id.iv_qr_code).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StringBuffer sbAddress = new StringBuffer(finalCoin.toLowerCase());
                        sbAddress.append(QRCodeUtil.QR_CODE_COLON);
                        sbAddress.append(finalAddress);
                        showQrCode(sbAddress.toString());
                    }
                });

                SpanUtil.setMatchSpan(matchStr, tvCurrencyName, tvAlias, tvSelectAddress);
            }

            @Override
            protected void onItemClick(View view, Address item, int position) {
                if (item.isContactsAddress()) {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, item.contactsAddress);
                    go2ActivityForResult(ContactsAddressDetailActivity.class, Constants.REQUEST_CODE1, args);
                }
            }
        };
        rvList.setAdapter(mAdapter);
        includeNotData.setVisibility(View.VISIBLE);
        rvList.setVisibility(View.GONE);
    }

    private void showQrCode(final String content) {
        new CommonAsyncTask.Builder<String, Void, Bitmap>()
                .setIDoInBackground(new IDoInBackground<String, Void, Bitmap>() {
                    @Override
                    public Bitmap doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        try {
                            return QRCodeEncoderUtils.encodeAsBitmap(AddressSearchActivity.this, strings[0]);
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
                            DialogUtil.showImageDialog(AddressSearchActivity.this, bitmap, content);
                    }
                })
                .start(content);
    }

    private void search(String text) {
        new CommonAsyncTask.Builder<String, Void, List<Address>>()
                .setIDoInBackground(new IDoInBackground<String, Void, List<Address>>() {
                    @Override
                    public List<Address> doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        List<Address> matchAddress = new ArrayList<>();
                        List<HDAddress> hdAddressList = HDAddressProvider.getInstance().matchHDAddress(strings[0]);
                        for (int i = 0; i < hdAddressList.size(); i++) {
                            matchAddress.add(Address.convertHDAddress(hdAddressList.get(i)));
                        }

                        List<ETHToken> eths = ETHTokenProvider.getInstance().matchETH(strings[0]);
                        if (eths != null) {
                            for (ETHToken token : eths) {
                                matchAddress.add(Address.convertETHToken(token));
                            }
                        }

                        List<ETHToken> tokenList = ETHTokenProvider.getInstance().matchToken(strings[0]);
                        for (int i = 0; i < tokenList.size(); i++) {
                            matchAddress.add(Address.convertETHToken(tokenList.get(i)));
                        }

                        EosAccount account = EosAccountProvider.getInstance().matchEos(strings[0]);
                        if (account != null) {
                            matchAddress.add(Address.convertEosAccount(account));
                        }
                        Collections.sort(matchAddress);
                        List<ContactsAddress> contactsAddress = ContactsAddressProvider.getInstance().matchAddress(strings[0]);
                        Collections.sort(contactsAddress);
                        for (int i = 0; i < contactsAddress.size(); i++) {
                            matchAddress.add(Address.convertContactsAddress(contactsAddress.get(i)));
                        }

                        return matchAddress;
                    }
                })
                .setIPostExecute(new IPostExecute<List<Address>>() {
                    @Override
                    public void onPostExecute(List<Address> list) {
                        includeNotData.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
                        rvList.setVisibility(list.size() == 0 ? View.GONE : View.VISIBLE);
                        addressList.clear();
                        addressList.addAll(list);
                        mAdapter.notifyView();
                    }
                })
                .start(text);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE1 && resultCode == Constants.RESULT_SUCCESS) {
            search(matchStr);
        }
    }
}
