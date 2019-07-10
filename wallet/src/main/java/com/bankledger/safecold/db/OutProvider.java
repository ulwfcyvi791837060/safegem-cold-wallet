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

package com.bankledger.safecold.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.db.base.AndroidDb;
import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.impl.AbstractOutProvider;
import com.bankledger.safecoldj.db.impl.base.IDb;
import com.bankledger.safecoldj.utils.GsonUtils;

import org.json.JSONObject;

public class OutProvider extends AbstractOutProvider {

    private static OutProvider txProvider = new OutProvider(SafeColdApplication.dbHelper);

    public static OutProvider getInstance() {
        return txProvider;
    }

    private SQLiteOpenHelper helper;

    public OutProvider(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    @Override
    public IDb getReadDb() {
        return new AndroidDb(this.helper.getReadableDatabase());
    }

    @Override
    public IDb getWriteDb() {
        return new AndroidDb(this.helper.getWritableDatabase());
    }

    @Override
    protected void insertOutToDb(IDb db, Out out) {
        if (TextUtils.isEmpty(out.getReserve()) || out.getReserve().equals("safe")) {
            AndroidDb adb = (AndroidDb) db;
            ContentValues cv = new ContentValues();
            cv.put(AbstractDb.OutsColumns.TX_HASH, out.getTxHashToHex());
            cv.put(AbstractDb.OutsColumns.OUT_SN, out.getOutSn());
            cv.put(AbstractDb.OutsColumns.COIN, out.getCurrency().coin);
            cv.put(AbstractDb.OutsColumns.OUT_STATUS, out.getOutStatus());
            cv.put(AbstractDb.OutsColumns.OUT_VALUE, out.getOutValue());
            cv.put(AbstractDb.OutsColumns.MUL_TYPE, out.getMulType());
            cv.put(AbstractDb.OutsColumns.OUT_ADDRESS, out.getOutAddress());
            cv.put(AbstractDb.OutsColumns.UN_LOCK_HEIGHT, out.getUnLockHeight());
            cv.put(AbstractDb.OutsColumns.RESERVE, "");
            adb.getSQLiteDatabase().insert(AbstractDb.Tables.OUTS, null, cv);
        } else {
            SafeAsset safeAsset = GsonUtils.getObjFromJSON(out.getReserve(), SafeAsset.class);
            insertOutToDb(db, out, safeAsset);
        }
    }

    @Override
    protected void insertOutToDb(IDb db, Out out, SafeAsset safeAsset) {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("assetId", safeAsset.assetId);
            jsonObj.put("assetShortName", safeAsset.assetShortName);
            jsonObj.put("assetName", safeAsset.assetName);
            jsonObj.put("assetUnit", safeAsset.assetUnit);
            jsonObj.put("assetDecimals", safeAsset.assetDecimals);
        } catch (Exception e) {
            e.printStackTrace();
        }
        AndroidDb adb = (AndroidDb) db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.OutsColumns.TX_HASH, out.getTxHashToHex());
        cv.put(AbstractDb.OutsColumns.OUT_SN, out.getOutSn());
        cv.put(AbstractDb.OutsColumns.COIN, out.getCurrency().coin);
        cv.put(AbstractDb.OutsColumns.OUT_STATUS, out.getOutStatus());
        cv.put(AbstractDb.OutsColumns.OUT_VALUE, out.getOutValue());
        cv.put(AbstractDb.OutsColumns.MUL_TYPE, out.getMulType());
        cv.put(AbstractDb.OutsColumns.OUT_ADDRESS, out.getOutAddress());
        cv.put(AbstractDb.OutsColumns.UN_LOCK_HEIGHT, out.getUnLockHeight());
        cv.put(AbstractDb.OutsColumns.RESERVE, jsonObj.toString());
        adb.getSQLiteDatabase().insert(AbstractDb.Tables.OUTS, null, cv);
    }

}
