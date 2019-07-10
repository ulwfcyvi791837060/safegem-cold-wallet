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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.db.AbstractDb;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DB_VERSION = 4;
    private static final String DB_NAME = "safecold.db";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(AbstractDb.CREATE_OUTS_SQL);
        db.execSQL(AbstractDb.CREATE_HD_ACCOUNT);
        db.execSQL(AbstractDb.CREATE_HD_ADDRESSES);
        db.execSQL(AbstractDb.CREATE_HD_ADDRESS_INDEX);
        db.execSQL(AbstractDb.CREATE_COIN_ROOT_KEY);
        db.execSQL(AbstractDb.CREATE_CONTACTS_ADDRESS);
        db.execSQL(AbstractDb.CREATE_ETH_TOKEN);
        db.execSQL(AbstractDb.CREATE_EOS_ACCOUNT);
        db.execSQL(AbstractDb.CREATE_EOS_USDT_BALANCE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion >= 2) {
            db.execSQL(AbstractDb.CREATE_EOS_ACCOUNT);
            db.execSQL(AbstractDb.CREATE_EOS_USDT_BALANCE);

            //添加USDT币种修改主键
            String tempTableName = "temp_hd_addresses";
            String renameSql = "ALTER TABLE " + AbstractDb.Tables.HD_ADDRESS + " RENAME TO " + tempTableName;
            db.execSQL(renameSql);
            db.execSQL(AbstractDb.CREATE_HD_ADDRESSES);
            String copySql = "INSERT INTO " + AbstractDb.Tables.HD_ADDRESS + " SELECT * FROM " + tempTableName;
            db.execSQL(copySql);
            String dropTemp = "DROP TABLE " + tempTableName;
            db.execSQL(dropTemp);

            //添加SAFE资产币种将预留字段赋值为空
            String updateSql = "UPDATE " + AbstractDb.Tables.OUTS + " SET reserve = ? WHERE 1 = 1";
            db.execSQL(updateSql, new String[]{""});

        } else if (oldVersion == 2 && newVersion >= 3) {
            //添加SAFE资产币种将预留字段赋值为空
            String updateSql = "UPDATE " + AbstractDb.Tables.OUTS + " SET reserve = ? WHERE 1 = 1";
            db.execSQL(updateSql, new String[]{""});
        } else if (oldVersion == 3 && newVersion >= 4) {
            //导入EOS账户添加私钥和类型
            String dropTable = "DROP TABLE " + AbstractDb.Tables.EOS_ACCOUNT;
            db.execSQL(dropTable);
            db.execSQL(AbstractDb.CREATE_EOS_ACCOUNT);
        }
    }

    public void deleteDB() {
        getWritableDatabase().execSQL("delete from " + AbstractDb.Tables.OUTS);
        getWritableDatabase().execSQL("delete from " + AbstractDb.Tables.HD_ACCOUNT);
        getWritableDatabase().execSQL("delete from " + AbstractDb.Tables.HD_ADDRESS);
        getWritableDatabase().execSQL("delete from " + AbstractDb.Tables.COIN_ROOT_KEY);
        getWritableDatabase().execSQL("delete from " + AbstractDb.Tables.CONTACTS_ADDRESS);
        getWritableDatabase().execSQL("delete from " + AbstractDb.Tables.ETH_TOKEN);
        getWritableDatabase().execSQL("delete from " + AbstractDb.Tables.EOS_ACCOUNT);
        getWritableDatabase().execSQL("delete from " + AbstractDb.Tables.EOS_UDST_BALANCE);
    }

}
