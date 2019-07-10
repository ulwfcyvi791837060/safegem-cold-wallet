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

package com.bankledger.safecoldj.db.impl;

import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.In;
import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.core.OutPoint;
import com.bankledger.safecoldj.db.IHDAddressProvider;
import com.bankledger.safecoldj.utils.Utils;
import com.google.common.base.Function;

import com.bankledger.safecoldj.core.AbstractHD;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.impl.base.ICursor;
import com.bankledger.safecoldj.db.impl.base.IDb;
import com.bankledger.safecoldj.exception.AddressFormatException;
import com.bankledger.safecoldj.utils.Base58;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public abstract class AbstractHDAddressProvider extends AbstractProvider implements IHDAddressProvider {

    @Override
    public void addHDAddress(List<HDAddress> hdAccountAddresses) {
        String sql = "insert into hd_addresses(address, address_index, path_type, pub_key, alias, coin, available_state) values(?, ?, ?, ?, ?, ?, ?)";
        IDb writeDb = this.getWriteDb();
        writeDb.beginTransaction();
        for (HDAddress hdAccountAddress : hdAccountAddresses) {
            this.execUpdate(writeDb, sql, new String[]{
                    hdAccountAddress.getAddress()
                    , Integer.toString(hdAccountAddress.getAddressIndex())
                    , Integer.toString(hdAccountAddress.getPathType().getValue())
                    , Base58.encode(hdAccountAddress.getPubKey())
                    , hdAccountAddress.getAlias()
                    , hdAccountAddress.getCoin()
                    , Integer.toString(hdAccountAddress.getAvailableState().getValue())
            });
        }
        writeDb.endTransaction();
    }

    @Override
    public List<HDAddress> getHDAddressList(Currency currency) {
        final List<HDAddress> hdAddressList = new ArrayList<>();
        String sql = "select * from hd_addresses where coin = ? and path_type = ? order by address_index";
        this.execQueryLoop(sql, new String[]{currency.coin, Integer.toString(currency.pathType.getValue())}, new Function<ICursor, Void>() {
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
    public List<HDAddress> getHDAddressListWithAvailable(Currency currency, HDAddress.AvailableState availableState) {
        final List<HDAddress> hdAddressList = new ArrayList<>();
        String sql = "select address, address_index, path_type, pub_key, alias, coin, available_state" +
                " from hd_addresses where coin = ? and path_type = ? and available_state = ? order by address_index asc";
        this.execQueryLoop(sql, new String[]{currency.coin, Integer.toString(currency.pathType.getValue()), Integer.toString(availableState.getValue())}, new Function<ICursor, Void>() {
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
    public List<HDAddress> getHDAddressesForInputs(List<In> ins, String coin) {
        final List<HDAddress> hdAddressList = new ArrayList<>();
        for (In in : ins) {
            String sql = "select a.address, a.address_index, a.path_type, a.pub_key, a.alias, a.coin, a.available_state" +
                    " from hd_addresses a, outs b" +
                    " where a.address = b.out_address" +
                    " and b.tx_hash = ? and b.out_sn = ?" +
                    " and a.coin = ?";
            OutPoint outPoint = in.getOutpoint();
            this.execQueryOneRecord(sql, new String[]{
                    Utils.hashToString(outPoint.getTxHash()),
                    Integer.toString(outPoint.getOutSn()),
                    coin}, new Function<ICursor, Void>() {
                @Nullable
                @Override
                public Void apply(@Nullable ICursor c) {
                    hdAddressList.add(formatAddress(c));
                    return null;
                }
            });
        }
        return hdAddressList;
    }


    private HDAddress formatAddress(ICursor c) {
        String address = null;
        byte[] pubs = null;
        AbstractHD.PathType pathType = null;
        int addressIndex = 0;
        String coin = "";
        String alias = "";
        HDAddress.AvailableState availableState = HDAddress.AvailableState.STATE_AVAILABLE;
        int idColumn = c.getColumnIndex(AbstractDb.HDAddressesColumns.ADDRESS);
        if (idColumn != -1) {
            address = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.HDAddressesColumns.ADDRESS_INDEX);
        if (idColumn != -1) {
            addressIndex = c.getInt(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.HDAddressesColumns.PATH_TYPE);
        if (idColumn != -1) {
            pathType = AbstractHD.getTernalRootType(c.getInt(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.HDAddressesColumns.PUB_KEY);
        if (idColumn != -1) {
            try {
                pubs = Base58.decode(c.getString(idColumn));
            } catch (AddressFormatException e) {
                e.printStackTrace();
            }
        }
        idColumn = c.getColumnIndex(AbstractDb.HDAddressesColumns.ALIAS);
        if (idColumn != -1) {
            alias = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.HDAddressesColumns.COIN);
        if (idColumn != -1) {
            coin = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.HDAddressesColumns.AVAILABLE_STATE);
        if (idColumn != -1) {
            availableState = c.getInt(idColumn) == HDAddress.AvailableState.STATE_AVAILABLE.getValue()
                    ? HDAddress.AvailableState.STATE_AVAILABLE : HDAddress.AvailableState.STATE_DISABLED;
        }

        return new HDAddress(address, pubs, pathType, addressIndex, coin, alias, availableState);
    }

    @Override
    public int getAddressCount(AbstractHD.PathType pathType, String coin) {
        String sql = "select ifnull(count(address),0) count from hd_addresses where path_type=? and coin=?";
        final int[] count = {0};
        this.execQueryOneRecord(sql, new String[]{Integer.toString(pathType.getValue()), coin}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                int idColumn = c.getColumnIndex("count");
                if (idColumn != -1) {
                    count[0] = c.getInt(idColumn);
                }
                return null;
            }
        });
        return count[0];
    }

    @Override
    public int getAvailableAddressCount(Currency currency) {
        int type = currency.pathType.getValue();
        String sql = "select ifnull(count(address),0) count from hd_addresses where available_state = 0 and path_type=? and coin=?";
        final int[] count = {0};
        this.execQueryOneRecord(sql, new String[]{Integer.toString(type), currency.coin}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                int idColumn = c.getColumnIndex("count");
                if (idColumn != -1) {
                    count[0] = c.getInt(idColumn);
                }
                return null;
            }
        });
        return count[0];
    }

    @Override
    public void replaceAddressAlias(String coin, String address, String alias) {
        String sql = "update hd_addresses set alias= ? where coin = ? and address = ?";
        this.execUpdate(sql, new String[]{alias, coin, address});
    }

    @Override
    public boolean checkAddressExist(String coin, String address) {
        final List<HDAddress> hdAddressList = new ArrayList<>();
        String sql = "select address, address_index, path_type, pub_key, alias, coin, available_state" +
                " from hd_addresses where coin = ? and address = ?";
        this.execQueryLoop(sql, new String[]{coin, address}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                hdAddressList.add(formatAddress(c));
                return null;
            }
        });
        return hdAddressList.size() > 0;
    }

    @Override
    public void setHDAddressAvailable(String coin, String address, HDAddress.AvailableState state) {
        String sql = "update hd_addresses set available_state = ? where coin = ? and  address = ?";
        this.execUpdate(sql, new String[]{Integer.toString(state.getValue()), coin, address});
    }


    @Override
    public HDAddress getHDAddressWithAddress(String coin, String address) {
        final List<HDAddress> hdAddressList = new ArrayList<>();
        String sql = "select address, address_index, path_type, pub_key, alias, coin, available_state" +
                " from hd_addresses where coin = ? and address = ?";
        this.execQueryLoop(sql, new String[]{coin, address}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                hdAddressList.add(formatAddress(c));
                return null;
            }
        });

        if (hdAddressList.size() > 0) {
            return hdAddressList.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<HDAddress> matchHDAddress(String matchStr) {
        final List<HDAddress> hdAddressList = new ArrayList<>();
        String sql = "select address, address_index, path_type, address, pub_key, alias, coin, available_state" +
                " from hd_addresses where available_state = 0 and (address like ? or coin like ? or alias like ?)";
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
}
