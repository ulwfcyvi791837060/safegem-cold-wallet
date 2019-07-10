package com.bankledger.safecoldj.db.impl;

import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.db.IContactsAddressProvider;
import com.bankledger.safecoldj.db.impl.base.ICursor;
import com.bankledger.safecoldj.db.impl.base.IDb;
import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/9/8 16:31
 */
public abstract class AbstractContactsAddressProvider extends AbstractProvider implements IContactsAddressProvider {

    protected abstract int insertContactsAddressToDb(IDb db, ContactsAddress contactsAddress);

    @Override
    public void addContactsAddress(ContactsAddress contactsAddress) {
        IDb db = getWriteDb();
        db.beginTransaction();
        insertContactsAddressToDb(db, contactsAddress);
        db.endTransaction();
    }

    @Override
    public void deleteContactsAddress(ContactsAddress contactsAddress) {
        IDb db = getWriteDb();
        db.beginTransaction();
        this.execUpdate(db, "delete from " + AbstractDb.Tables.CONTACTS_ADDRESS + " where coin = ? and address = ? and alias = ?",
                new String[]{contactsAddress.getCoin(), contactsAddress.getAddress(), contactsAddress.getAlias()});
        db.endTransaction();
    }

    @Override
    public boolean checkContactsAddressExist(ContactsAddress contactsAddress) {
        String sql = "select ifnull(count(*),0) from " + AbstractDb.Tables.CONTACTS_ADDRESS + " where address = ? and coin = ?";
        final int[] count = {0};
        this.execQueryOneRecord(sql, new String[]{contactsAddress.getAddress(), contactsAddress.getCoin()}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                count[0] = c.getInt(0);
                return null;
            }
        });
        return count[0] > 0;
    }

    @Override
    public void replaceAddressAlias(ContactsAddress contactsAddress, String alias) {
        String sql = "update " + AbstractDb.Tables.CONTACTS_ADDRESS + " set alias= ? where coin = ? and address = ? and alias = ?";
        this.execUpdate(sql, new String[]{alias, contactsAddress.getCoin(), contactsAddress.getAddress(), contactsAddress.getAlias()});
    }

    @Override
    public List<ContactsAddress> matchAddress(String matchStr) {
        final List<ContactsAddress> hdAddressList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.CONTACTS_ADDRESS + " where address like ? or coin like ? or alias like ?";
        this.execQueryLoop(sql, new String[]{"%" + matchStr + "%", "%" + matchStr + "%", "%" + matchStr + "%"}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                hdAddressList.add(formatAddress(c));
                return null;
            }
        });
        return hdAddressList;
    }

    @Override
    public List<ContactsAddress> getCurrencyAddressWithCoin(String coin) {
        final List<ContactsAddress> hdAddressList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.CONTACTS_ADDRESS + " where coin = ? and coin_type = ?";
        this.execQueryLoop(sql, new String[]{coin, Integer.toString(ContactsAddress.COIN_TYPE_CURRENCY)}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                hdAddressList.add(formatAddress(c));
                return null;
            }
        });
        return hdAddressList;
    }

    @Override
    public List<ContactsAddress> getSafeAssetAddressWithCoin(String assetName) {
        final List<ContactsAddress> hdAddressList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.CONTACTS_ADDRESS + " where coin = ? and coin_type = ?";
        this.execQueryLoop(sql, new String[]{assetName, Integer.toString(ContactsAddress.COIN_TYPE_SAFE_ASSET)}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                hdAddressList.add(formatAddress(c));
                return null;
            }
        });
        return hdAddressList;
    }

    @Override
    public List<ContactsAddress> getAddressWithContractAddress(String contractAddress) {
        final List<ContactsAddress> hdAddressList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.CONTACTS_ADDRESS + " where contract_address =?";
        this.execQueryLoop(sql, new String[]{contractAddress}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                hdAddressList.add(formatAddress(c));
                return null;
            }
        });
        return hdAddressList;
    }

    @Override
    public List<ContactsAddress> getEosAddress() {
        final List<ContactsAddress> hdAddressList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.CONTACTS_ADDRESS + " where coin_type = ?";
        this.execQueryLoop(sql, new String[]{Integer.toString(ContactsAddress.COIN_TYPE_EOS)}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                hdAddressList.add(formatAddress(c));
                return null;
            }
        });
        return hdAddressList;
    }

    @Override
    public List<ContactsAddress> getEosTokenAddress() {
        final List<ContactsAddress> hdAddressList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.CONTACTS_ADDRESS + " where coin_type = ?";
        this.execQueryLoop(sql, new String[]{Integer.toString(ContactsAddress.COIN_TYPE_EOS_TOKEN)}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                hdAddressList.add(formatAddress(c));
                return null;
            }
        });
        return hdAddressList;
    }


    @Override
    public String getAliasWithAddress(String coin, String address) {
        if (coin == null || "".equals(coin)) {
            coin = "null";
        }
        String sql = "select alias from " + AbstractDb.Tables.CONTACTS_ADDRESS + " where coin = ? and address = ?";

        final String[] alias = {""};
        this.execQueryOneRecord(sql, new String[]{coin, address}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                int idColumn = c.getColumnIndex("alias");
                if (idColumn != -1) {
                    alias[0] = c.getString(idColumn);
                }
                return null;
            }
        });
        return alias[0];
    }

    private ContactsAddress formatAddress(ICursor c) {
        String address = "";
        String coin = "";
        String alias = "";
        String contractAddress = "";
        int coinType = 0;
        int idColumn = c.getColumnIndex(AbstractDb.ContactsAddressColumns.ADDRESS);
        if (idColumn != -1) {
            address = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.ContactsAddressColumns.COIN);
        if (idColumn != -1) {
            coin = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.ContactsAddressColumns.ALIAS);
        if (idColumn != -1) {
            alias = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.ContactsAddressColumns.CONTRACT_ADDRESS);
        if (idColumn != -1) {
            contractAddress = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.ContactsAddressColumns.COIN_TYPE);
        if (idColumn != -1) {
            coinType = c.getInt(idColumn);
        }
        return new ContactsAddress(coin, address, alias, contractAddress, coinType);
    }
}
