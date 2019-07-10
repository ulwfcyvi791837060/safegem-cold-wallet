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
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.exception.ProtocolException;
import com.bankledger.safecoldj.exception.ScriptException;
import com.bankledger.safecoldj.message.Message;
import com.bankledger.safecoldj.script.Script;
import com.bankledger.safecoldj.utils.Sha256Hash;
import com.bankledger.safecoldj.utils.Utils;
import com.bankledger.safecoldj.utils.VarInt;
import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;


public class In extends Message {

    public static final int OUTPOINT_MESSAGE_LENGTH = 36;

    public static final long NO_SEQUENCE = 0xFFFFFFFFL;

    private byte[] txHash;
    private int inSn;
    private byte[] prevTxHash;
    private int prevOutSn;
    private byte[] inSignature;
    private byte[] prevOutScript;
    private long inSequence;
    private Tx tx;

    public byte[] getTxHash() {
        return txHash;
    }

    public void setTxHash(byte[] txHash) {
        this.txHash = txHash;
    }

    public int getInSn() {
        return inSn;
    }

    public void setInSn(int inSn) {
        this.inSn = inSn;
    }

    public byte[] getPrevTxHash() {
        return prevTxHash;
    }

    public void setPrevTxHash(byte[] prevTxHash) {
        this.prevTxHash = prevTxHash;
    }

    public String getPrevTxHashToHex() {
        return Utils.hashToString(prevTxHash);
    }

    public void setPrevTxHash(String prevTxHash) {
        byte[] data = Utils.hexStringToBytes(prevTxHash);
        this.prevTxHash = Utils.reverseBytes(data);
    }

    public int getPrevOutSn() {
        return prevOutSn;
    }

    public void setPrevOutSn(int prevOutSn) {
        this.prevOutSn = prevOutSn;
    }

    public byte[] getInSignature() {
        return inSignature;
    }

    public void setInSignature(byte[] inSignature) {
        this.inSignature = inSignature;
    }

    public byte[] getPrevOutScript() {
        return prevOutScript;
    }

    public void setPrevOutScript(byte[] prevOutScript) {
        this.prevOutScript = prevOutScript;
    }

    public long getInSequence() {
        return inSequence;
    }

    public void setInSequence(long inSequence) {
        this.inSequence = inSequence;
    }

    public OutPoint getOutpoint() {
        return new OutPoint(this.prevTxHash, this.prevOutSn);
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
        if (o instanceof In) {
            In inItem = (In) o;
            return getInSn() == inItem.getInSn() &&
                    Arrays.equals(getPrevTxHash(), inItem.getPrevTxHash()) &&
                    getPrevOutSn() == inItem.getPrevOutSn() &&
                    Arrays.equals(getTxHash(), inItem.getTxHash()) &&
                    getInSequence() == inItem.getInSequence() &&
                    Arrays.equals(getInSignature(), inItem.getInSignature());
        } else {
            return false;
        }
    }

    public In() {
        this.inSequence = NO_SEQUENCE;
    }

    public In(Currency mCurrency, Tx tx, byte[] msg, int offset) {
        super(mCurrency, msg, offset);
        this.tx = tx;
        this.txHash = tx.getTxHash();
    }

    public In(Tx tx, Out out) {
        this.tx = tx;
        this.txHash = tx.getTxHash();
        prevTxHash = out.getTxHash();
        prevOutSn = out.getOutSn();
        prevOutScript = out.getOutScript();
        this.inSequence = NO_SEQUENCE;
    }

    public In(@Nullable Tx parentTransaction, byte[] scriptBytes,
              Out outpoint) {
        super();
        this.inSignature = scriptBytes;
        this.prevTxHash = outpoint.getTxHash();
        this.prevOutSn = outpoint.getOutSn();
        this.prevOutScript = outpoint.getOutScript();
        this.inSequence = NO_SEQUENCE;
        this.tx = parentTransaction;
        this.txHash = this.tx.getTxHash();
        length = 40 + (scriptBytes == null ? 1 : VarInt.sizeOf(scriptBytes.length) + scriptBytes.length);
    }

    protected void parse() throws ProtocolException {
        int curs = cursor;
        int scriptLen = (int) readVarInt(36);
        length = cursor - offset + scriptLen + 4;
        cursor = curs;
        prevTxHash = readHash();
        prevOutSn = (int) readUint32();
        scriptLen = (int) readVarInt();
        inSignature = readBytes(scriptLen);
        inSequence = readUint32();
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        stream.write(prevTxHash);
        Utils.uint32ToByteStreamLE(prevOutSn, stream);
        stream.write(new VarInt(inSignature.length).encode());
        stream.write(inSignature);
        Utils.uint32ToByteStreamLE(inSequence, stream);
    }

    /**
     * Coinbase transactions have special inputs with hashes of zero. If this is such an input, returns true.
     */
    public boolean isCoinBase() {
        return Arrays.equals(prevTxHash, new byte[32]) &&
                (prevOutSn & 0xFFFFFFFFL) == 0xFFFFFFFFL;  // -1 but all is serialized to the wire as unsigned int.
    }

    /**
     * @return true if this transaction's sequence number is set (ie it may be a part of a time-locked transaction)
     */
    public boolean hasSequence() {
        return inSequence != NO_SEQUENCE;
    }

    public List<byte[]> getP2SHPubKeys() {
        Script script = new Script(this.getInSignature());
        return script.getP2SHPubKeys(this.tx, this.inSn);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("In { \n");
        try {
            s.append("txHash: " + Utils.hashToString(txHash) + "\n");
            s.append("sn: " + getInSn() + "\n");
            s.append("prevTxHash: " + getPrevTxHashToHex() + "\n");
            s.append("prevOutSn: " + getPrevOutSn() + "\n");
            if (getInSignature() != null) {
                s.append("inSignature: " + Utils.bytesToHexString(getInSignature()) + "\n");
            }
            s.append("inSequence: " + getInSequence() + "\n");
            return s.toString();
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }



}
