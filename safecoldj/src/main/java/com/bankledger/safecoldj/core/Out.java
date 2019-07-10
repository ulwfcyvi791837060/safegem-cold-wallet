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

import com.bankledger.protobuf.bean.UTXO;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.crypto.ECKey;
import com.bankledger.safecoldj.exception.AddressFormatException;
import com.bankledger.safecoldj.exception.ProtocolException;
import com.bankledger.safecoldj.exception.ScriptException;
import com.bankledger.safecoldj.message.Message;
import com.bankledger.safecoldj.script.Script;
import com.bankledger.safecoldj.script.ScriptBuilder;
import com.bankledger.safecoldj.utils.Utils;
import com.bankledger.safecoldj.utils.VarInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Out extends Message {

    public static final int OUT_STATUS_UNSPEND = 0;
    public static final int OUT_STATUS_SPEND = 1;

    private byte[] txHash;//交易ID
    private int outSn;//交易索引
    private byte[] outScript;//公钥
    private long outValue;//金额
    private int outStatus;//0未花费，1已花费
    private int mulType;//0单地址，1多地址
    private String outAddress;//地址，多地址逗号分割
    private long unLockHeight;//SAFE预留
    private String reserve;//SAFE预留

    private Tx tx;

    private transient int scriptLen;

    public Out() {

    }

    public Out(UTXO utxo) {
        setTxHash(utxo.txHash);
        outSn = utxo.outSn;
        outValue = utxo.value;
        outStatus = utxo.status;
        outAddress = utxo.address.get(0);
        unLockHeight = utxo.unlockHeight;
        reserve = utxo.reserve;
    }

    public Out(Currency mCurrency, Tx tx, byte[] msg, int offset) {
        super(mCurrency, msg, offset);
        this.tx = tx;
        this.txHash = this.tx.getTxHash();
    }

    public Out(Tx parent, long value, String outAddress, Currency mCurrency) throws AddressFormatException {
        this(parent, value, ScriptBuilder.createOutputScript(outAddress, mCurrency).getProgram());
        this.outAddress = outAddress;
        setCurrency(mCurrency);
    }

    public Out(Tx parent, long value, ECKey to) {
        this(parent, value, ScriptBuilder.createOutputScript(to).getProgram());
    }

    public Out(Tx parent, long value, byte[] scriptBytes) {
        super();
        checkArgument(value >= 0 || value == -1, "Negative values not allowed");
        checkArgument(value < SafeColdSettings.MAX_MONEY, "Values larger than MAX_MONEY not " +
                "allowed");
        this.outValue = value;
        this.outScript = scriptBytes;
        this.tx = parent;
        this.txHash = this.tx.getTxHash();
        length = 8 + VarInt.sizeOf(scriptBytes.length) + scriptBytes.length;
    }

    public byte[] getTxHash() {
        return txHash;
    }

    public String getTxHashToHex() {
        return Utils.hashToString(txHash);
    }

    public void setTxHash(String txHash) {
        byte[] data = Utils.hexStringToBytes(txHash);
        this.txHash = Utils.reverseBytes(data);
    }

    public int getOutSn() {
        return outSn;
    }

    public void setOutSn(int outSn) {
        this.outSn = outSn;
    }

    public void setCoin(String coin) {
        setCurrency(HDAddressManager.getInstance().getCurrencyMap().get(coin));
    }

    public void setCoinExistAsset(String coin) {
        setCurrencyExistAsset(HDAddressManager.getInstance().getCurrencyMap().get(coin));
    }

    public byte[] getOutScript() {
        return outScript;
    }

    public void setOutScript(byte[] outScript) {
        this.outScript = outScript;
    }

    public long getOutValue() {
        return outValue;
    }

    public void setOutValue(long outValue) {
        this.outValue = outValue;
    }

    public int getOutStatus() {
        return outStatus;
    }

    public void setOutStatus(int outStatus) {
        this.outStatus = outStatus;
    }

    public int getMulType() {
        return mulType;
    }

    public void setMulType(int mulType) {
        this.mulType = mulType;
    }

    public String getOutAddress() {
        if (outAddress == null) {
            try {
                Script pubKeyScript = new Script(this.getOutScript());
                outAddress = pubKeyScript.getToAddress(tx.getCurrency());
            } catch (ScriptException e) {
            }
        }
        return outAddress;

    }

    public void setOutAddress(String outAddress) {
        this.outAddress = outAddress;
        if (outScript == null) {
            try {
                this.outScript = ScriptBuilder.createOutputScript(outAddress, mCurrency).getProgram();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public long getUnLockHeight() {
        return unLockHeight;
    }

    public void setUnLockHeight(long unLockHeight) {
        this.unLockHeight = unLockHeight;
    }

    public String getReserve() {
        return reserve;
    }

    public void setReserve(String reserve) {
        this.reserve = reserve;
    }

    public Tx getTx() {
        return tx;
    }

    public void setTx(Tx tx) {
        this.tx = tx;
        this.txHash = tx.getTxHash();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Out) {
            Out outItem = (Out) o;
            return getOutSn() == outItem.getOutSn() &&
                    Arrays.equals(getTxHash(), outItem.getTxHash()) &&
                    Arrays.equals(getOutScript(), outItem.getOutScript()) &&
                    getOutValue() == outItem.getOutValue() &&
                    getOutStatus() == outItem.getOutStatus() &&
                    Utils.compareString(getOutAddress(), outItem.getOutAddress());

        } else {
            return false;
        }
    }


    protected void parse() throws ProtocolException {
        outValue = readInt64();
        scriptLen = (int) readVarInt();
        length = cursor - offset + scriptLen;
        outScript = readBytes(scriptLen);
        if (mCurrency.isSafe()) {
            unLockHeight = readInt64();
            int reserveLen = (int) readVarInt();
            length = cursor - offset + reserveLen;
            reserve = new String(readBytes(reserveLen));
        }
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        checkNotNull(outScript);
        Utils.int64ToByteStreamLE(outValue, stream);
        stream.write(new VarInt(outScript.length).encode());
        stream.write(outScript);
        if (mCurrency.isSafe()) {
            Utils.int64ToByteStreamLE(unLockHeight, stream);
            byte[] mReserve = getReserveBytes();
            stream.write(new VarInt(mReserve.length).encode());
            stream.write(mReserve);
        }
    }

    public byte[] getReserveBytes() {
        try {
            return reserve.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * <p>Gets the minimum value for a txout of this size to be considered non-dust by a
     * reference client
     * (and thus relayed). See: CTxOut::IsDust() in the reference client. The assumption is that
     * any output that would
     * consume more than a third of its value in fees is not something the Bitcoin system wants
     * to deal with right now,
     * so we call them "dust outputs" and they're made non standard. The choice of one third is
     * somewhat arbitrary and
     * may change in future.</p>
     * <p/>
     * <p>You probably should use {@link com.bankledger.safecoldj.core.Out#getMinNonDustValue()} which
     * uses
     * a safe fee-per-kb by default.</p>
     *
     * @param feePerKbRequired The fee required per kilobyte. Note that this is the same as the
     *                         reference client's -minrelaytxfee * 3
     *                         If you want a safe default, use {@link com.bankledger.safecoldj.core
     *                         .Tx#REFERENCE_DEFAULT_MIN_TX_FEE}*3
     */
    public BigInteger getMinNonDustValue(BigInteger feePerKbRequired) {
        // A typical output is 33 bytes (pubkey hash + opcodes) and requires an input of 148
        // bytes to spend so we add
        // that together to find out the total amount of data used to transfer this amount of
        // value. Note that this
        // formula is wrong for anything that's not a pay-to-address output, unfortunately, we
        // must follow the reference
        // clients wrongness in order to ensure we're considered standard. A better formula would
        // either estimate the
        // size of data needed to satisfy all different script types, or just hard code 33 below.
        final BigInteger size = BigInteger.valueOf(this.bitcoinSerialize().length + 148);
        BigInteger[] nonDustAndRemainder = feePerKbRequired.multiply(size).divideAndRemainder
                (BigInteger.valueOf(1000));
        return nonDustAndRemainder[1].equals(BigInteger.ZERO) ? nonDustAndRemainder[0] :
                nonDustAndRemainder[0].add(BigInteger.ONE);
    }

    /**
     * Returns the minimum value for this output to be considered "not dust", i.e. the
     * transaction will be relayable
     * and mined by default miners. For normal pay to address outputs, this is 5460 satoshis, the
     * same as
     * {@link com.bankledger.safecoldj.core.Tx#MIN_NONDUST_OUTPUT}.
     */
    public BigInteger getMinNonDustValue() {
        return getMinNonDustValue(BigInteger.valueOf(Tx.REFERENCE_DEFAULT_MIN_TX_FEE * 3));
    }

    @Override
    public void setCurrency(Currency mCurrency) {
        super.setCurrency(mCurrency);
        if (mCurrency.isSafe()) {
            unLockHeight = 0L; //默认添加字段
            reserve = "safe"; //默认添加字段
        }
    }

    public void setCurrencyExistAsset(Currency mCurrency) {
        super.setCurrency(mCurrency);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("TxOut = \n");
        try {
            s.append("txHash: " + Utils.hashToString(txHash) + "\n");
            s.append("outSn: " + getOutSn() + "\n");
            s.append("address: " + getOutAddress() + "\n");
            s.append("outStatus: " + getOutStatus() + "\n");
            s.append("value: " + outValue + "\n");
            if (outScript != null) {
                s.append("outScript: " + Utils.bytesToHexString(outScript) + "\n");
            }
            s.append("unLockHeight: " + unLockHeight + "\n");
            s.append("reserve: " + reserve + "\n");
            return s.toString();
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}
