
package com.bankledger.safecold.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteOpenHelper;

import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.db.base.AndroidDb;
import com.bankledger.safecold.utils.EncryptionChipManagerV2;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.impl.AbstractHDAccountProvider;
import com.bankledger.safecoldj.db.impl.base.ICursor;
import com.bankledger.safecoldj.db.impl.base.IDb;
import com.google.common.base.Function;

import javax.annotation.Nullable;

/**
 * @author zhangmiao
 * @time 2019/7/02 15:52
 */
public class HDAccountProvider extends AbstractHDAccountProvider {

    private static HDAccountProvider hdAccountProvider = new HDAccountProvider(SafeColdApplication.dbHelper);

    public static HDAccountProvider getInstance() {
        return hdAccountProvider;
    }

    private SQLiteOpenHelper helper;

    public HDAccountProvider(SQLiteOpenHelper helper) {
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


    protected int insertHDAccountToDb(IDb db, String encryptedMnemonicSeed, String encryptSeed) {
        AndroidDb mdb = (AndroidDb) db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.HDAccountColumns.ENCRYPT_SEED, encryptSeed);
        cv.put(AbstractDb.HDAccountColumns.ENCRYPT_MNEMONIC_SEED, encryptedMnemonicSeed);
        return (int) mdb.getSQLiteDatabase().insert(AbstractDb.Tables.HD_ACCOUNT, null, cv);
    }

    @Override
    public long addHDAccount(String encryptedMnemonicSeed, String encryptSeed, String userPIN) {
        if (SafeColdSettings.SEED_SAVE_TO_CHIP) {
            return EncryptionChipManagerV2.getInstance().saveMnemonicSeed(encryptedMnemonicSeed, encryptSeed, userPIN);
        } else {
            IDb writeDb = this.getWriteDb();
            writeDb.beginTransaction();
            this.insertHDAccountToDb(writeDb, encryptedMnemonicSeed, encryptSeed);
            writeDb.endTransaction();
            return 0;
        }
    }

    @Override
    public boolean hasMnemonicSeed() {
        if (SafeColdSettings.SEED_SAVE_TO_CHIP) {
            return EncryptionChipManagerV2.getInstance().hasMnemonicSeed();
        } else {
            String sql = "select count(0) cnt from hd_account where encrypt_mnemonic_seed is not null";
            final boolean[] result = {false};
            this.execQueryOneRecord(sql, new String[]{}, new Function<ICursor, Void>() {
                @Nullable
                @Override
                public Void apply(@Nullable ICursor c) {
                    int idColumn = c.getColumnIndex("cnt");
                    if (idColumn != -1) {
                        result[0] = c.getInt(idColumn) > 0;
                    }
                    return null;
                }
            });
            return result[0];
        }
    }

    @Override
    public String getHDAccountEncryptSeed(String userPIN) {
        if (SafeColdSettings.SEED_SAVE_TO_CHIP) {
            return EncryptionChipManagerV2.getInstance().getEncryptMnemonicSeed(userPIN, EncryptionChipManagerV2.SeedType.TYPE_SEED);
        } else {
            final String[] hdAccountEncryptSeed = {null};
            String sql = "select encrypt_seed from hd_account";
            this.execQueryOneRecord(sql, new String[]{}, new Function<ICursor, Void>() {
                @Nullable
                @Override
                public Void apply(@Nullable ICursor c) {
                    int idColumn = c.getColumnIndex(AbstractDb.HDAccountColumns.ENCRYPT_SEED);
                    if (idColumn != -1) {
                        hdAccountEncryptSeed[0] = c.getString(idColumn);
                    }
                    return null;
                }
            });
            return hdAccountEncryptSeed[0];
        }
    }

    @Override
    public String getHDAccountEncryptMnemonicSeed(String userPIN) {
        if (SafeColdSettings.SEED_SAVE_TO_CHIP) {
            return EncryptionChipManagerV2.getInstance().getEncryptMnemonicSeed(userPIN, EncryptionChipManagerV2.SeedType.TYPE_MNEMONIC_SEED);
        } else {
            final String[] hdAccountMnemonicEncryptSeed = {null};
            String sql = "select encrypt_mnemonic_seed from hd_account";
            this.execQueryOneRecord(sql, new String[]{}, new Function<ICursor, Void>() {
                @Nullable
                @Override
                public Void apply(@Nullable ICursor c) {
                    int idColumn = c.getColumnIndex(AbstractDb.HDAccountColumns.ENCRYPT_MNEMONIC_SEED);
                    if (idColumn != -1) {
                        hdAccountMnemonicEncryptSeed[0] = c.getString(idColumn);
                    }
                    return null;
                }
            });
            return hdAccountMnemonicEncryptSeed[0];
        }
    }


    @Override
    public long replaceEncryptedSeed(String encryptedMnemonicSeed, String encryptSeed, String userPIN) {
        if (SafeColdSettings.SEED_SAVE_TO_CHIP) {
            EncryptionChipManagerV2.getInstance().deleteApp(EncryptionChipManagerV2.getInstance().NEW_APP_NAME);
            EncryptionChipManagerV2.getInstance().deleteApp(EncryptionChipManagerV2.getInstance().APP_NAME);
            return EncryptionChipManagerV2.getInstance().saveMnemonicSeed(encryptedMnemonicSeed, encryptSeed, userPIN);
        } else {
            String sql = "update hd_account set encrypt_mnemonic_seed= ?,encrypt_seed = ?";
            this.execUpdate(sql, new String[]{encryptedMnemonicSeed, encryptSeed});
            return 0;
        }
    }
}
