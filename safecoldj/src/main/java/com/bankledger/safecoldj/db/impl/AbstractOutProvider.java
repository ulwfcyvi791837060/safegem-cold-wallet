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

import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.IOutProvider;
import com.bankledger.safecoldj.db.impl.base.ICursor;
import com.bankledger.safecoldj.db.impl.base.IDb;
import com.bankledger.safecoldj.utils.GsonUtils;
import com.bankledger.safecoldj.utils.Utils;
import com.google.common.base.Function;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import jdk.nashorn.internal.parser.JSONParser;


public abstract class AbstractOutProvider extends AbstractProvider implements IOutProvider {

    public static final Logger log = LoggerFactory.getLogger(AbstractOutProvider.class);

    public static Out applyCursorOut(ICursor c) {
        Out outItem = new Out();
        int idColumn = c.getColumnIndex(AbstractDb.OutsColumns.TX_HASH);
        if (idColumn != -1) {
            outItem.setTxHash(c.getString(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.OUT_SN);
        if (idColumn != -1) {
            outItem.setOutSn(c.getInt(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.COIN);
        if (idColumn != -1) {
            outItem.setCoin(c.getString(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.OUT_VALUE);
        if (idColumn != -1) {
            outItem.setOutValue(c.getLong(idColumn));
        }
        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.OUT_STATUS);
        if (idColumn != -1) {
            outItem.setOutStatus(c.getInt(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.MUL_TYPE);
        if (idColumn != -1) {
            outItem.setMulType(c.getInt(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.OUT_ADDRESS);
        if (idColumn != -1) {
            outItem.setOutAddress(c.getString(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.UN_LOCK_HEIGHT);
        if (idColumn != -1 && !c.isNull(idColumn)) {
            outItem.setUnLockHeight(c.getLong(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.OutsColumns.RESERVE);
        if (idColumn != -1 && !c.isNull(idColumn)) {
            outItem.setReserve(c.getString(idColumn));
        }
        return outItem;
    }

    @Override
    public void clearOuts(String coin) {
        IDb db = getWriteDb();
        db.beginTransaction();
        this.execUpdate(db, "delete from " + AbstractDb.Tables.OUTS + " where coin = ? and reserve = ?", new String[]{coin, ""});
        db.endTransaction();
    }

    @Override
    public void clearOuts(String coin, String assetId) {
        IDb db = getWriteDb();
        db.beginTransaction();
        this.execUpdate(db, "delete from " + AbstractDb.Tables.OUTS + " where coin = ? and reserve like ?", new String[]{coin, "%" + assetId + "%"});
        db.endTransaction();
    }

    @Override
    public void addOut(Out out) {
        IDb db = getWriteDb();
        db.beginTransaction();
        this.insertOutToDb(db, out);
        db.endTransaction();
    }

    @Override
    public void addOut(Out out, SafeAsset safeAsset) {
        IDb db = getWriteDb();
        db.beginTransaction();
        this.insertOutToDb(db, out, safeAsset);
        db.endTransaction();
    }

    @Override
    public List<Out> getUnspendOutsWithCoin(String coin) {
        final List<Out> outList = new ArrayList<>();
        String sql = "select tx_hash, out_sn, coin, out_value, out_status, mul_type, out_address, un_lock_height, reserve from outs where coin = ? and out_status = ? and reserve = ?";
        this.execQueryLoop(sql, new String[]{coin, Integer.toString(Out.OUT_STATUS_UNSPEND), ""}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                outList.add(applyCursorOut(c));
                return null;
            }
        });
        return outList;
    }

    @Override
    public List<Out> getUnspendOutsWithCoinSafeAsset(String coin, String assetId) {
        final List<Out> outList = new ArrayList<>();
        String sql = "select tx_hash, out_sn, coin, out_value, out_status, mul_type, out_address, un_lock_height, reserve from outs where coin = ? and out_status = ? and reserve like ?";
        this.execQueryLoop(sql, new String[]{coin, Integer.toString(Out.OUT_STATUS_UNSPEND), "%" + assetId + "%"}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                outList.add(applyCursorOut(c));
                return null;
            }
        });
        return outList;
    }

    @Override
    public List<Out> getUnspendOutsWithAddress(String address) {
        final List<Out> outList = new ArrayList<>();
        String sql = "select tx_hash, out_sn, coin, out_value, out_status, mul_type, out_address, un_lock_height, reserve from outs where out_address = ? and out_status = ? and reserve = ?";
        this.execQueryLoop(sql, new String[]{address, Integer.toString(Out.OUT_STATUS_UNSPEND), ""}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                outList.add(applyCursorOut(c));
                return null;
            }
        });
        return outList;
    }

    @Override
    public List<Out> getUnspendOutsWithAddressSafeAsset(String address, String assetId) {
        final List<Out> outList = new ArrayList<>();
        String sql = "select tx_hash, out_sn, coin, out_value,out_status, mul_type, out_address, un_lock_height, reserve from outs where out_address = ? and out_status = ? and reserve like ?";
        this.execQueryLoop(sql, new String[]{address, Integer.toString(Out.OUT_STATUS_UNSPEND), "%" + assetId + "%"}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                outList.add(applyCursorOut(c));
                return null;
            }
        });
        return outList;
    }

    @Override
    public long getBalanceWithAddress(String address) {
        final long[] sum = {0};
        String unspendOutSql = "select ifnull(sum(out_value),0) sum from outs where out_address = ? and out_status= ? and reserve = ?";
        this.execQueryOneRecord(unspendOutSql, new String[]{address, Integer.toString(Out.OUT_STATUS_UNSPEND), ""}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                if (c != null) {
                    int idColumn = c.getColumnIndex("sum");
                    if (idColumn != -1) {
                        sum[0] = c.getLong(idColumn);
                    }
                }
                return null;
            }
        });
        return sum[0];
    }

    @Override
    public long getBalanceWithAddressSafeAsset(String address, String assetId) {
        final long[] sum = {0};
        String unspendOutSql = "select ifnull(sum(out_value),0) sum from outs where out_address = ? and out_status= ? and reserve like ?";
        this.execQueryOneRecord(unspendOutSql, new String[]{address, Integer.toString(Out.OUT_STATUS_UNSPEND), "%" + assetId + "%"}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                if (c != null) {
                    int idColumn = c.getColumnIndex("sum");
                    if (idColumn != -1) {
                        sum[0] = c.getLong(idColumn);
                    }
                }
                return null;
            }
        });
        return sum[0];
    }


    @Override
    public long getBalanceWithCoin(String coin) {
        final long[] sum = {0};
        String unspendOutSql = "select ifnull(sum(out_value),0) sum from outs where coin = ? and out_status= ? and reserve = ?";
        this.execQueryOneRecord(unspendOutSql, new String[]{coin, Integer.toString(Out.OUT_STATUS_UNSPEND), ""}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                if (c != null) {
                    int idColumn = c.getColumnIndex("sum");
                    if (idColumn != -1) {
                        sum[0] = c.getLong(idColumn);
                    }
                }
                return null;
            }
        });
        return sum[0];
    }

    @Override
    public long getBalanceWithCoinSafeAsset(final String coin, final String assetId) {
        final long[] sum = {0};
        String unspendOutSql = "select ifnull(sum(out_value),0) sum from outs where coin = ? and out_status= ? and reserve like ?";
        this.execQueryOneRecord(unspendOutSql, new String[]{coin, Integer.toString(Out.OUT_STATUS_UNSPEND), "%" + assetId + "%"}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                if (c != null) {
                    int idColumn = c.getColumnIndex("sum");
                    if (idColumn != -1) {
                        sum[0] = c.getLong(idColumn);
                    }
                }
                return null;
            }
        });
        return sum[0];
    }

    @Override
    public void labelOutSpend(String txHash, int outSn) {
        IDb db = getWriteDb();
        String sql = "update " + AbstractDb.Tables.OUTS + " set out_status = ? where tx_hash = ? and out_sn = ?";
        this.execUpdate(db, sql, new String[]{Integer.toString(Out.OUT_STATUS_SPEND), txHash, Integer.toString(outSn)});
    }

    @Override
    public Out getOut(String txHash, int outSn) {
        final Out[] out = new Out[1];
        String sql = "select tx_hash, out_sn, coin, out_value, out_status, mul_type, out_address, un_lock_height, reserve from outs where tx_hash = ? and out_sn = ?";
        this.execQueryOneRecord(sql, new String[]{txHash, Integer.toString(outSn)}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                out[0] = applyCursorOut(c);
                return null;
            }
        });
        return out[0];
    }

    @Override
    public List<String> getSafeAsset() {
        final List<String> outList = new ArrayList<>();
        String sql = "select distinct reserve from outs where coin = ? and reserve <> ?";
        this.execQueryLoop(sql, new String[]{SafeColdSettings.SAFE, ""}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                if (c != null) {
                    String reserve = c.getString(0);
                    if(validate(reserve)){
                        outList.add(c.getString(0));
                    }
                }
                return null;
            }
        });
        return outList;
    }

    @Override
    public SafeAsset getSafeAsset(String assetName) {
        final SafeAsset[] safeAssets = new SafeAsset[1];
        String sql = "select distinct reserve from outs where coin = ? and reserve like ? limit 1";
        this.execQueryLoop(sql, new String[]{SafeColdSettings.SAFE, "%" + assetName + "%"}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                if (c != null) {
                    String reserve = c.getString(0);
                    safeAssets[0] = GsonUtils.getObjFromJSON(reserve, SafeAsset.class);
                }
                return null;
            }
        });
        return safeAssets[0];
    }

    protected abstract void insertOutToDb(IDb db, Out out);

    protected abstract void insertOutToDb(IDb db, Out out, SafeAsset safeAsset);

    public static boolean validate(String jsonStr) {
        JsonElement jsonElement;
        try {
            jsonElement = new JsonParser().parse(jsonStr);
        } catch (Exception e) {
            return false;
        }
        if (jsonElement == null) {
            return false;
        }
        if (!jsonElement.isJsonObject()) {
            return false;
        }
        return true;
    }
}
