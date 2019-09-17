package com.bankledger.safecoldj.utils;

import com.bankledger.safecoldj.Safe;
import com.bankledger.safecoldj.core.Out;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by zm on 2019/1/7.
 */

public class SafeUtils {

    public static final int RESERVE_HEADER_VERSION = 1; //预留字段头部版本编号 1、2、3、4 （安资、安付、安聊、安投）

    public static final String SAFE_APP_ID = "cfe2450bf016e2ad8130e4996960a32e0686c1704b62a6ad02e49ee805a9b288"; //安资APP_ID

    public static final int CMD_REGISTER = 100; //注册应用命令

    public static final int CMD_ISSUE = 200; //发行命令

    public static final int CMD_ADD_ISSUE = 201; //追加发行命令

    public static final int CMD_TRANSFER = 202; //转让命令

    public static final int CMD_DESTORY = 203; //销毁命令

    public static final int CMD_ASSET_CHANGE = 204; //资产找零

    public static final int CMD_GRANT_CANDY = 205; //发放糖果命令

    public static final int CMD_GET_CANDY = 206; //领取糖果命令

    //封装转账Protos
    public static Safe.CommonData getTransferProtos(long transferAmount, String assetId) {
        String remarks = "Asset transfer";
        byte[] versionByte = new byte[2];
        Utils.uint16ToByteArrayLE(1, versionByte, 0);
        return Safe.CommonData.newBuilder()
                .setVersion(ByteString.copyFrom(versionByte))
                .setAssetId(ByteString.copyFrom(SafeUtils.assetIdToHash256(assetId)))
                .setAmount(transferAmount)
                .setRemarks(ByteString.copyFromUtf8(remarks))
                .build();
    }

    //封装找零Protos
    public static Safe.CommonData getAssetChangeProtos(long assetChangeAmount, String assetId) {
        String remarks = "Asset change";
        byte[] versionByte = new byte[2];
        Utils.uint16ToByteArrayLE(1, versionByte, 0);
        return Safe.CommonData.newBuilder()
                .setVersion(ByteString.copyFrom(versionByte))
                .setAssetId(ByteString.copyFrom(SafeUtils.assetIdToHash256(assetId)))
                .setAmount(assetChangeAmount)
                .setRemarks(ByteString.copyFromUtf8(remarks))
                .build();
    }

    /**
     * 资产Id转hash
     *
     * @param assetId
     * @return
     */
    public static byte[] assetIdToHash256(String assetId) {
        Sha256Hash sha256Hash = new Sha256Hash(assetId);
        return Utils.reverseBytes(sha256Hash.getBytes());
    }

    //封装预留字段
    public static String serialReserve(int appCommand, String appId, byte[] protos) throws Exception {
        ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream();
        byte[] safeFlag = "safe".getBytes();
        stream.write(safeFlag);
        Utils.uint16ToByteStreamLE(RESERVE_HEADER_VERSION, stream);
        stream.write(assetIdToHash256(appId));
        Utils.uint32ToByteStreamLE(appCommand, stream);
        stream.write(protos);
        return stream.toString("ISO-8859-1");
    }

    public static long getRealDecimals(long decimals) {
        long ret = 1;
        for (int i = 0; i < decimals; i++) {
            ret = ret * 10;
        }
        return ret;
    }

}
