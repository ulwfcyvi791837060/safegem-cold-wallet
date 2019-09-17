package com.bankledger.safecold.db;

import android.database.sqlite.SQLiteOpenHelper;

import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.db.base.AndroidDb;
import com.bankledger.safecoldj.db.impl.AbstractETHTokenProvider;
import com.bankledger.safecoldj.db.impl.base.IDb;

/**
 * @author bankledger
 * @time 2018/9/10 17:43
 */
public class ETHTokenProvider extends AbstractETHTokenProvider {

    private static ETHTokenProvider ethTokenProvider = new ETHTokenProvider(SafeColdApplication.dbHelper);

    public static ETHTokenProvider getInstance() {
        return ethTokenProvider;
    }

    private SQLiteOpenHelper helper;

    public ETHTokenProvider(SQLiteOpenHelper helper) {
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
}
