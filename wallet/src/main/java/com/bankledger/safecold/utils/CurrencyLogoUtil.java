package com.bankledger.safecold.utils;

import com.bankledger.safecold.R;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecoldj.SafeColdSettings;

/**
 * @author bankledger
 * @time 2018/10/18 20:14
 */
public class CurrencyLogoUtil {

    public static int getResourceByCoin(String coin, int type) {
        if (type == ConvertViewBean.TYPE_CURRENCY) {
            if (SafeColdSettings.BTC.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_btc;
            } else if (SafeColdSettings.LTC.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_ltc;
            } else if (SafeColdSettings.SAFE.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_safe;
            } else if (SafeColdSettings.DASH.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_dash;
            } else if (SafeColdSettings.QTUM.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_qtum;
            } else if (SafeColdSettings.BCH.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_bch;
            } else if (SafeColdSettings.BSV.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_bchsv;
            } else if (SafeColdSettings.BTG.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_btg;
            } else if (SafeColdSettings.FTO.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_fto;
            } else if (SafeColdSettings.USDT.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_usdt;
            } else {
                return 0;
            }
        } else if (type == ConvertViewBean.TYPE_ETH_TOKEN) {
            if (SafeColdSettings.ETH.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_eth;
            } else if (SafeColdSettings.ETC.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_etc;
            } else {
                return R.mipmap.logo_eth_token;
            }
        } else if (type == ConvertViewBean.TYPE_EOS_COIN) {
            if (SafeColdSettings.EOS.equalsIgnoreCase(coin)) {
                return R.mipmap.logo_eos;
            } else {
                return R.mipmap.logo_eos_token;
            }
        } else if (type == ConvertViewBean.TYPE_SAFE_ASSET) {
            return R.mipmap.logo_safe;
        } else {
            return 0;
        }
    }
}
