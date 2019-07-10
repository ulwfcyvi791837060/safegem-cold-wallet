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

import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.exception.AddressFormatException;
import com.bankledger.safecoldj.exception.TxBuilderException;
import com.bankledger.safecoldj.script.Script;
import com.bankledger.safecoldj.script.ScriptBuilder;
import com.bankledger.safecoldj.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TxBuilder {

    final static Logger log = LoggerFactory.getLogger(TxBuilder.class);

    private static TxBuilder uniqueInstance = new TxBuilder();

    protected static long TX_FREE_MIN_PRIORITY = 57600000L;

    private TxBuilderProtocol emptyWallet = new TxBuilderEmptyWallet();

    private List<TxBuilderProtocol> txBuilders = new ArrayList<TxBuilderProtocol>();

    TxBuilder() {
        txBuilders.add(new TxBuilderDefault());
    }

    public static TxBuilder getInstance() {
        return uniqueInstance;
    }

    public Tx buildTxFromTx(Script simpleSendScript, String changeAddress, List<Out> unspendOuts, Long addAssetFee, Tx prepareTx, long transFee) throws TxBuilderException, AddressFormatException {
        boolean mayMaxTxSize = false;
        List<Tx> txs = new ArrayList<Tx>();
        for (TxBuilderProtocol builder : this.txBuilders) {
            Tx tx = builder.buildTx(simpleSendScript, changeAddress, unspendOuts, addAssetFee, prepareTx, transFee);
            // note: need all unspent out is pay-to-pubkey-hash
            if (tx != null && TxBuilder.estimationTxSize(tx.getIns().size(), tx.getOuts().size()) <= SafeColdSettings.MAX_TX_SIZE) {
                txs.add(tx);
            } else if (tx != null) {
                mayMaxTxSize = true;
            }
        }
        if (txs.size() > 0) {
            return txs.get(0);
        } else if (mayMaxTxSize) {
            throw new TxBuilderException(TxBuilderException.ERR_REACH_MAX_TX_SIZE_LIMIT_CODE);
        } else {
            throw new TxBuilderException();
        }
    }

    public Tx buildTxFromAllAddress(Script simpleSendScript, List<Out> unspendOuts, String changeAddress, List<Long> amounts, List<String> addresses, Currency mCurrency, long transFee) throws TxBuilderException, AddressFormatException {

        List<String> addressList = new ArrayList<>(addresses.size());
        if (mCurrency.isLtc()) { //莱特币地址P2SH地址转换
            for (String item : addresses) {
                if (Utils.getAddressHeader(item) == 50) {
                    item = Utils.addressToP2SHAddress(item, mCurrency);
                }
                addressList.add(item);
            }
        } else {
            addressList.addAll(addresses);
        }

        long value = 0;
        for (long amount : amounts) {
            value += amount;
        }

        if (value > getAmount(unspendOuts)) {
            throw new TxBuilderException.TxBuilderNotEnoughMoneyException(value - TxBuilder.getAmount(unspendOuts));
        }


        Tx emptyWalletTx;
        if (mCurrency.isQtum()) {
            emptyWalletTx = emptyWallet.buildTx(ScriptBuilder.createOutputScript(changeAddress, mCurrency), changeAddress, unspendOuts, prepareTx(simpleSendScript, amounts,
                    addressList, mCurrency), transFee, false);
        } else {
            emptyWalletTx = emptyWallet.buildTx(changeAddress, unspendOuts, prepareTx(simpleSendScript, amounts,
                    addressList, mCurrency), transFee);
        }
        if (emptyWalletTx != null && TxBuilder.estimationTxSize(emptyWalletTx.getIns().size(),
                emptyWalletTx.getOuts().size()) <= SafeColdSettings.MAX_TX_SIZE) {
            return emptyWalletTx;
        } else if (emptyWalletTx != null) {
            throw new TxBuilderException(TxBuilderException.ERR_REACH_MAX_TX_SIZE_LIMIT_CODE);
        }

        for (long amount : amounts) {
            if (amount < Tx.getMinNondustOutput(mCurrency)) {
                throw new TxBuilderException(TxBuilderException.ERR_TX_DUST_OUT_CODE);
            }
        }

        boolean mayMaxTxSize = false;
        List<Tx> txs = new ArrayList<Tx>();
        for (TxBuilderProtocol builder : this.txBuilders) {
            Tx tx;
            if (mCurrency.isQtum()) {
                tx = builder.buildTx(ScriptBuilder.createOutputScript(changeAddress, mCurrency), changeAddress, unspendOuts, prepareTx(simpleSendScript, amounts, addressList, mCurrency), transFee, false);
            } else {
                tx = builder.buildTx(changeAddress, unspendOuts, prepareTx(simpleSendScript, amounts, addressList, mCurrency), transFee);
            }
            // note: need all unspent out is pay-to-pubkey-hash
            if (tx != null && TxBuilder.estimationTxSize(tx.getIns().size(), tx.getOuts().size()) <= SafeColdSettings.MAX_TX_SIZE) {
                txs.add(tx);
            } else if (tx != null) {
                mayMaxTxSize = true;
            }
        }

        if (txs.size() > 0) {
            return txs.get(0);
        } else if (mayMaxTxSize) {
            throw new TxBuilderException(TxBuilderException.ERR_REACH_MAX_TX_SIZE_LIMIT_CODE);
        } else {
            throw new TxBuilderException();
        }
    }

    static Tx prepareTx(Script simpleSendScript, List<Long> amounts, List<String> addresses, Currency mCurrency) throws AddressFormatException {
        Tx tx = new Tx();
        tx.setCurrency(mCurrency);
        if (simpleSendScript != null) {
            Out out = new Out(tx, 0, simpleSendScript.getProgram());
            out.setCurrency(mCurrency);
            tx.addOutput(out);
        }
        for (int i = 0; i < amounts.size(); i++) {
            tx.addOutput(amounts.get(i), addresses.get(i), mCurrency);
        }
        return tx;
    }

    static int estimationTxSize(int inCount, int outCount) {
        return 10 + 149 * inCount + 34 * outCount;
    }

    static int estimationTxSize(int inCount, Script scriptPubKey, List<Out> outs, boolean isCompressed) {
        int size = 8 + 2;

        Script redeemScript = null;
        if (scriptPubKey.isMultiSigRedeem()) {
            redeemScript = scriptPubKey;
            scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        }

        int sigScriptSize = scriptPubKey.getNumberOfBytesRequiredToSpend(isCompressed, redeemScript);
        size += inCount * (32 + 4 + 1 + sigScriptSize + 4);

        for (Out out : outs) {
            size += 8 + 1 + out.getOutScript().length;
        }
        return size;
    }

    static boolean needMinFee(List<Out> amounts) {
        return true;
    }

    static long getAmount(List<Out> outs) {
        long amount = 0;
        for (Out out : outs) {
            amount += out.getOutValue();
        }
        return amount;
    }

    static long getCoinDepth(List<Out> outs) {
        long depth = 0;
        for (Out out : outs) {

        }
        return depth;
    }
}

interface TxBuilderProtocol {

    public Tx buildTx(Script scriptPubKey, String changeAddress, List<Out> unspendOuts, Tx tx, long transFee, boolean isCompressed) throws AddressFormatException;

    public Tx buildTx(String changeAddress, List<Out> unspendOuts, Tx tx, long transFee) throws AddressFormatException;

    public Tx buildTx(Script scriptPubKey, String changeAddress, List<Out> unspendOuts, Long safeAssetFee, Tx tx, long transFee) throws AddressFormatException;
}

class TxBuilderEmptyWallet implements TxBuilderProtocol {

    final Logger log = LoggerFactory.getLogger(TxBuilderEmptyWallet.class);

    public Tx buildTx(Script scriptPubKey, String changeAddress, List<Out> unspendOuts, Tx tx, long transFee, boolean isCompressed) throws AddressFormatException {

        List<Out> outs = unspendOuts;

        long value = 0;
        for (Out out : tx.getOuts()) {
            value += out.getOutValue();
        }
        boolean needMinFee = TxBuilder.needMinFee(tx.getOuts());
        log.info("-------transFee = {}, needMinFee = {}", transFee, needMinFee);

        if (value != TxBuilder.getAmount(outs)) {
            return null;
        }

        long fees = 0;
        if (needMinFee) {
            fees = transFee;
            log.info("-------default fees = {}", fees);
        } else {
            // no fee logic
            int s = TxBuilder.estimationTxSize(outs.size(), scriptPubKey, tx.getOuts(), isCompressed);
            if (TxBuilder.getCoinDepth(outs) <= TxBuilder.TX_FREE_MIN_PRIORITY * s) {
                fees = transFee;
                log.info("-------s fees = {}, {}", s, fees);
            }
        }

        log.info("-------new tx = {}", tx);
        int size = TxBuilder.estimationTxSize(outs.size(), scriptPubKey, tx.getOuts(), isCompressed);

        log.info("-----size = {}", size);
        if (size > 1000) {
            fees = (size / 1000 + 1) * transFee;
        }
        log.info("-----fees = {}", fees);

        // note : like bitcoinj, empty wallet will not check min output
        if (fees > 0) {
            Out lastOut = tx.getOuts().get(tx.getOuts().size() - 1);
            log.info("-----lastOut 1 = {}", lastOut);
            if (lastOut.getOutValue() > fees) {
                lastOut.setOutValue(lastOut.getOutValue() - fees);
            } else {
                return null;
            }
            log.info("-----lastOut 1 = {}", lastOut);
        }
        for (Out out : outs) {
            tx.addInput(out);
        }

        tx.setSource(Tx.SourceType.self.getValue());
        return tx;
    }

    @Override
    public Tx buildTx(String changeAddress, List<Out> unspendOuts, Tx tx, long transFee) {
        List<Out> outs = unspendOuts;

        long value = 0;
        for (Out out : tx.getOuts()) {
            value += out.getOutValue();
        }
        boolean needMinFee = TxBuilder.needMinFee(tx.getOuts());
        log.info("-------transFee = {}, needMinFee = {}", transFee, needMinFee);

        if (value != TxBuilder.getAmount(unspendOuts) || value != TxBuilder.getAmount(outs)) {
            return null;
        }

        long fees = 0;
        if (needMinFee) {
            fees = transFee;
            log.info("-------default fees = {}", fees);
        } else {
            // no fee logic
            int s = TxBuilder.estimationTxSize(outs.size(), tx.getOuts().size());
            if (TxBuilder.getCoinDepth(outs) <= TxBuilder.TX_FREE_MIN_PRIORITY * s) {
                fees = transFee;
                log.info("-------s fees = {}, {}", s, fees);
            }
        }

        log.info("-------new tx = {}", tx);

        int size = TxBuilder.estimationTxSize(outs.size(), tx.getOuts().size());

        log.info("-----size = {}", size);

        if (size > 1000) {
            fees = (size / 1000 + 1) * transFee;
        }
        log.info("-----fees = {}", fees);

        // note : like bitcoinj, empty wallet will not check min output
        if (fees > 0) {
            Out lastOut = tx.getOuts().get(tx.getOuts().size() - 1);
            log.info("-----lastOut 1 = {}", lastOut);
            if (lastOut.getOutValue() > fees) {
                lastOut.setOutValue(lastOut.getOutValue() - fees);
            } else {
                return null;
            }
            log.info("-----lastOut 2 = {}", lastOut);
        }
        for (Out out : outs) {
            tx.addInput(out);
        }

        tx.setSource(Tx.SourceType.self.getValue());
        return tx;
    }

    @Override
    public Tx buildTx(Script scriptPubKey, String changeAddress, List<Out> unspendOuts, Long safeAssetFee, Tx tx, long transFee) throws AddressFormatException {
        return null; //SAFE资产不需要构建清空钱包交易
    }
}

class TxBuilderDefault implements TxBuilderProtocol {

    final Logger log = LoggerFactory.getLogger(TxBuilder.class);

    public Tx buildTx(Script scriptPubKey, String changeAddress, List<Out> unspendOuts, Tx tx, long transFee, boolean isCompressed) throws AddressFormatException {

        List<Out> outs = unspendOuts;

        Collections.sort(outs, new Comparator<Out>() {
            public int compare(Out out1, Out out2) {
                if (out1.getOutValue() != out2.getOutValue()) {
                    if (out2.getOutValue() > out1.getOutValue())
                        return 1;
                    else
                        return -1;
                } else {
                    BigInteger hash1 = new BigInteger(1, out1.getTxHash());
                    BigInteger hash2 = new BigInteger(1, out2.getTxHash());
                    int result = hash1.compareTo(hash2);
                    if (result != 0) {
                        return result;
                    } else {
                        return out1.getOutSn() - out2.getOutSn();
                    }
                }
            }
        });

        long additionalValueForNextCategory = 0;
        List<Out> selection3 = null;
        List<Out> selection2 = null;
        Out selection2Change = null;
        List<Out> selection1 = null;
        Out selection1Change = null;

        int lastCalculatedSize = 0;
        long valueNeeded;
        long value = 0;
        for (Out out : tx.getOuts()) {
            value += out.getOutValue();
        }

        boolean needAtLeastReferenceFee = TxBuilder.needMinFee(tx.getOuts());

        List<Out> bestCoinSelection = null;
        Out bestChangeOutput = null;
        while (true) {
            long fees = 0;

            if (lastCalculatedSize >= 1000) {
                // If the size is exactly 1000 bytes then we'll over-pay, but this should be rare.
                fees += (lastCalculatedSize / 1000 + 1) * transFee;
            }
            if (needAtLeastReferenceFee && fees < transFee)
                fees = transFee;

            valueNeeded = value + fees;

            if (additionalValueForNextCategory > 0)
                valueNeeded += additionalValueForNextCategory;

            long additionalValueSelected = additionalValueForNextCategory;

            List<Out> selectedOuts = this.selectOuts(outs, valueNeeded);

            if (TxBuilder.getAmount(selectedOuts) < valueNeeded)
                break;

            // no fee logic
            if (!needAtLeastReferenceFee) {
                long total = TxBuilder.getAmount(selectedOuts);
                if (total - value < Utils.CENT && total - value >= transFee) {
                    needAtLeastReferenceFee = true;
                    continue;
                }
                int s = TxBuilder.estimationTxSize(selectedOuts.size(), scriptPubKey, tx.getOuts(), isCompressed);
                if (total - value > Utils.CENT)
                    s += 34;
                if (TxBuilder.getCoinDepth(selectedOuts) <= TxBuilder.TX_FREE_MIN_PRIORITY * s) {
                    needAtLeastReferenceFee = true;
                    continue;
                }
            }

            boolean eitherCategory2Or3 = false;
            boolean isCategory3 = false;

            long change = TxBuilder.getAmount(selectedOuts) - valueNeeded;
            if (additionalValueSelected > 0)
                change += additionalValueSelected;

            if (SafeColdSettings.ensureMinFee && change != 0 && change < Utils.CENT
                    && fees < transFee) {
                // This solution may fit into category 2, but it may also be category 3, we'll check that later
                eitherCategory2Or3 = true;
                additionalValueForNextCategory = Utils.CENT;
                // If the change is smaller than the fee we want to add, this will be negative
                change -= transFee - fees;
            }

            int size = 0;
            Out changeOutput = null;
            if (change > 0) {
                changeOutput = new Out();
                changeOutput.setOutValue(change);
                changeOutput.setCurrency(tx.getCurrency());
                changeOutput.setOutAddress(changeAddress);
                // If the change output would result in this transaction being rejected as dust, just drop the change and make it a fee
                if (SafeColdSettings.ensureMinFee && Tx.getMinNondustOutput(tx.getCurrency()) >= change) {
                    // This solution definitely fits in category 3
                    isCategory3 = true;
                    additionalValueForNextCategory = transFee + Tx.getMinNondustOutput(tx.getCurrency()) + 1;
                } else {
                    size += 34;
                    // This solution is either category 1 or 2
                    if (!eitherCategory2Or3) // must be category 1
                        additionalValueForNextCategory = 0;
                }
            } else {
                if (eitherCategory2Or3) {
                    // This solution definitely fits in category 3 (we threw away change because it was smaller than MIN_TX_FEE)
                    isCategory3 = true;
                    additionalValueForNextCategory = transFee + 1;
                }
            }
            size += TxBuilder.estimationTxSize(selectedOuts.size(), scriptPubKey, tx.getOuts(), true);
            if (size / 1000 > lastCalculatedSize / 1000 && transFee > 0) {
                lastCalculatedSize = size;
                // We need more fees anyway, just try again with the same additional value
                additionalValueForNextCategory = additionalValueSelected;
                continue;
            }

            if (isCategory3) {
                if (selection3 == null)
                    selection3 = selectedOuts;
            } else if (eitherCategory2Or3) {
                // If we are in selection2, we will require at least CENT additional. If we do that, there is no way
                // we can end up back here because CENT additional will always get us to 1
                if (selection2 != null) {
                    long oldFee = TxBuilder.getAmount(selection2) - selection2Change.getOutValue() - value;
                    long newFee = TxBuilder.getAmount(selectedOuts) - changeOutput.getOutValue() - value;
                    if (newFee <= oldFee) {
                        selection2 = selectedOuts;
                        selection2Change = changeOutput;
                    }
                } else {
                    selection2 = selectedOuts;
                    selection2Change = changeOutput;
                }
            } else {
                // Once we get a category 1 (change kept), we should break out of the loop because we can't do better
                if (selection1 != null) {
                    long oldFee = TxBuilder.getAmount(selection1) - value;
                    if (selection1Change != null) {
                        oldFee -= selection1Change.getOutValue();
                    }
                    long newFee = TxBuilder.getAmount(selectedOuts) - value;
                    if (changeOutput != null) {
                        newFee -= changeOutput.getOutValue();
                    }
                    if (newFee <= oldFee) {
                        selection1 = selectedOuts;
                        selection1Change = changeOutput;
                    }
                } else {
                    selection1 = selectedOuts;
                    selection1Change = changeOutput;
                }
            }

            if (additionalValueForNextCategory > 0) {
                continue;
            }
            break;
        }

        if (selection3 == null && selection2 == null && selection1 == null) {
            return null;
        }

        long lowestFee = 0;

        if (selection1 != null) {
            if (selection1Change != null)
                lowestFee = TxBuilder.getAmount(selection1) - selection1Change.getOutValue() - value;
            else
                lowestFee = TxBuilder.getAmount(selection1) - value;
            bestCoinSelection = selection1;
            bestChangeOutput = selection1Change;
        }

        if (selection2 != null) {
            long fee = TxBuilder.getAmount(selection2) - selection2Change.getOutValue() - value;
            if (lowestFee == 0 || fee < lowestFee) {
                lowestFee = fee;
                bestCoinSelection = selection2;
                bestChangeOutput = selection2Change;
            }
        }

        if (selection3 != null) {
            if (lowestFee == 0 || TxBuilder.getAmount(selection3) - value < lowestFee) {
                bestCoinSelection = selection3;
                bestChangeOutput = null;
            }
        }

        if (bestChangeOutput != null) {
            tx.addOutput(bestChangeOutput.getOutValue(), bestChangeOutput.getOutAddress(), tx.getCurrency());
        }

        for (Out out : bestCoinSelection) {
            tx.addInput(out);
        }

        tx.setSource(Tx.SourceType.self.getValue());
        return tx;
    }

    @Override
    public Tx buildTx(String changeAddress, List<Out> unspendOuts, Tx tx, long transFee) throws AddressFormatException {

        List<Out> outs = unspendOuts;

        long additionalValueForNextCategory = 0;
        List<Out> selection3 = null;
        List<Out> selection2 = null;
        Out selection2Change = null;
        List<Out> selection1 = null;
        Out selection1Change = null;

        int lastCalculatedSize = 0;
        long valueNeeded;
        long value = 0;
        for (Out out : tx.getOuts()) {
            value += out.getOutValue();
        }

        boolean needAtLeastReferenceFee = TxBuilder.needMinFee(tx.getOuts());

        List<Out> bestCoinSelection = null;
        Out bestChangeOutput = null;
        while (true) {
            long fees = 0;

            if (lastCalculatedSize >= 1000) {
                // If the size is exactly 1000 bytes then we'll over-pay, but this should be rare.
                fees += (lastCalculatedSize / 1000 + 1) * transFee;
            }
            if (needAtLeastReferenceFee && fees < transFee)
                fees = transFee;

            valueNeeded = value + fees;

            if (additionalValueForNextCategory > 0)
                valueNeeded += additionalValueForNextCategory;

            long additionalValueSelected = additionalValueForNextCategory;

            List<Out> selectedOuts = this.selectOuts(outs, valueNeeded);

            if (TxBuilder.getAmount(selectedOuts) < valueNeeded)
                break;

            // no fee logic
            if (!needAtLeastReferenceFee) {
                long total = TxBuilder.getAmount(selectedOuts);
                if (total - value < Utils.CENT && total - value >= transFee) {
                    needAtLeastReferenceFee = true;
                    continue;
                }
                int s = TxBuilder.estimationTxSize(selectedOuts.size(), tx.getOuts().size());
                if (total - value > Utils.CENT)
                    s += 34;
                if (TxBuilder.getCoinDepth(selectedOuts) <= TxBuilder.TX_FREE_MIN_PRIORITY * s) {
                    needAtLeastReferenceFee = true;
                    continue;
                }
            }

            boolean eitherCategory2Or3 = false;
            boolean isCategory3 = false;

            long change = TxBuilder.getAmount(selectedOuts) - valueNeeded;
            if (additionalValueSelected > 0)
                change += additionalValueSelected;

            if (SafeColdSettings.ensureMinFee && change != 0 && change < Utils.CENT
                    && fees < transFee) {
                // This solution may fit into category 2, but it may also be category 3, we'll check that later
                eitherCategory2Or3 = true;
                additionalValueForNextCategory = Utils.CENT;
                // If the change is smaller than the fee we want to add, this will be negative
                change -= transFee - fees;
            }

            int size = 0;
            Out changeOutput = null;
            if (change > 0) {
                changeOutput = new Out();
                changeOutput.setOutValue(change);
                changeOutput.setCurrency(tx.getCurrency());
                changeOutput.setOutAddress(changeAddress);
                // If the change output would result in this transaction being rejected as dust, just drop the change and make it a fee
                if (SafeColdSettings.ensureMinFee && Tx.getMinNondustOutput(tx.getCurrency()) >= change) {
                    // This solution definitely fits in category 3
                    isCategory3 = true;
                    additionalValueForNextCategory = transFee + Tx.getMinNondustOutput(tx.getCurrency()) + 1;
                } else {
                    size += 34;
                    // This solution is either category 1 or 2
                    if (!eitherCategory2Or3) // must be category 1
                        additionalValueForNextCategory = 0;
                }
            } else {
                if (eitherCategory2Or3) {
                    // This solution definitely fits in category 3 (we threw away change because it was smaller than MIN_TX_FEE)
                    isCategory3 = true;
                    additionalValueForNextCategory = transFee + 1;
                }
            }
            size += TxBuilder.estimationTxSize(selectedOuts.size(), tx.getOuts().size());
            if (size / 1000 > lastCalculatedSize / 1000 && transFee > 0) {
                lastCalculatedSize = size;
                // We need more fees anyway, just try again with the same additional value
                additionalValueForNextCategory = additionalValueSelected;
                continue;
            }

            if (isCategory3) {
                if (selection3 == null)
                    selection3 = selectedOuts;
            } else if (eitherCategory2Or3) {
                // If we are in selection2, we will require at least CENT additional. If we do that, there is no way
                // we can end up back here because CENT additional will always get us to 1
                if (selection2 != null) {
                    long oldFee = TxBuilder.getAmount(selection2) - selection2Change.getOutValue() - value;
                    long newFee = TxBuilder.getAmount(selectedOuts) - changeOutput.getOutValue() - value;
                    if (newFee <= oldFee) {
                        selection2 = selectedOuts;
                        selection2Change = changeOutput;
                    }
                } else {
                    selection2 = selectedOuts;
                    selection2Change = changeOutput;
                }
            } else {
                // Once we get a category 1 (change kept), we should break out of the loop because we can't do better
                if (selection1 != null) {
                    long oldFee = TxBuilder.getAmount(selection1) - value;
                    if (selection1Change != null) {
                        oldFee -= selection1Change.getOutValue();
                    }
                    long newFee = TxBuilder.getAmount(selectedOuts) - value;
                    if (changeOutput != null) {
                        newFee -= changeOutput.getOutValue();
                    }
                    if (newFee <= oldFee) {
                        selection1 = selectedOuts;
                        selection1Change = changeOutput;
                    }
                } else {
                    selection1 = selectedOuts;
                    selection1Change = changeOutput;
                }
            }

            if (additionalValueForNextCategory > 0) {
                continue;
            }
            break;
        }

        if (selection3 == null && selection2 == null && selection1 == null) {
            return null;
        }

        long lowestFee = 0;

        if (selection1 != null) {
            if (selection1Change != null)
                lowestFee = TxBuilder.getAmount(selection1) - selection1Change.getOutValue() - value;
            else
                lowestFee = TxBuilder.getAmount(selection1) - value;
            bestCoinSelection = selection1;
            bestChangeOutput = selection1Change;
        }

        if (selection2 != null) {
            long fee = TxBuilder.getAmount(selection2) - selection2Change.getOutValue() - value;
            if (lowestFee == 0 || fee < lowestFee) {
                lowestFee = fee;
                bestCoinSelection = selection2;
                bestChangeOutput = selection2Change;
            }
        }

        if (selection3 != null) {
            if (lowestFee == 0 || TxBuilder.getAmount(selection3) - value < lowestFee) {
                bestCoinSelection = selection3;
                bestChangeOutput = null;
            }
        }

        if (bestChangeOutput != null) {
            tx.addOutput(bestChangeOutput.getOutValue(), bestChangeOutput.getOutAddress(), tx.getCurrency());
        }

        for (Out out : bestCoinSelection) {
            tx.addInput(out);
        }

        tx.setSource(Tx.SourceType.self.getValue());
        return tx;
    }

    @Override
    public Tx buildTx(Script scriptPubKey, String changeAddress, List<Out> unspendOuts, Long safeAssetFee, Tx tx, long transFee) throws AddressFormatException {

        List<Out> outs = unspendOuts;

        long additionalValueForNextCategory = 0;
        List<Out> selection3 = null;
        List<Out> selection2 = null;
        Out selection2Change = null;
        List<Out> selection1 = null;
        Out selection1Change = null;

        int lastCalculatedSize = 0;
        long valueNeeded;
        long value = safeAssetFee;

        boolean needAtLeastReferenceFee = TxBuilder.needMinFee(tx.getOuts());

        List<Out> bestCoinSelection = null;
        Out bestChangeOutput = null;
        while (true) {
            long fees = 0;

            if (lastCalculatedSize >= 1000) {
                // If the size is exactly 1000 bytes then we'll over-pay, but this should be rare.
                fees += (lastCalculatedSize / 1000 + 1) * transFee;
            }
            if (needAtLeastReferenceFee && fees < transFee)
                fees = transFee;

            valueNeeded = value + fees;

            log.info("-------valueNeeded = {} , fees = {}", valueNeeded, fees);

            if (additionalValueForNextCategory > 0)
                valueNeeded += additionalValueForNextCategory;

            long additionalValueSelected = additionalValueForNextCategory;

            List<Out> selectedOuts = this.selectOuts(outs, valueNeeded);

            if (TxBuilder.getAmount(selectedOuts) < valueNeeded)
                break;

            // no fee logic
            if (!needAtLeastReferenceFee) {
                long total = TxBuilder.getAmount(selectedOuts);
                if (total - value < Utils.CENT && total - value >= transFee) {
                    needAtLeastReferenceFee = true;
                    continue;
                }
                int s = TxBuilder.estimationTxSize(selectedOuts.size(), tx.getOuts().size());
                if (total - value > Utils.CENT)
                    s += 34;
                if (TxBuilder.getCoinDepth(selectedOuts) <= TxBuilder.TX_FREE_MIN_PRIORITY * s) {
                    needAtLeastReferenceFee = true;
                    continue;
                }
            }

            boolean eitherCategory2Or3 = false;
            boolean isCategory3 = false;

            long change = TxBuilder.getAmount(selectedOuts) - valueNeeded;

            log.info("-------change 1 = {}", change);
            if (additionalValueSelected > 0)
                change += additionalValueSelected;

            log.info("-------change 2 = {}", change);
            if (SafeColdSettings.ensureMinFee && change != 0 && change < Utils.CENT
                    && fees < transFee) {

                // This solution may fit into category 2, but it may also be category 3, we'll check that later
                eitherCategory2Or3 = true;
                additionalValueForNextCategory = Utils.CENT;
                // If the change is smaller than the fee we want to add, this will be negative
                change -= transFee - fees;
            }
            log.info("-------change 3 = {}", change);
            int size = 0;
            Out changeOutput = null;
            if (change > 0) {
                changeOutput = new Out();
                changeOutput.setOutValue(change);
                changeOutput.setCurrency(tx.getCurrency());
                changeOutput.setOutAddress(changeAddress);
                // If the change output would result in this transaction being rejected as dust, just drop the change and make it a fee
                if (SafeColdSettings.ensureMinFee && Tx.getMinNondustOutput(tx.getCurrency()) >= change) {
                    // This solution definitely fits in category 3
                    isCategory3 = true;
                    additionalValueForNextCategory = transFee + Tx.getMinNondustOutput(tx.getCurrency()) + 1;
                    log.info("---------additionalValueForNextCategory 1 = {}", additionalValueForNextCategory);
                } else {
                    size += 34;
                    // This solution is either category 1 or 2
                    if (!eitherCategory2Or3) // must be category 1
                        additionalValueForNextCategory = 0;
                    log.info("---------additionalValueForNextCategory 2 = {}", additionalValueForNextCategory);
                }
            } else {
                if (eitherCategory2Or3) {
                    // This solution definitely fits in category 3 (we threw away change because it was smaller than MIN_TX_FEE)
                    isCategory3 = true;
                    additionalValueForNextCategory = transFee + 1;
                    log.info("---------additionalValueForNextCategory 3 = {}", additionalValueForNextCategory);
                }
            }
            log.info("-------size 1 = {}", size);
            log.info("-------ins size = {}, outs size = {}", selectedOuts.size(), tx.getOuts().size());
            int realInSize = tx.getIns().size() + selectedOuts.size();
            log.info("-------realInSize = {}", realInSize);
            size += TxBuilder.estimationTxSize(realInSize, scriptPubKey, tx.getOuts(), false);
            log.info("-------size 2 = {}", size);

            log.info("-------lastCalculatedSize 1 = {}", lastCalculatedSize);

            if (size / 1000 > lastCalculatedSize / 1000 && transFee > 0) {
                lastCalculatedSize = size;
                // We need more fees anyway, just try again with the same additional value
                additionalValueForNextCategory = additionalValueSelected;
                continue;
            }
            log.info("-------lastCalculatedSize 2 = {}", lastCalculatedSize);
            if (isCategory3) {
                if (selection3 == null)
                    selection3 = selectedOuts;
            } else if (eitherCategory2Or3) {
                // If we are in selection2, we will require at least CENT additional. If we do that, there is no way
                // we can end up back here because CENT additional will always get us to 1
                if (selection2 != null) {
                    long oldFee = TxBuilder.getAmount(selection2) - selection2Change.getOutValue() - value;
                    long newFee = TxBuilder.getAmount(selectedOuts) - changeOutput.getOutValue() - value;
                    if (newFee <= oldFee) {
                        selection2 = selectedOuts;
                        selection2Change = changeOutput;
                    }
                } else {
                    selection2 = selectedOuts;
                    selection2Change = changeOutput;
                }
            } else {
                // Once we get a category 1 (change kept), we should break out of the loop because we can't do better
                if (selection1 != null) {
                    long oldFee = TxBuilder.getAmount(selection1) - value;
                    log.info("-------oldFee 1 = {} ", oldFee);
                    if (selection1Change != null) {
                        oldFee -= selection1Change.getOutValue();
                    }
                    log.info("-------oldFee 2 = {} ", oldFee);
                    long newFee = TxBuilder.getAmount(selectedOuts) - value;
                    log.info("-------newFee 1 = {} ", newFee);
                    if (changeOutput != null) {
                        newFee -= changeOutput.getOutValue();
                    }
                    log.info("-------newFee 2 = {} ", newFee);
                    if (newFee <= oldFee) {
                        selection1 = selectedOuts;
                        selection1Change = changeOutput;
                    }
                } else {
                    selection1 = selectedOuts;
                    selection1Change = changeOutput;
                }
            }

            if (additionalValueForNextCategory > 0) {
                continue;
            }
            log.info("-------selection1 = {} , selection2 = {}, selection3 = {}", selection1, selection2, selection3);
            break;
        }

        if (selection3 == null && selection2 == null && selection1 == null) {
            return null;
        }

        long lowestFee = 0;
        log.info("-------lowestFee 1 = {} ", lowestFee);

        if (selection1 != null) {
            if (selection1Change != null)
                lowestFee = TxBuilder.getAmount(selection1) - selection1Change.getOutValue() - value;
            else
                lowestFee = TxBuilder.getAmount(selection1) - value;
            bestCoinSelection = selection1;
            bestChangeOutput = selection1Change;
        }
        log.info("-------lowestFee 2 = {} ", lowestFee);

        if (selection2 != null) {
            long fee = TxBuilder.getAmount(selection2) - selection2Change.getOutValue() - value;
            if (lowestFee == 0 || fee < lowestFee) {
                lowestFee = fee;
                bestCoinSelection = selection2;
                bestChangeOutput = selection2Change;
            }
        }

        if (selection3 != null) {
            if (lowestFee == 0 || TxBuilder.getAmount(selection3) - value < lowestFee) {
                bestCoinSelection = selection3;
                bestChangeOutput = null;
            }
        }

        if (bestChangeOutput != null) {
            tx.addOutput(bestChangeOutput.getOutValue(), bestChangeOutput.getOutAddress(), tx.getCurrency());
        }

        for (Out out : bestCoinSelection) {
            tx.addInput(out);
        }

        tx.setSource(Tx.SourceType.self.getValue());
        return tx;
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
}