package com.bankledger.safecold.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteOpenHelper;

import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.db.base.AndroidDb;
import com.bankledger.safecold.utils.LogUtils;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.db.impl.AbstractContactsAddressProvider;
import com.bankledger.safecoldj.db.impl.base.IDb;

/**
 * @author bankledger
 * @time 2018/9/8 17:15
 */
public class ContactsAddressProvider extends AbstractContactsAddressProvider {
    private final SQLiteOpenHelper helper;

    private static ContactsAddressProvider contactsAddressProvider = new ContactsAddressProvider(SafeColdApplication.dbHelper);

    public static ContactsAddressProvider getInstance() {
        return contactsAddressProvider;
    }

    public ContactsAddressProvider(SQLiteOpenHelper helper) {
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
    protected int insertContactsAddressToDb(IDb db, ContactsAddress contactsAddress) {
        AndroidDb mdb = (AndroidDb) db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.ContactsAddressColumns.COIN, contactsAddress.getCoin());
        cv.put(AbstractDb.ContactsAddressColumns.ADDRESS, contactsAddress.getAddress());
        cv.put(AbstractDb.ContactsAddressColumns.ALIAS, contactsAddress.getAlias());
        cv.put(AbstractDb.ContactsAddressColumns.CONTRACT_ADDRESS, contactsAddress.getContractAddress());
        cv.put(AbstractDb.ContactsAddressColumns.COIN_TYPE, contactsAddress.getCoinType());
        return (int) mdb.getSQLiteDatabase().insert(AbstractDb.Tables.CONTACTS_ADDRESS, null, cv);
    }
}
