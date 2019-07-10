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

package com.bankledger.safecoldj.core;

import com.bankledger.safecoldj.crypto.EncryptedData;
import com.bankledger.safecoldj.crypto.KeyCrypterException;
import com.bankledger.safecoldj.crypto.hd.DeterministicKey;
import com.bankledger.safecoldj.crypto.hd.HDKeyDerivation;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicCode;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicException;
import com.bankledger.safecoldj.exception.PasswordException;
import com.bankledger.safecoldj.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractHD {

    protected transient byte[] mnemonicSeed;
    protected transient byte[] hdSeed;
    protected boolean isFromXRandom = true;

    private static final Logger logger = LoggerFactory.getLogger(AbstractHD.class);

    public enum PathType {
        KEY_PATH_BTC(0),
        KEY_PATH_SAFE(1),
        KEY_PATH_LTC(2),
        KEY_PATH_BCH(3),
        KEY_PATH_BTG(4),
        KEY_PATH_DASH(5),
        KEY_PATH_QTUM(6),
        KEY_PATH_ETH(7),
        KEY_PATH_FTO(8),
        KEY_PATH_ETC(9),
        KEY_PATH_BSV(10);

        private int value;

        PathType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static class PathTypeIndex {
        public PathType pathType;
        public int index;
    }

    public static PathType getTernalRootType(int value) {
        switch (value) {
            case 0:
                return PathType.KEY_PATH_BTC;
            case 1:
                return PathType.KEY_PATH_SAFE;
            case 2:
                return PathType.KEY_PATH_LTC;
            case 3:
                return PathType.KEY_PATH_BCH;
            case 4:
                return PathType.KEY_PATH_BTG;
            case 5:
                return PathType.KEY_PATH_DASH;
            case 6:
                return PathType.KEY_PATH_QTUM;
            case 7:
                return PathType.KEY_PATH_ETH;
            case 8:
                return PathType.KEY_PATH_FTO;
            case 9:
                return PathType.KEY_PATH_ETC;
            case 10:
                return PathType.KEY_PATH_BSV;
            default:
                return PathType.KEY_PATH_BTC;
        }
    }

    protected abstract String getEncryptedHDSeed(String userPIN);

    protected abstract String getEncryptedMnemonicSeed(String userPIN);

    protected DeterministicKey getChainRootKey(DeterministicKey accountKey, PathType pathType) {
        return accountKey.deriveSoftened(pathType.getValue());
    }

    protected DeterministicKey getAccount(DeterministicKey master) {
        DeterministicKey purpose = master.deriveHardened(44);
        DeterministicKey coinType = purpose.deriveHardened(0);
        DeterministicKey account = coinType.deriveHardened(0);
        purpose.wipe();
        coinType.wipe();
        return account;
    }


    protected DeterministicKey masterKey(CharSequence password) throws MnemonicException
            .MnemonicLengthException, PasswordException, MnemonicException.MnemonicWordException {
        long begin = System.currentTimeMillis();
        decryptHDSeed(password);
        DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(hdSeed);
        wipeHDSeed();
        logger.info("hd keychain decrypt time: {}", System.currentTimeMillis() - begin);
        return master;
    }

    protected void decryptHDSeed(CharSequence password) throws MnemonicException.MnemonicLengthException, PasswordException, MnemonicException.MnemonicWordException {
        if (password == null) {
            return;
        }
        String encryptedHDSeed = getEncryptedHDSeed(password.toString());
        if (Utils.isEmpty(encryptedHDSeed)) {
            initHDSeedFromMnemonicSeed(password);
        } else {
            hdSeed = new EncryptedData(encryptedHDSeed).decrypt(password);
        }
    }



    public byte[] decryptMnemonicSeed(CharSequence password) throws KeyCrypterException, PasswordException {
        if (password == null) {
            return null;
        }
        String encrypted = getEncryptedMnemonicSeed(password.toString());
        if (!Utils.isEmpty(encrypted)) {
            mnemonicSeed = new EncryptedData(encrypted).decrypt(password);
        }
        return mnemonicSeed;
    }

    private void initHDSeedFromMnemonicSeed(CharSequence password) throws MnemonicException
            .MnemonicLengthException, PasswordException, MnemonicException.MnemonicWordException {
        decryptMnemonicSeed(password);
        hdSeed = seedFromMnemonic(mnemonicSeed);
        wipeMnemonicSeed();
    }

    public List<String> getSeedWords(CharSequence password, int seedType) throws MnemonicException
            .MnemonicLengthException, PasswordException {
        decryptMnemonicSeed(password);
        List<String> words = MnemonicCode.instance().toMnemonic(mnemonicSeed, seedType);
        wipeMnemonicSeed();
        return words;
    }


    protected byte[] getMasterPubKeyExtended(CharSequence password) throws PasswordException {
        try {
            DeterministicKey master = masterKey(password);
            DeterministicKey accountKey = getAccount(master);
            return accountKey.getPubKeyExtended();
        } catch (KeyCrypterException e) {
            throw new PasswordException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void wipeHDSeed() {
        if (hdSeed == null) {
            return;
        }
        Utils.wipeBytes(hdSeed);
    }

    protected void wipeMnemonicSeed() {
        if (mnemonicSeed == null) {
            return;
        }
        Utils.wipeBytes(mnemonicSeed);
    }

    public static final byte[] seedFromMnemonic(byte[] mnemonicSeed) throws MnemonicException
            .MnemonicLengthException, MnemonicException.MnemonicWordException {
        MnemonicCode mnemonic = MnemonicCode.instance();
        return MnemonicCode.toSeed(mnemonic.toMnemonic(mnemonicSeed), "");
    }

}
