package com.bankledger.safecold.ui.activity;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.google.zxing.WriterException;

/**
 * @author bankledger
 * @time 2018/7/30 20:57
 */
public class MyPublicKeyActivity extends ToolbarBaseActivity {
    private ImageView ivPublicKey;
    private TextView tvPublicKey;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_my_public_key;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.my_public_key);
        setDefaultNavigation();

        ivPublicKey = findViewById(R.id.iv_public_key);
        tvPublicKey = findViewById(R.id.tv_public_key);
    }

    @Override
    public void initData() {
        super.initData();
        String key = getIntent().getStringExtra(Constants.INTENT_KEY1);
        tvPublicKey.setText(key);
        try {
            Bitmap bm = QRCodeEncoderUtils.encodeAsBitmap(this, key);
            ivPublicKey.setImageBitmap(bm);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
