/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bankledger.safecoldj.crypto;


import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.exception.AddressFormatException;
import com.bankledger.safecoldj.exception.PasswordException;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.bankledger.safecoldj.utils.Base58;
import com.bankledger.safecoldj.utils.PrivateKeyUtil;
import com.bankledger.safecoldj.utils.Utils;


public class PasswordSeed {
    private String address;
    private String keyStr;

    public PasswordSeed(String str) {
        String[] arr = QRCodeUtil.split(str);
        this.address = arr[0];
        this.keyStr = arr[1];
    }


    public PasswordSeed(String address, String encryptedKey) {
        this.address = address;
        this.keyStr = encryptedKey;
    }

    public boolean checkPassword(CharSequence password, Currency mCurrency) {
        ECKey ecKey = PrivateKeyUtil.getECKeyFromSingleString(keyStr, password, mCurrency);
        String ecKeyAddress;
        if (ecKey == null) {
            return false;
        } else {
            ecKeyAddress = ecKey.toAddress(mCurrency);
            ecKey.clearPrivateKey();
        }
        return Utils.compareString(this.address,
                ecKeyAddress);

    }

    public boolean changePassword(CharSequence oldPassword, CharSequence newPassword) throws PasswordException {
        keyStr = PrivateKeyUtil.changePassword(keyStr, oldPassword, newPassword);
        return !Utils.isEmpty(keyStr);

    }

    public ECKey getECKey(CharSequence password, Currency mCurrency) {
        return PrivateKeyUtil.getECKeyFromSingleString(keyStr, password, mCurrency);
    }

    public String getAddress() {
        return this.address;
    }

    public String getKeyStr() {
        return this.keyStr;
    }


}
