/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.bankledger.safecoldj.core;

import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.Safe;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicCode;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.exception.AddressFormatException;
import com.bankledger.safecoldj.exception.NotSufficientFundsException;
import com.bankledger.safecoldj.exception.PasswordException;
import com.bankledger.safecoldj.exception.SaveChipException;
import com.bankledger.safecoldj.exception.TxBuilderException;

import com.bankledger.safecoldj.crypto.ECKey;
import com.bankledger.safecoldj.crypto.EncryptedData;
import com.bankledger.safecoldj.crypto.TransactionSignature;
import com.bankledger.safecoldj.crypto.hd.DeterministicKey;
import com.bankledger.safecoldj.crypto.hd.HDKeyDerivation;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicException;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.script.Script;
import com.bankledger.safecoldj.script.ScriptBuilder;
import com.bankledger.safecoldj.utils.SafeUtils;
import com.bankledger.safecoldj.utils.Utils;
import com.bankledger.utils.AesUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.tx.ChainId;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.eblock.eos4j.Ecc;
import io.eblock.eos4j.OfflineSign;
import io.eblock.eos4j.api.vo.SignParam;
import io.eblock.eos4j.ecc.EccTool;


/**
 * Created by zm on 15/6/19.
 */
public class HDAccount extends AbstractHD implements Cloneable {

    private int addressIndex = 0;
    private final byte[] lock = new byte[0];
    public static final Logger logger = LoggerFactory.getLogger(HDAccount.class);

    public HDAccount(byte[] mnemonicSeed, CharSequence password, boolean isFromXRandom)
            throws MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, CipherException, SaveChipException {
        this.mnemonicSeed = mnemonicSeed;
        hdSeed = seedFromMnemonic(mnemonicSeed);
        DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(hdSeed);
        EncryptedData encryptedHDSeed = new EncryptedData(hdSeed, password, isFromXRandom);
        EncryptedData encryptedMnemonicSeed = new EncryptedData(mnemonicSeed, password, isFromXRandom);
        ECKey k = new ECKey(mnemonicSeed, null);
        k.clearPrivateKey();
        DeterministicKey accountKey = getAccount(master);

        List<HDAddress> hdAddressList = new ArrayList<>();

        List<Currency> currencyList = HDAddressManager.getInstance().getCurrencyList();
        for (int i = 0; i < currencyList.size(); i++) {
            DeterministicKey rootKey = getChainRootKey(accountKey, currencyList.get(i).pathType);
            for (int j = 0; j < SafeColdSettings.MAX_HDADDRESS_COUNT; j++) {
                DeterministicKey addressKey = rootKey.deriveSoftened(j);
                String address = addressKey.toAddress(currencyList.get(i));
                if (j == 0) {
                    currencyList.get(i).selectAddress = address;
                }
                byte[] pubKey = Arrays.copyOf(addressKey.getPubKey(), addressKey.getPubKey().length);
                hdAddressList.add(new HDAddress(address, pubKey, currencyList.get(i).pathType, j, currencyList.get(i).coin, ""));
                addressKey.wipe();
                if (currencyList.get(i).isUsdt()) {
                    AbstractDb.eosUsdtBalanceProvider.addBalance(EosUsdtBalance.getZeroUsdtBalance());
                    break;
                }
            }
            AbstractDb.coinRootKeyProvider.addRootKey(currencyList.get(i).coin, rootKey.getPubKeyExtended());
            rootKey.wipe();
        }
        AbstractDb.hdAddressProvider.addHDAddress(hdAddressList);

        //ETH
        DeterministicKey ethRootKey = getChainRootKey(accountKey, PathType.KEY_PATH_ETH).deriveSoftened(addressIndex);
        ECKeyPair ethKeyPair = ECKeyPair.create(ethRootKey.getPrivKeyBytes());
        WalletFile ethWalletFile = Wallet.createLight(password.toString(), ethKeyPair);
        String ethAddress = "0x" + ethWalletFile.getAddress();
        AbstractDb.ethTokenProvider.addETH(ETHToken.newEth(ethAddress));
        ethRootKey.wipe();

        //ETC
        DeterministicKey etcRootKey = getChainRootKey(accountKey, PathType.KEY_PATH_ETC).deriveSoftened(addressIndex);
        ECKeyPair etcKeyPair = ECKeyPair.create(etcRootKey.getPrivKeyBytes());
        WalletFile etcWalletFile = Wallet.createLight(password.toString(), etcKeyPair);
        String etcAddress = "0x" + etcWalletFile.getAddress();
        AbstractDb.ethTokenProvider.addETH(ETHToken.newEtc(etcAddress));
        etcRootKey.wipe();

        //EOS
        String privkey = EccTool.seedPrivate(accountKey.getPrivKeyBytes());
        String pubKey = Ecc.privateToPublic(privkey);
        EosAccount account = new EosAccount();
        account.setOwnerPubKey(pubKey);
        account.setActivePubKey(pubKey);
        account.setAccountName("");
        account.setOwnerPrivKey("");
        account.setActivePrivKey("");
        account.setOpType(EosAccount.OpType.TYPE_CREATE);
        account.setState(EosAccount.AvailableState.STATE_DISABLED);
        AbstractDb.eosAccountProvider.addEosAccount(account);
        AbstractDb.eosUsdtBalanceProvider.addBalance(EosUsdtBalance.getZeroEosBalance());

        accountKey.wipe();
        master.wipe();
        wipeHDSeed();
        wipeMnemonicSeed();
        long ret = AbstractDb.hdAccountProvider.addHDAccount(encryptedMnemonicSeed.toEncryptedString(), encryptedHDSeed.toEncryptedString(), password.toString());
        if (ret != 0) {
            throw new SaveChipException(ret);
        }
    }

    public HDAccount(byte[] mnemonicSeed, CharSequence password) throws MnemonicException
            .MnemonicLengthException, MnemonicException.MnemonicWordException, CipherException, SaveChipException {
        this(mnemonicSeed, password, false);
    }

    public HDAccount(SecureRandom random, CharSequence password) throws MnemonicException
            .MnemonicLengthException, MnemonicException.MnemonicWordException, CipherException, SaveChipException {
        this(randomByteFromSecureRandom(random, 16), password, random.getClass().getCanonicalName
                ().indexOf("XRandom") >= 0);
    }

    public HDAccount(EncryptedData encryptedMnemonicSeed, CharSequence password) throws
            MnemonicException.MnemonicLengthException, PasswordException, MnemonicException.MnemonicWordException, CipherException, SaveChipException {
        this(encryptedMnemonicSeed.decrypt(password), password, encryptedMnemonicSeed.isXRandom());
    }

    public HDAccount(CharSequence password) {
        for (Currency item : HDAddressManager.getInstance().getCurrencyList()) {
            List<HDAddress> hdAddressList = AbstractDb.hdAddressProvider.getHDAddressList(item);
            if (hdAddressList.size() > 0) {
                item.selectAddress = hdAddressList.get(0).getAddress();
            } else {
                try { //加比特币系列的币种，没有初始化地址的情况
                    decryptHDSeed(password);
                    DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(hdSeed);
                    DeterministicKey accountKey = getAccount(master);
                    DeterministicKey rootKey = getChainRootKey(accountKey, item.pathType);
                    for (int j = 0; j < SafeColdSettings.MAX_HDADDRESS_COUNT; j++) {
                        DeterministicKey addressKey = rootKey.deriveSoftened(j);
                        String address = addressKey.toAddress(item);
                        if (j == 0) {
                            item.selectAddress = address;
                        }
                        byte[] pubKey = Arrays.copyOf(addressKey.getPubKey(), addressKey.getPubKey().length);
                        hdAddressList.add(new HDAddress(address, pubKey, item.pathType, j, item.coin, ""));
                        addressKey.wipe();
                        if (item.isUsdt()) {
                            AbstractDb.eosUsdtBalanceProvider.addBalance(EosUsdtBalance.getZeroUsdtBalance());
                            break;
                        }
                    }
                    AbstractDb.coinRootKeyProvider.addRootKey(item.coin, rootKey.getPubKeyExtended());
                    rootKey.wipe();
                    AbstractDb.hdAddressProvider.addHDAddress(hdAddressList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (AbstractDb.eosAccountProvider.queryCreateEosAccount() == null) {
            try { //加EOS币种，没有初始化公钥的情况
                decryptHDSeed(password);
                DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(hdSeed);
                DeterministicKey accountKey = getAccount(master);
                String privkey = EccTool.seedPrivate(accountKey.getPrivKeyBytes());
                String pubKey = Ecc.privateToPublic(privkey);
                EosAccount account = new EosAccount();
                account.setOwnerPubKey(pubKey);
                account.setActivePubKey(pubKey);
                account.setAccountName("");
                account.setOwnerPrivKey("");
                account.setActivePrivKey("");
                account.setOpType(EosAccount.OpType.TYPE_CREATE);
                account.setState(EosAccount.AvailableState.STATE_DISABLED);
                AbstractDb.eosAccountProvider.addEosAccount(account);
                AbstractDb.eosUsdtBalanceProvider.addBalance(EosUsdtBalance.getZeroEosBalance());

                accountKey.wipe();
                master.wipe();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public HDAccount() {
        for (Currency item : HDAddressManager.getInstance().getCurrencyList()) {
            List<HDAddress> hdAddressList = AbstractDb.hdAddressProvider.getHDAddressList(item);
            if (hdAddressList.size() > 0) {
                item.selectAddress = hdAddressList.get(0).getAddress();
            }
        }
    }

    public static byte[] randomSeedByte(SecureRandom random) {
        return randomByteFromSecureRandom(random, 16);
    }

    public static byte[] randomSeedByte() {
        return randomSeedByte(new SecureRandom());
    }

    public static List<String> seedByte2Seed(byte[] seedByte, int seedType) {
        try {
            return MnemonicCode.instance().toMnemonic(seedByte, seedType);
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getAccountPubKey(String password) {
        try {
            decryptHDSeed(password);
            DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(hdSeed);
            String key = Utils.bytesToHexString(master.getPubKey());
            wipeHDSeed();
            master.wipe();
            return key;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean checkWithPassword(CharSequence password) {
        try {
            decryptHDSeed(password);
            decryptMnemonicSeed(password);
            byte[] hdBytes = Arrays.copyOf(hdSeed, hdSeed.length);
            byte[] msBytes = seedFromMnemonic(mnemonicSeed);
            boolean mnemonicSeedSafe = Arrays.equals(msBytes, hdBytes);
            Utils.wipeBytes(hdBytes);
            wipeHDSeed();
            wipeMnemonicSeed();
            return mnemonicSeedSafe;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public long resetPassword(String oldPassword, String newPassword) throws PasswordException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, Exception {
        decryptHDSeed(oldPassword);
        decryptMnemonicSeed(oldPassword);
        resetEosPriv(oldPassword, newPassword);
        byte[] hdCopy = Arrays.copyOf(hdSeed, hdSeed.length);
        if (Arrays.equals(seedFromMnemonic(mnemonicSeed), hdCopy)) {
            EncryptedData encryptedHDSeed = new EncryptedData(hdSeed, newPassword, isFromXRandom);
            EncryptedData encryptedMnemonicSeed = new EncryptedData(mnemonicSeed, newPassword, isFromXRandom);
            long ret = AbstractDb.hdAccountProvider.replaceEncryptedSeed(
                    encryptedMnemonicSeed.toEncryptedString(),
                    encryptedHDSeed.toEncryptedString(), newPassword);
            Utils.wipeBytes(hdCopy);
            wipeHDSeed();
            wipeMnemonicSeed();
            return ret;
        } else {
            Utils.wipeBytes(hdCopy);
            wipeHDSeed();
            wipeMnemonicSeed();
            throw new PasswordException("old password error");
        }
    }

    public void resetEosPriv(String oldPassword, String newPassword) throws PasswordException, Exception {
        EosAccount account = AbstractDb.eosAccountProvider.queryAvailableEosAccount();
        if (account != null && account.opType == EosAccount.OpType.TYPE_IMPORT) {
            byte[] activeBytes = Utils.hexStringToBytes(account.getActivePrivKey());
            byte[] activeDecryptBytes = AesUtil.decrypt(Utils.addZeroForNum(oldPassword, 16), Utils.addZeroForNum(oldPassword, 16), activeBytes);
            String activePrivKey = new String(activeDecryptBytes); //active私钥

            byte[] ownerBytes = Utils.hexStringToBytes(account.getOwnerPrivKey());
            byte[] ownerDecryptBytes = AesUtil.decrypt(Utils.addZeroForNum(oldPassword, 16), Utils.addZeroForNum(oldPassword, 16), ownerBytes);
            String ownerPrivKey = new String(ownerDecryptBytes); //owner私钥

            String activePubKey = EccTool.privateToPublic(activePrivKey);
            String ownerPubKey = EccTool.privateToPublic(ownerPrivKey);

            if (activePubKey.equals(account.getActivePubKey()) && ownerPubKey.equals(account.getOwnerPubKey())) {
                byte[] newOwnerBytes = AesUtil.encrypt(Utils.addZeroForNum(newPassword, 16), Utils.addZeroForNum(newPassword, 16), ownerPrivKey.getBytes());
                byte[] newActiveBytes = AesUtil.encrypt(Utils.addZeroForNum(newPassword, 16), Utils.addZeroForNum(newPassword, 16), activePrivKey.getBytes());
                account.setOwnerPrivKey(Utils.bytesToHexString(newOwnerBytes));
                account.setActivePrivKey(Utils.bytesToHexString(newActiveBytes));
                AbstractDb.eosAccountProvider.addOrUpdateEosAccount(account);
            } else {
                throw new PasswordException("old password error");
            }
        }
    }

    public String getAddressPrivKey(HDAddress hdAddress, String password) {
        try {
            decryptHDSeed(password);
        } catch (Exception e) {
            return null;
        }
        DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(hdSeed);
        DeterministicKey accountKey = getAccount(master);
        DeterministicKey rootKey = getChainRootKey(accountKey, HDAddressManager.getInstance().getCurrencyMap().get(hdAddress.getCoin()).pathType);
        DeterministicKey addressKey = rootKey.deriveSoftened(hdAddress.getAddressIndex());
        byte[] privKeyBytes = addressKey.getPrivKeyBytes();
        addressKey.wipe();
        rootKey.wipe();
        accountKey.wipe();
        master.wipe();
        return privKeyBytes == null ? null : Utils.bytesToHexString(privKeyBytes);
    }

    public String getAddressPrivKey(ETHToken ethToken, String password) {
        PathType type;
        if (ethToken.isEtc()) {
            type = PathType.KEY_PATH_ETC;
        } else if (ethToken.isEth() || ethToken.isErc20()) {
            type = PathType.KEY_PATH_ETH;
        } else {
            return null;
        }
        try {
            decryptHDSeed(password);
        } catch (Exception e) {
            return null;
        }
        DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(hdSeed);
        DeterministicKey accountKey = getAccount(master);
        DeterministicKey rootKey = getChainRootKey(accountKey, type);
        DeterministicKey addressKey = rootKey.deriveSoftened(0);
        byte[] privKeyBytes = addressKey.getPrivKeyBytes();
        addressKey.wipe();
        rootKey.wipe();
        accountKey.wipe();
        master.wipe();
        return privKeyBytes == null ? null : Utils.bytesToHexString(privKeyBytes);
    }

    public String getEosPrivKey(String password) {
        try {
            decryptHDSeed(password);
        } catch (Exception e) {
            return null;
        }
        DeterministicKey master = HDKeyDerivation.createMasterPrivateKey(hdSeed);
        DeterministicKey accountKey = getAccount(master);
        String privkey = EccTool.seedPrivate(accountKey.getPrivKeyBytes());
        accountKey.wipe();
        master.wipe();
        return privkey;
    }

    @Override
    protected String getEncryptedHDSeed(String userPIN) {
        return AbstractDb.hdAccountProvider.getHDAccountEncryptSeed(userPIN);
    }

    @Override
    protected String getEncryptedMnemonicSeed(String userPIN) {
        return AbstractDb.hdAccountProvider.getHDAccountEncryptMnemonicSeed(userPIN);
    }

    private static byte[] randomByteFromSecureRandom(SecureRandom random, int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public byte[] accountPubExtended(CharSequence password) throws MnemonicException
            .MnemonicLengthException, PasswordException, MnemonicException.MnemonicWordException {
        DeterministicKey master = masterKey(password);
        DeterministicKey account = getAccount(master);
        byte[] extended = account.getPubKeyExtended();
        master.wipe();
        account.wipe();
        return extended;
    }

    public String xPubB58(CharSequence password) throws MnemonicException
            .MnemonicLengthException, PasswordException, MnemonicException.MnemonicWordException {
        DeterministicKey master = masterKey(password);
        DeterministicKey purpose = master.deriveHardened(44);
        DeterministicKey coinType = purpose.deriveHardened(0);
        DeterministicKey account = coinType.deriveHardened(0);
        String xpub = account.serializePubB58();
        master.wipe();
        purpose.wipe();
        coinType.wipe();
        account.wipe();
        return xpub;
    }

    @Override
    public Object clone() {
        HDAccount obj = null;
        try {
            obj = (HDAccount) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public Tx newTx(String[] toAddresses, Long[] amounts, Currency mCurrency, long transFee, boolean isUsdt) throws TxBuilderException, AddressFormatException {
        return newTx(null, toAddresses, amounts, mCurrency, transFee, isUsdt);
    }

    public Tx newTx(Script simpleSendScript, String[] toAddresses, Long[] amounts, CharSequence password, Currency mCurrency, long transFee, boolean isUsdt) throws
            TxBuilderException, MnemonicException.MnemonicLengthException, PasswordException, AddressFormatException, MnemonicException.MnemonicWordException {
        Tx tx = newTx(simpleSendScript, toAddresses, amounts, mCurrency, transFee, isUsdt);

        List<HDAddress> signingAddresses = AbstractDb.hdAddressProvider.getHDAddressesForInputs(tx.getIns(), mCurrency.coin);

        assert signingAddresses.size() == tx.getIns().size();

        DeterministicKey master = masterKey(password);
        if (master == null) {
            return null;
        }

        DeterministicKey accountKey = getAccount(master);

        DeterministicKey rootKey = getChainRootKey(accountKey, mCurrency.pathType);
        accountKey.wipe();
        master.wipe();

        List<Out> outList = AbstractDb.outProvider.getUnspendOutsWithCoin(mCurrency.coin);
        logger.info("-------- outList = {} ", outList);

        List<byte[]> unsignedHashes = tx.getUnsignedInHashes();
        assert unsignedHashes.size() == signingAddresses.size();
        ArrayList<byte[]> signatures = new ArrayList<>();
        HashMap<String, DeterministicKey> addressToKeyMap = new HashMap<>(signingAddresses.size());
        for (int i = 0; i < signingAddresses.size(); i++) {
            HDAddress address = signingAddresses.get(i);
            logger.info("-------- address = {} ", address);
            byte[] unsigned = unsignedHashes.get(i);
            if (!addressToKeyMap.containsKey(address.getAddress())) {
                addressToKeyMap.put(address.getAddress(), rootKey.deriveSoftened(address.getAddressIndex()));
                logger.info("-------- address = {}, index = {}", address.getAddress(), address.getAddressIndex());
            }
            DeterministicKey key = addressToKeyMap.get(address.getAddress());
            logger.info("-------- pubkey = {} -------- address = {}", Utils.bytesToHexString(key.getPubKey()), key.toAddress(mCurrency));
            assert key != null;
            if (mCurrency.isBchOrBsv()) {
                TransactionSignature signature = new TransactionSignature(key.sign(unsigned),
                        TransactionSignature.SigHash.ALL, false, true);
                signatures.add(ScriptBuilder.createInputScript(signature, key).getProgram());
            } else if (mCurrency.isBtg()) {
                TransactionSignature signature = new TransactionSignature(key.sign(unsigned),
                        TransactionSignature.SigHash.ALL_FORKID, false);
                signatures.add(ScriptBuilder.createInputScript(signature, key).getProgram());
            } else {
                TransactionSignature signature = new TransactionSignature(key.sign(unsigned),
                        TransactionSignature.SigHash.ALL, false);
                signatures.add(ScriptBuilder.createInputScript(signature, key).getProgram());
            }
        }
        tx.signWithSignatures(signatures);
        assert tx.verifySignatures();
        rootKey.wipe();
        for (DeterministicKey key : addressToKeyMap.values()) {
            key.wipe();
        }

        for (In in : tx.getIns()) {
            AbstractDb.outProvider.labelOutSpend(in.getPrevTxHashToHex(), in.getPrevOutSn());
        }

        for (Out out : tx.getOuts()) {
            if (out.getOutAddress() != null && out.getOutAddress().equalsIgnoreCase(mCurrency.selectAddress)) {
                AbstractDb.outProvider.addOut(out);
            }
        }
        return tx;
    }

    public Tx newTx(String[] toAddresses, Long[] amounts, CharSequence password, Currency mCurrency, long transFee) throws
            TxBuilderException, MnemonicException.MnemonicLengthException, PasswordException, AddressFormatException, MnemonicException.MnemonicWordException {
        return newTx(null, toAddresses, amounts, password, mCurrency, transFee, false);
    }

    public Tx newTx(Script simpleSendScript, String[] toAddresses, Long[] amounts, Currency mCurrency, long transFee, boolean isUdst) throws TxBuilderException, AddressFormatException {
        List<Out> outs = AbstractDb.outProvider.getUnspendOutsWithCoin(mCurrency.coin);
        if(isUdst){
            List<Out> newOuts = new ArrayList<>();
            for (Out item : outs) {
                if (item.getOutAddress().equals(mCurrency.selectAddress)) {
                    newOuts.add(item);
                }
            }
            outs = newOuts;
        }
        //这里将找零地址换为币种首地址
        Tx tx = TxBuilder.getInstance().buildTxFromAllAddress(simpleSendScript, outs, mCurrency.selectAddress,
                Arrays.asList(amounts), Arrays.asList(toAddresses), mCurrency, transFee);
        logger.info("------ finish tx = {}", tx);
        return tx;
    }

    public Tx newUsdtTx(String toAddress, long amount, long transFee, CharSequence password) throws AddressFormatException, TxBuilderException, PasswordException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, NotSufficientFundsException {
        BigDecimal b1 = new BigDecimal(amount);
        BigDecimal b2 = new BigDecimal(AbstractDb.eosUsdtBalanceProvider.getUsdtBalance()).multiply(new BigDecimal(100000000));
        BigDecimal sub = b2.subtract(b1);
        if (sub.doubleValue() < 0) {
            throw new NotSufficientFundsException("usdt not sufficient funds");
        }
        byte[] completeSimpleSend = OmniSimpleSendBuilder.completeSimpleSend(amount);
        Script simpleSendScript = ScriptBuilder.createOpReturnScript(completeSimpleSend);
        Currency currency = HDAddressManager.getInstance().getCurrencyMap().get(SafeColdSettings.BTC);
        Tx tx = newTx(simpleSendScript, new String[]{toAddress}, new Long[]{Tx.getMinNondustOutput(currency)}, password, currency, transFee, true);
        AbstractDb.eosUsdtBalanceProvider.updateUsdtBalance(sub.divide(new BigDecimal(100000000)).toPlainString());
        return tx;
    }

    public Tx newSafeAssetTx(SafeAsset safeAsset, String toAddress, long amount, Currency mCurrency, long transFee) throws AddressFormatException, TxBuilderException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, NotSufficientFundsException {

        //检查地址是否可用
        Script simpleSendScript = ScriptBuilder.createOutputScript(toAddress, mCurrency);

        Tx tx = new Tx();
        tx.setCurrency(mCurrency);

        List<Out> outs = AbstractDb.outProvider.getUnspendOutsWithCoinSafeAsset(SafeColdSettings.SAFE, safeAsset.assetId);
        List<Out> selectOuts = selectOuts(outs, amount);

        for (Out out : selectOuts) {
            tx.addInput(out);
        }

        long total = TxBuilder.getAmount(selectOuts);

        //---------发送资产---------
        Safe.CommonData transferData = SafeUtils.getTransferProtos(amount, safeAsset.assetId);
        try {
            String assetTransferReserve = SafeUtils.serialReserve(SafeUtils.CMD_TRANSFER, SafeUtils.SAFE_APP_ID, transferData.toByteArray());
            Out out = new Out(tx, amount, toAddress, mCurrency);
            out.setUnLockHeight(0);
            out.setReserve(assetTransferReserve);
            tx.addOutput(out);
        } catch (Exception e) {
            return null;
        }
        logger.info("-------- tx = {} ", tx);

        if (total > amount) {

            //---------处理资产找零---------
            long changeValue = total - amount;
            Safe.CommonData changeData = SafeUtils.getAssetChangeProtos(changeValue, safeAsset.assetId);
            try {
                String assetChangeReserve = SafeUtils.serialReserve(SafeUtils.CMD_ASSET_CHANGE, SafeUtils.SAFE_APP_ID, changeData.toByteArray());
                Out out = new Out(tx, changeValue, mCurrency.selectAddress, mCurrency);
                out.setUnLockHeight(0);
                out.setReserve(assetChangeReserve);
                tx.addOutput(out);
            } catch (Exception e) {
                return null;
            }
            logger.info("-------- tx = {} ", tx);
        } else if (total < amount) {
            throw new NotSufficientFundsException("asset not sufficient funds");
        }

        Long addAssetFee = 0L;
        Long assetSize = 0L;
        for (Out out : tx.getOuts()) {
            byte[] mBytes = out.getReserveBytes();
            int length = mBytes.length;
            long minFee = 10000; //最低费用
            if (length > 300) {
                int count = length / 300;
                if (length % 300 > 0) {
                    count++;
                }
                minFee = minFee * count;
            }
            addAssetFee += minFee;
            assetSize += length;
        }

        logger.info("----assetSize = {}, addAssetFee = {}", assetSize, addAssetFee);

        addAssetFee += assetSize;

        logger.info("----addAssetFee = {}", addAssetFee);

        List<Out> unspendOuts = AbstractDb.outProvider.getUnspendOutsWithCoin(mCurrency.coin);

        //---------生成交易---------
        Tx newTx = TxBuilder.getInstance().buildTxFromTx(simpleSendScript, mCurrency.selectAddress, unspendOuts, addAssetFee, tx, transFee);

        logger.info("-------- newTx = {} ", newTx);

        return newTx;
    }

    public Tx newSafeAssetTx(SafeAsset safeAsset, String toAddress, long amount, Currency mCurrency, long transFee, CharSequence password) throws AddressFormatException, TxBuilderException, PasswordException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, NotSufficientFundsException {

        Tx newTx = newSafeAssetTx(safeAsset, toAddress, amount, mCurrency, transFee);

        List<HDAddress> signingAddresses = AbstractDb.hdAddressProvider.getHDAddressesForInputs(newTx.getIns(), mCurrency.coin);

        assert signingAddresses.size() == newTx.getIns().size();

        DeterministicKey master = masterKey(password);
        if (master == null) {
            return null;
        }

        DeterministicKey accountKey = getAccount(master);

        DeterministicKey rootKey = getChainRootKey(accountKey, mCurrency.pathType);
        accountKey.wipe();
        master.wipe();

        List<Out> outList = AbstractDb.outProvider.getUnspendOutsWithCoin(mCurrency.coin);
        logger.info("-------- outList = {} ", outList);

        List<byte[]> unsignedHashes = newTx.getUnsignedInHashes();
        assert unsignedHashes.size() == signingAddresses.size();
        ArrayList<byte[]> signatures = new ArrayList<>();
        HashMap<String, DeterministicKey> addressToKeyMap = new HashMap<>(signingAddresses.size());
        for (int i = 0; i < signingAddresses.size(); i++) {
            HDAddress address = signingAddresses.get(i);
            logger.info("-------- address = {} ", address);
            byte[] unsigned = unsignedHashes.get(i);
            if (!addressToKeyMap.containsKey(address.getAddress())) {
                addressToKeyMap.put(address.getAddress(), rootKey.deriveSoftened(address.getAddressIndex()));
                logger.info("-------- address = {}, index = {}", address.getAddress(), address.getAddressIndex());
            }
            DeterministicKey key = addressToKeyMap.get(address.getAddress());
            logger.info("-------- pubkey = {} -------- address = {}", Utils.bytesToHexString(key.getPubKey()), key.toAddress(mCurrency));
            assert key != null;
            if (mCurrency.isBchOrBsv()) {
                TransactionSignature signature = new TransactionSignature(key.sign(unsigned),
                        TransactionSignature.SigHash.ALL, false, true);
                signatures.add(ScriptBuilder.createInputScript(signature, key).getProgram());
            } else if (mCurrency.isBtg()) {
                TransactionSignature signature = new TransactionSignature(key.sign(unsigned),
                        TransactionSignature.SigHash.ALL_FORKID, false);
                signatures.add(ScriptBuilder.createInputScript(signature, key).getProgram());
            } else {
                TransactionSignature signature = new TransactionSignature(key.sign(unsigned),
                        TransactionSignature.SigHash.ALL, false);
                signatures.add(ScriptBuilder.createInputScript(signature, key).getProgram());
            }
        }
        newTx.signWithSignatures(signatures);
        assert newTx.verifySignatures();
        rootKey.wipe();
        for (DeterministicKey key : addressToKeyMap.values()) {
            key.wipe();
        }

        for (In in : newTx.getIns()) {
            AbstractDb.outProvider.labelOutSpend(in.getPrevTxHashToHex(), in.getPrevOutSn());
        }

        for (Out out : newTx.getOuts()) {
            if (out.getOutAddress() != null && out.getOutAddress().equalsIgnoreCase(mCurrency.selectAddress)) {
                if (out.getReserve().equals("safe")) {
                    AbstractDb.outProvider.addOut(out);
                } else {
                    AbstractDb.outProvider.addOut(out, safeAsset);
                }
            }
        }

        logger.info("--------sign newTx = {} ", newTx);

        return newTx;
    }

    private List<Out> selectOuts(List<Out> outs, long amount) {
        List<Out> result = new ArrayList<Out>();
        long sum = 0;
        for (Out out : outs) {
            sum += out.getOutValue();
            result.add(out);
            if (sum >= amount) {
                break;
            }
        }
        return result;
    }

    public String createHDAddress(Currency mCurrency, String alias) {
        synchronized (lock) {
            int addressIndex;
            DeterministicKey rootKey = getRootKey(mCurrency.coin);
            addressIndex = AbstractDb.hdAddressProvider.getAddressCount(mCurrency.pathType, mCurrency.coin);
            DeterministicKey addressKey = rootKey.deriveSoftened(addressIndex);
            String address = addressKey.toAddress(mCurrency);
            List<HDAddress> addressList = new ArrayList<>();
            addressList.add(new HDAddress(address, addressKey.getPubKey(), mCurrency.pathType, addressIndex, mCurrency.coin, alias));
            AbstractDb.hdAddressProvider.addHDAddress(addressList);
            rootKey.wipe();
            addressKey.wipe();
            return address;
        }
    }

    private DeterministicKey getRootKey(String coin) {
        byte[] keyByte = AbstractDb.coinRootKeyProvider.getRootKey(coin);
        return HDKeyDerivation.createMasterPubKeyFromExtendedBytes(keyByte);
    }

    /**
     * 消息签名
     *
     * @param hdAddress
     * @param message
     * @param password
     * @return
     * @throws PasswordException
     * @throws MnemonicException.MnemonicLengthException
     * @throws MnemonicException.MnemonicWordException
     */
    public String messageSign(HDAddress hdAddress, String message, String password) throws PasswordException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        DeterministicKey master = masterKey(password);
        if (master == null) {
            return null;
        }
        DeterministicKey accountKey = getAccount(master);
        Currency currency = HDAddressManager.getInstance().getCurrencyMap().get(hdAddress.getCoin());
        DeterministicKey rootKey = getChainRootKey(accountKey, currency.pathType);
        accountKey.wipe();
        master.wipe();
        DeterministicKey key = rootKey.deriveSoftened(hdAddress.getAddressIndex());
        rootKey.wipe();
        String sign = key.signMessage(message, hdAddress.getCoin());
        key.wipe();
        return sign;
    }

    public boolean verifyMessage(Currency currency, String address, String messageText, String signatureText) {
        // Strip CRLF from signature text
        try {
            signatureText = signatureText.replaceAll("\n", "").replaceAll("\r", "");
            ECKey key = ECKey.signedMessageToKey(messageText, signatureText, currency.coin);
            String signAddress = key.toAddress(currency);
            return Utils.compareString(address, signAddress);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 签名交易
     */
    public String signRawTx(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String toAddress,
                            BigInteger amount, String data, CharSequence password, ETHToken token) throws PasswordException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException {
        byte[] signedMessage;
        RawTransaction rawTransaction;
        if (token.isErc20()) {
            rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    toAddress,
                    data);
        } else {
            rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    toAddress,
                    amount,
                    data);
        }

        DeterministicKey master = masterKey(password);
        if (master == null) {
            return null;
        }
        DeterministicKey accountKey = getAccount(master);
        DeterministicKey rootKey;
        if (token.isEtc()) {
            rootKey = getChainRootKey(accountKey, PathType.KEY_PATH_ETC).deriveSoftened(addressIndex);
        } else {
            rootKey = getChainRootKey(accountKey, PathType.KEY_PATH_ETH).deriveSoftened(addressIndex);
        }
        ECKeyPair ecKeyPair = ECKeyPair.create(rootKey.getPrivKeyBytes());
        Credentials credentials = Credentials.create(ecKeyPair);
        if (token.isEtc()) {
            if (SafeColdSettings.DEV_DEBUG) {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, ChainId.ETHEREUM_CLASSIC_TESTNET, credentials);
            } else {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, ChainId.ETHEREUM_CLASSIC_MAINNET, credentials);
            }
        } else {
            if (SafeColdSettings.DEV_DEBUG) {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, ChainId.KOVAN, credentials);
            } else {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            }
        }
        accountKey.wipe();
        master.wipe();
        rootKey.wipe();
        return Numeric.toHexString(signedMessage);
    }

    public String signRawTx(CharSequence password, SignParam params, String from, String to, String amount, String memo) throws Exception {
        EosAccount account = AbstractDb.eosAccountProvider.queryAvailableEosAccount();
        String privkey = null;
        if (account.opType == EosAccount.OpType.TYPE_CREATE) {
            DeterministicKey master = masterKey(password);
            if (master == null) {
                return null;
            }
            DeterministicKey accountKey = getAccount(master);
            if (accountKey == null || accountKey.getPrivKeyBytes() == null) {
                return null;
            }
            privkey = EccTool.seedPrivate(accountKey.getPrivKeyBytes());

        } else if (account.opType == EosAccount.OpType.TYPE_IMPORT) {
            String encodePrivKey = account.getActivePrivKey();
            byte[] encodeBytes = Utils.hexStringToBytes(encodePrivKey);
            byte[] decryptBytes = AesUtil.decrypt(Utils.addZeroForNum(password.toString(), 16), Utils.addZeroForNum(password.toString(), 16), encodeBytes);
            privkey = new String(decryptBytes);

        }
        if (privkey == null) {
            return null;
        }
        // 离线签名
        OfflineSign sign = new OfflineSign();
        // 交易信息
        return sign.transfer(params, privkey, "eosio.token", from, to, amount, memo);
    }

    public String buyRam(CharSequence password, SignParam params, String payer, String receiver, long buyRam) throws Exception {
        EosAccount account = AbstractDb.eosAccountProvider.queryAvailableEosAccount();
        String privkey = null;
        if (account.opType == EosAccount.OpType.TYPE_CREATE) {
            DeterministicKey master = masterKey(password);
            if (master == null) {
                return null;
            }
            DeterministicKey accountKey = getAccount(master);
            if (accountKey == null || accountKey.getPrivKeyBytes() == null) {
                return null;
            }
            privkey = EccTool.seedPrivate(accountKey.getPrivKeyBytes());

        } else if (account.opType == EosAccount.OpType.TYPE_IMPORT) {
            String encodePrivKey = account.getActivePrivKey();
            byte[] encodeBytes = Utils.hexStringToBytes(encodePrivKey);
            byte[] decryptBytes = AesUtil.decrypt(Utils.addZeroForNum(password.toString(), 16), Utils.addZeroForNum(password.toString(), 16), encodeBytes);
            privkey = new String(decryptBytes);
        }
        if (privkey == null) {
            return null;
        }
        // 离线签名
        OfflineSign sign = new OfflineSign();
        // 交易信息
        return sign.buyRam(params, privkey, payer, receiver, buyRam);
    }

    public String delegatebw(CharSequence password, SignParam params, String payer, String receiver, String stakeNetQuantity, String stakeCpuQuantity, Long transfer) throws Exception {
        EosAccount account = AbstractDb.eosAccountProvider.queryAvailableEosAccount();
        String privkey = null;
        if (account.opType == EosAccount.OpType.TYPE_CREATE) {
            DeterministicKey master = masterKey(password);
            if (master == null) {
                return null;
            }
            DeterministicKey accountKey = getAccount(master);
            if (accountKey == null || accountKey.getPrivKeyBytes() == null) {
                return null;
            }
            privkey = EccTool.seedPrivate(accountKey.getPrivKeyBytes());

        } else if (account.opType == EosAccount.OpType.TYPE_IMPORT) {
            String encodePrivKey = account.getActivePrivKey();
            byte[] encodeBytes = Utils.hexStringToBytes(encodePrivKey);
            byte[] decryptBytes = AesUtil.decrypt(Utils.addZeroForNum(password.toString(), 16), Utils.addZeroForNum(password.toString(), 16), encodeBytes);
            privkey = new String(decryptBytes);
        }
        if (privkey == null) {
            return null;
        }
        // 离线签名
        OfflineSign sign = new OfflineSign();
        // 交易信息
        return sign.delegatebw(params, privkey, payer, receiver, stakeNetQuantity, stakeCpuQuantity, transfer);
    }

}
