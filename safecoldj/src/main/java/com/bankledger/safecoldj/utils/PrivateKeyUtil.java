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

package com.bankledger.safecoldj.utils;

import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.crypto.DumpedPrivateKey;
import com.bankledger.safecoldj.crypto.ECKey;
import com.bankledger.safecoldj.crypto.EncryptedPrivateKey;
import com.bankledger.safecoldj.crypto.KeyCrypter;
import com.bankledger.safecoldj.crypto.KeyCrypterException;
import com.bankledger.safecoldj.crypto.KeyCrypterScrypt;
import com.bankledger.safecoldj.crypto.SecureCharSequence;
import com.bankledger.safecoldj.crypto.bip38.Bip38;
import com.bankledger.safecoldj.exception.AddressFormatException;
import com.bankledger.safecoldj.exception.PasswordException;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.bankledger.safecoldj.qrcode.SaltForQRCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;

public class PrivateKeyUtil {

    private static final Logger log = LoggerFactory.getLogger(PrivateKeyUtil.class);

    public static String getEncryptedString(ECKey ecKey) {
        String salt = "1";
        if (ecKey.getKeyCrypter() instanceof KeyCrypterScrypt) {
            KeyCrypterScrypt scrypt = (KeyCrypterScrypt) ecKey.getKeyCrypter();
            salt = Utils.bytesToHexString(scrypt.getSalt());
        }
        EncryptedPrivateKey key = ecKey.getEncryptedPrivateKey();
        return Utils.bytesToHexString(key.getEncryptedBytes()) + QRCodeUtil.QR_CODE_SPLIT + Utils
                .bytesToHexString(key.getInitialisationVector()) + QRCodeUtil.QR_CODE_SPLIT + salt;
    }

    public static ECKey getECKeyFromSingleString(String str, CharSequence password, Currency mCurrency) {
        try {
            DecryptedECKey decryptedECKey = decryptionECKey(str, password, false, mCurrency);
            if (decryptedECKey != null && decryptedECKey.ecKey != null) {
                return decryptedECKey.ecKey;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static DecryptedECKey decryptionECKey(String str, CharSequence password, boolean needPrivteKeyText, Currency mCurrency) throws Exception {
        String[] strs = QRCodeUtil.split(str);
        if (strs.length != 3) {
            log.error("decryption: PrivateKeyFromString format error");
            return null;
        }
        byte[] temp = Utils.hexStringToBytes(strs[2]);
        if (temp.length != KeyCrypterScrypt.SALT_LENGTH + 1 && temp.length != KeyCrypterScrypt.SALT_LENGTH) {
            log.error("decryption:  salt lenth is {} not {}", temp.length, KeyCrypterScrypt.SALT_LENGTH + 1);
            return null;
        }
        SaltForQRCode saltForQRCode = new SaltForQRCode(temp);
        byte[] salt = saltForQRCode.getSalt();
        boolean isCompressed = saltForQRCode.isCompressed();
        boolean isFromXRandom = saltForQRCode.isFromXRandom();

        KeyCrypterScrypt crypter = new KeyCrypterScrypt(salt);
        EncryptedPrivateKey epk = new EncryptedPrivateKey(Utils.hexStringToBytes
                (strs[1]), Utils.hexStringToBytes(strs[0]));
        byte[] decrypted = crypter.decrypt(epk, crypter.deriveKey(password));
        
        ECKey ecKey = null;
        SecureCharSequence privateKeyText = null;
        if (needPrivteKeyText) {
            DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(decrypted, isCompressed, mCurrency);
            privateKeyText = dumpedPrivateKey.toSecureCharSequence();
            dumpedPrivateKey.clearPrivateKey();
        } else {
            BigInteger bigInteger = new BigInteger(1, decrypted);
            byte[] pub = ECKey.publicKeyFromPrivate(bigInteger, isCompressed);

            ecKey = new ECKey(epk, pub, crypter);
            ecKey.setFromXRandom(isFromXRandom);

        }
        Utils.wipeBytes(decrypted);
        return new DecryptedECKey(ecKey, privateKeyText);
    }

    public static String getBIP38PrivateKeyString(HDAddress address, CharSequence password, Currency mCurrency) throws
            AddressFormatException, InterruptedException {
        SecureCharSequence decrypted = getDecryptPrivateKeyString(address.getAddress()
                , password, mCurrency);
        String bip38 = Bip38.encryptNoEcMultiply(password, decrypted.toString(), mCurrency);
        if (SafeColdSettings.DEV_DEBUG) {
            SecureCharSequence d = Bip38.decrypt(bip38, password, mCurrency);
            if (d.equals(decrypted)) {
                log.info("BIP38 right");
            } else {
                throw new RuntimeException("BIP38 wrong " + d.toString() + " , " +
                        "" + decrypted.toString());
            }
        }
        decrypted.wipe();
        return bip38;
    }

    public static SecureCharSequence getDecryptPrivateKeyString(String privateKey, CharSequence password, Currency mCurrency) {
        try {
            DecryptedECKey decryptedECKey = decryptionECKey(privateKey, password, true, mCurrency);
            if (decryptedECKey != null && decryptedECKey.privateKeyText != null) {
                return decryptedECKey.privateKeyText;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String changePassword(String text, CharSequence oldpassword, CharSequence newPassword) throws PasswordException {
        String[] strs = QRCodeUtil.split(text);
        if (strs.length != 3) {
            log.error("change Password: PrivateKeyFromString format error");
            return null;
        }

        byte[] temp = Utils.hexStringToBytes(strs[2]);
        if (temp.length != KeyCrypterScrypt.SALT_LENGTH + 1 && temp.length != KeyCrypterScrypt.SALT_LENGTH) {
            log.error("decryption:  salt lenth is {} not {}", temp.length, KeyCrypterScrypt.SALT_LENGTH + 1);
            return null;
        }
        byte[] salt = new byte[KeyCrypterScrypt.SALT_LENGTH];
        if (temp.length == KeyCrypterScrypt.SALT_LENGTH) {
            salt = temp;
        } else {
            System.arraycopy(temp, 1, salt, 0, salt.length);
        }
        KeyCrypterScrypt crypter = new KeyCrypterScrypt(salt);
        EncryptedPrivateKey epk = new EncryptedPrivateKey(Utils.hexStringToBytes
                (strs[1]), Utils.hexStringToBytes(strs[0]));

        byte[] decrypted = crypter.decrypt(epk, crypter.deriveKey(oldpassword));
        EncryptedPrivateKey encryptedPrivateKey = crypter.encrypt(decrypted, crypter.deriveKey(newPassword));
        byte[] newDecrypted = crypter.decrypt(encryptedPrivateKey, crypter.deriveKey(newPassword));
        if (!Arrays.equals(decrypted, newDecrypted)) {
            throw new KeyCrypterException("change Password, cannot be successfully decrypted after encryption so aborting wallet encryption.");
        }
        Utils.wipeBytes(decrypted);
        Utils.wipeBytes(newDecrypted);
        return Utils.bytesToHexString(encryptedPrivateKey.getEncryptedBytes())
                + QRCodeUtil.QR_CODE_SPLIT + Utils.bytesToHexString(encryptedPrivateKey.getInitialisationVector())
                + QRCodeUtil.QR_CODE_SPLIT + strs[2];

    }

    /**
     * will release key
     *
     * @param key
     * @param password
     * @return
     */
    public static ECKey encrypt(ECKey key, CharSequence password) {
        KeyCrypter scrypt = new KeyCrypterScrypt();
        KeyParameter derivedKey = scrypt.deriveKey(password);
        ECKey encryptedKey = key.encrypt(scrypt, derivedKey);

        // Check that the encrypted key can be successfully decrypted.
        // This is done as it is a critical failure if the private key cannot be decrypted successfully
        // (all bitcoin controlled by that private key is lost forever).
        // For a correctly constructed keyCrypter the encryption should always be reversible so it is just being as cautious as possible.
        if (!ECKey.encryptionIsReversible(key, encryptedKey, scrypt, derivedKey)) {
            // Abort encryption
            throw new KeyCrypterException("The key " + key.toString() + " cannot be successfully decrypted after encryption so aborting wallet encryption.");
        }
        key.clearPrivateKey();
        return encryptedKey;
    }

    private static class DecryptedECKey {
        public DecryptedECKey(ECKey ecKey, SecureCharSequence privateKeyText) {
            this.ecKey = ecKey;
            this.privateKeyText = privateKeyText;
        }

        public ECKey ecKey;
        public SecureCharSequence privateKeyText;

    }

    public static boolean verifyMessage(String address, String messageText, String signatureText, Currency currency) {
        // Strip CRLF from signature text
        try {
            signatureText = signatureText.replaceAll("\n", "").replaceAll("\r", "");

            ECKey key = ECKey.signedMessageToKey(messageText, signatureText);
            String signAddress = key.toAddress(currency);
            return Utils.compareString(address, signAddress);
        } catch (SignatureException e) {
            e.printStackTrace();
            return false;
        }

    }

    public static String formatEncryptPrivateKeyForDb(String encryptPrivateKey) {
        if (Utils.isEmpty(encryptPrivateKey)) {
            return encryptPrivateKey;
        }
        String[] strs = QRCodeUtil.split(encryptPrivateKey);
        byte[] temp = Utils.hexStringToBytes(strs[2]);
        byte[] salt = new byte[KeyCrypterScrypt.SALT_LENGTH];
        if (temp.length == KeyCrypterScrypt.SALT_LENGTH + 1) {
            System.arraycopy(temp, 1, salt, 0, salt.length);
        } else {
            salt = temp;
        }
        strs[2] = Utils.bytesToHexString(salt);
        return Utils.joinString(strs, QRCodeUtil.QR_CODE_SPLIT);

    }

    public static String getFullencryptPrivateKey(HDAddress address, String encryptPrivKey) {
        String[] strings = QRCodeUtil.split(encryptPrivKey);
        byte[] salt = Utils.hexStringToBytes(strings[2]);
        if (salt.length == KeyCrypterScrypt.SALT_LENGTH) {
            SaltForQRCode saltForQRCode = new SaltForQRCode(salt, false, false);
            strings[2] = Utils.bytesToHexString(saltForQRCode.getQrCodeSalt());
        }
        return Utils.joinString(strings, QRCodeUtil.QR_CODE_SPLIT);
    }

    public static String getFullencryptHDMKeyChain(boolean isFromXRandom, String encryptPrivKey) {
        String[] strings = QRCodeUtil.split(encryptPrivKey);
        byte[] salt = Utils.hexStringToBytes(strings[2]);
        if (salt.length == KeyCrypterScrypt.SALT_LENGTH) {
            SaltForQRCode saltForQRCode = new SaltForQRCode(salt, true, isFromXRandom);
            strings[2] = Utils.bytesToHexString(saltForQRCode.getQrCodeSalt()).toUpperCase();
        }
        return Utils.joinString(strings, QRCodeUtil.QR_CODE_SPLIT);
    }

}
