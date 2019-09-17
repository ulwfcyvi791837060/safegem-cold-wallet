package com.bankledger.safecold.mnemon;

import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.R;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicCode;

import java.io.IOException;
import java.io.InputStream;

public class MnemonicCodeAndroid extends MnemonicCode {
    
    public MnemonicCodeAndroid() throws IOException {
        super();
    }

    @Override
    protected InputStream openEnglishWordList() throws IOException {
        return SafeColdApplication.mContext.getResources().openRawResource(R.raw.mnemonic_wordlist_english);
    }

    @Override
    protected InputStream openChineseWordList() throws IOException {
        return SafeColdApplication.mContext.getResources().openRawResource(R.raw.mnemonic_wordlist_chinese);
    }
}
