package com.bankledger.safecold.ui.activity;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.ui.widget.CommonTextWidget;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.WriterException;

/**
 * 帮助
 * @author bankledger
 * @time 2018/7/25 11:19
 */
public class HelpActivity extends ToolbarBaseActivity {

    private ImageView ivQrCode;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_help;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.help);

        CommonTextWidget ctwWebsite = findViewById(R.id.ctw_website);
        ctwWebsite.setLeftText(R.string.official_website);
        ctwWebsite.setLeftImageResource(R.mipmap.official_website);
        ctwWebsite.setRightHintText(Constants.OFFICIAL_WEBSITE);

        CommonTextWidget ctwWechat = findViewById(R.id.ctw_wechat);
        ctwWechat.setLeftText(R.string.official_wechat);
        ctwWechat.setLeftImageResource(R.mipmap.official_wechat);
        ctwWechat.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwWechat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtil.showImageDialog(HelpActivity.this, R.mipmap.qr_wechat, null);
            }
        });

        ivQrCode = findViewById(R.id.iv_qr_code);
    }

    @Override
    public void initData() {
        super.initData();
        try {
            Bitmap bitmap = QRCodeEncoderUtils.encodeAsBitmap(this, Constants.HOT_WALLET_DOWNLOAD_URL);
            ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
