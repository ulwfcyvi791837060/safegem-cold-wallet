package com.bankledger.safecold.ui.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.db.ContactsAddressProvider;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.ui.activity.eos.EosSendActivity;
import com.bankledger.safecold.ui.activity.eth.ETHTokenSendActivity;
import com.bankledger.safecold.ui.widget.CommonEditWidget;
import com.bankledger.safecold.ui.widget.EditLengthInputFilter;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.WriterException;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bankledger
 * @time 2018/8/8 17:45
 */
public class ContactsAddressDetailActivity extends ToolbarBaseActivity {
    private CommonEditWidget cewAlias;
    private TextView tvAddress;
    private ImageView ivQrAddress;
    private ContactsAddress contactsAddress;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_contact_address_detail;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        tvAddress = findViewById(R.id.tv_address);

        cewAlias = findViewById(R.id.cew_alias);
        cewAlias.getEditText().setFilters(new InputFilter[]{new EditLengthInputFilter(12)});
        cewAlias.setHint(R.string.remark);

        ivQrAddress = findViewById(R.id.iv_qr_address);

        findViewById(R.id.bt_resend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtils.isRepeatClick()) {
                    if (contactsAddress.isCurrencyAddress()) {
                        Bundle args = new Bundle();
                        args.putSerializable(Constants.INTENT_KEY1, HDAddressManager.getInstance().getCurrencyMap().get(contactsAddress.getCoin()));
                        args.putString(Constants.INTENT_KEY2, contactsAddress.getAddress());
                        go2Activity(CurrencySendActivity.class, args);
                    } else if (contactsAddress.isTokenAddress() || contactsAddress.isETHAddress() || contactsAddress.isETCAddress()) {
                        resendETHToken();
                    } else if (contactsAddress.isEosAddress()) {
                        resendEos();
                    } else if (contactsAddress.isSafeAsset()) {
                        resendSafeAsset();
                    }
                }
            }
        });

        findViewById(R.id.bt_delete_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtil.showTextDialogWithCancelButton(ContactsAddressDetailActivity.this, R.string.delete_contact, R.string.submit_delete_contact, new DialogUtil.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String content) {
                        if (which == Dialog.BUTTON_POSITIVE) {
                            new CommonAsyncTask.Builder<ContactsAddress, Void, Void>()
                                    .setIDoInBackground(new IDoInBackground<ContactsAddress, Void, Void>() {
                                        @Override
                                        public Void doInBackground(IPublishProgress<Void> publishProgress, ContactsAddress... addresses) {
                                            ContactsAddressProvider.getInstance().deleteContactsAddress(addresses[0]);
                                            return null;
                                        }
                                    })
                                    .setIPostExecute(new IPostExecute<Void>() {
                                        @Override
                                        public void onPostExecute(Void aVoid) {
                                            EventBus.getDefault().post(new EventMessage(EventMessage.TYPE_CONTACT_ADDRESS_CHANGED));
                                            setResult(Constants.RESULT_SUCCESS);
                                            finish();
                                        }
                                    })
                                    .start(contactsAddress);
                        }
                    }
                });
            }
        });
    }

    private void resendEos() {
        EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
        if (eosAccount != null) {
            new CommonAsyncTask.Builder<Void, Void, EosBalance>()
                    .setIDoInBackground(new IDoInBackground<Void, Void, EosBalance>() {
                        @Override
                        public EosBalance doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                            return EosUsdtBalanceProvider.getInstance().getEosBalance();
                        }
                    })
                    .setIPostExecute(new IPostExecute<EosBalance>() {
                        @Override
                        public void onPostExecute(EosBalance eosBalance) {
                            Bundle args = new Bundle();
                            args.putSerializable(Constants.INTENT_KEY1, eosBalance);
                            args.putString(Constants.INTENT_KEY2, contactsAddress.getAddress());
                            go2Activity(EosSendActivity.class, args);
                        }
                    }).start();
        } else {
            ToastUtil.showToast(R.string.eos_account_disabled);
        }
    }

    private void resendSafeAsset() {
        new CommonAsyncTask.Builder<Void, Void, SafeAsset>()
                .setIDoInBackground(new IDoInBackground<Void, Void, SafeAsset>() {
                    @Override
                    public SafeAsset doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        return OutProvider.getInstance().getSafeAsset(contactsAddress.getCoin());
                    }
                })
                .setIPostExecute(new IPostExecute<SafeAsset>() {
                    @Override
                    public void onPostExecute(SafeAsset safeAsset) {

                        Bundle args = new Bundle();
                        args.putSerializable(Constants.INTENT_KEY1, HDAddressManager.getInstance().getCurrencyMap().get(SafeColdSettings.SAFE));
                        args.putString(Constants.INTENT_KEY2, contactsAddress.getAddress());
                        args.putSerializable(Constants.INTENT_KEY4, safeAsset);
                        go2Activity(CurrencySendActivity.class, args);
                    }
                }).start();
    }

    private void resendETHToken() {
        new CommonAsyncTask.Builder<String, Void, ETHToken>()
                .setIDoInBackground(new IDoInBackground<String, Void, ETHToken>() {
                    @Override
                    public ETHToken doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        return ETHTokenProvider.getInstance().queryETHTokenWithContractAddress(strings[0]);
                    }
                })
                .setIPostExecute(new IPostExecute<ETHToken>() {
                    @Override
                    public void onPostExecute(ETHToken ethToken) {
                        Bundle args = new Bundle();
                        args.putSerializable(Constants.INTENT_KEY1, ethToken);
                        args.putString(Constants.INTENT_KEY2, contactsAddress.getAddress());
                        go2Activity(ETHTokenSendActivity.class, args);
                    }
                }).start(contactsAddress.getContractAddress());
    }

    @Override
    public void initData() {
        super.initData();
        contactsAddress = (ContactsAddress) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        setTitle(CurrencyNameUtil.getContactsCoinName(contactsAddress));
        tvAddress.setText(contactsAddress.getAddress());
        cewAlias.setText(contactsAddress.getAlias());
        createQrCode();
    }

    private void createQrCode() {
        String fullName = "";
        Map<String, String> params = new HashMap<>();
        params.put("amount", "");
        if (contactsAddress.isCurrencyAddress()) {
            fullName = HDAddressManager.getInstance().getCurrencyCoin(contactsAddress.getCoin()).fullName;
        } else if (contactsAddress.isETHAddress()) {
            fullName = SafeColdSettings.ETH_FULL_NAME;
        } else if (contactsAddress.isETCAddress()) {
            fullName = SafeColdSettings.ETC_FULL_NAME;
        } else if (contactsAddress.isTokenAddress()) {
            fullName = SafeColdSettings.ETH_FULL_NAME;
            params.put("token", contactsAddress.getCoin());
        } else if (contactsAddress.isEosAddress()) {
            fullName = SafeColdSettings.EOS_FULL_NAME;
        } else if (contactsAddress.isEosTokenAddress()) {
            fullName = SafeColdSettings.EOS_FULL_NAME;
            params.put("token", contactsAddress.getCoin());
        } else if (contactsAddress.isSafeAsset()) {
            fullName = SafeColdSettings.SAFE_FULL_NAME;
            params.put("token", contactsAddress.getCoin());
        }
        UriDecode uri = new UriDecode(fullName, contactsAddress.getAddress(), params);
        new CommonAsyncTask.Builder<String, Void, Bitmap>()
                .setIDoInBackground(new IDoInBackground<String, Void, Bitmap>() {
                    @Override
                    public Bitmap doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        try {
                            return QRCodeEncoderUtils.encodeAsBitmap(ContactsAddressDetailActivity.this, strings[0]);
                        } catch (WriterException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<Bitmap>() {
                    @Override
                    public void onPostExecute(Bitmap bitmap) {
                        ivQrAddress.setImageBitmap(bitmap);
                    }
                }).startOnSingleThread(QRCodeUtil.encodeUri(uri));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.complete_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_complete) {
            if (contactsAddress.getAlias().equals(cewAlias.getText())) {
                finish();
            } else {
                new CommonAsyncTask.Builder<ContactsAddress, Void, Void>()
                        .setIDoInBackground(new IDoInBackground<ContactsAddress, Void, Void>() {
                            @Override
                            public Void doInBackground(IPublishProgress<Void> publishProgress, ContactsAddress... contactsAddresses) {
                                ContactsAddressProvider.getInstance().replaceAddressAlias(contactsAddresses[0], cewAlias.getText().replace("\n", " ").replace("\r", " "));
                                return null;
                            }
                        })
                        .setIPostExecute(new IPostExecute<Void>() {
                            @Override
                            public void onPostExecute(Void aVoid) {
                                EventBus.getDefault().post(new EventMessage(EventMessage.TYPE_CONTACT_ADDRESS_CHANGED));
                                setResult(Constants.RESULT_SUCCESS);
                                finish();
                            }
                        })
                        .start(contactsAddress);
            }
        }
        return super.onOptionsItemSelected(item);
    }
}

