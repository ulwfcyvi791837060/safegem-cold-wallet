package com.bankledger.safecold.ui.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.google.zxing.WriterException;

/**
 * Created by zm on 2018/7/4.
 */

public class QrCodeFragment extends BaseFragment {

    private ImageView mImgQrcode;
    private Bitmap bitmap;

    @Override
    public int setContentView() {
        return R.layout.fragment_qr_code;
    }

    @Override
    public void initView() {
        mImgQrcode = findViewById(R.id.img_qrcode);
    }

    @Override
    public void initData() {
        Bundle args = getArguments();
        String hexTx = args.getString(Constants.INTENT_KEY1);
        try {
            bitmap = QRCodeEncoderUtils.encodeAsBitmap(getContext(), hexTx);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        if (bitmap != null)
            mImgQrcode.setImageBitmap(bitmap);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }
}
