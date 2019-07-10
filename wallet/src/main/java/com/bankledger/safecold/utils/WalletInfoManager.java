package com.bankledger.safecold.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.db.CoinRootKeyProvider;
import com.bankledger.safecold.ui.activity.AppUpgradeActivity;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.crypto.hd.DeterministicKey;
import com.bankledger.safecoldj.crypto.hd.HDKeyDerivation;
import com.bankledger.safecoldj.utils.Base58;
import com.bankledger.safecoldj.utils.Utils;

import org.spongycastle.util.encoders.Base64;


/**
 * $desc
 *
 * @author bankledger
 * @time 2018/8/29 15:30
 */
public class WalletInfoManager {

    public static void saveWalletName(String name) {
        SafeColdApplication.appSharedPreferenceUtil.put(Constants.KEY_SP_WALLET_NAME, name);
    }

    public static String getWalletName() {
        return SafeColdApplication.appSharedPreferenceUtil.get(Constants.KEY_SP_WALLET_NAME, "");
    }

    public static boolean hasWalletName() {
        return getWalletName().length() > 0;
    }

    public static String getWalletNumber() {
        byte[] keyByte = CoinRootKeyProvider.getInstance().getRootKey(SafeColdSettings.SAFE);
        DeterministicKey rootKey = HDKeyDerivation.createMasterPubKeyFromExtendedBytes(keyByte);
        String walletNumber = Base58.encode(Utils.sha256hash160(rootKey.getPubKey())).toUpperCase();
        rootKey.wipe();
        return walletNumber;
    }

    public static boolean checkWalletNumber(String WalletNumber) {
        return getWalletNumber().equalsIgnoreCase(WalletNumber);
    }

    //获取冷端的版本号
    public static int getCodeWalletVersionCode() {
        PackageManager manager = SafeColdApplication.mContext.getPackageManager();
        int code = 0;
        try {
            PackageInfo info = manager.getPackageInfo(SafeColdApplication.mContext.getPackageName(), 0);
            code = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return code;
    }

    //获取冷端的版本名
    public static String getCodeWalletVersionName() {
        PackageManager manager = SafeColdApplication.mContext.getPackageManager();
        String name = "";
        try {
            PackageInfo info = manager.getPackageInfo(SafeColdApplication.mContext.getPackageName(), 0);
            name = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return name;
    }

    public static String getBluetoothAddress() {
        String address = android.provider.Settings.Secure.getString(SafeColdApplication.mContext.getContentResolver(), "bluetooth_address");
        if(AppUpgradeActivity.TEST_XIAOMI){
            address = "20:47:DA:FE:35:F3";
        }
        return new String(Base64.encode(address.getBytes()));
    }
}
