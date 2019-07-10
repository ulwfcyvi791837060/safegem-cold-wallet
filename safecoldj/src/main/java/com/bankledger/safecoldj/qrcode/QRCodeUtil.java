package com.bankledger.safecoldj.qrcode;

import com.bankledger.protobuf.bean.TransColdWalletInfo;
import com.bankledger.protobuf.utils.ProtoUtils;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.utils.GsonUtils;
import com.bankledger.safecoldj.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 二维码传输协议
 *
 * @author zm
 */
public class QRCodeUtil {

    public static final String QR_CODE_COLON = ":";
    private static final String QR_CODE_VALUE = "=";//
    private static final String QR_CODE_QUESTION_E = "?";
    public static final String QR_CODE_UNDERLINE = "_";
    private static final String QR_CODE_PlUS = "&";
    public static final String QR_CODE_SPLIT = "/";
    private static final String QR_CODE_PAGE_SPLIT = "@";

    public static String[] split(String text) {
        return text.split(QR_CODE_SPLIT);
    }

    public static String[] splitPage(String text) {
        return text.split(QR_CODE_PAGE_SPLIT);
    }

    public static String encodeUri(UriDecode src) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(src.scheme);
        stringBuilder.append(QR_CODE_COLON);
        stringBuilder.append(src.path);
        if (src.params != null && src.params.size() > 0) {
            stringBuilder.append(QR_CODE_QUESTION_E);
            for (String key : src.params.keySet()) {
                stringBuilder.append(key);
                stringBuilder.append(QR_CODE_VALUE);
                stringBuilder.append(src.params.get(key));
                stringBuilder.append(QR_CODE_PlUS);
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    public static UriDecode decodeUri(String uri) throws Exception {
        UriDecode d = new UriDecode();
        if (uri.contains("?")) {
            String[] arrDecode = uri.split("\\?");
            String[] arrS = arrDecode[0].split(":");
            if (arrS.length == 2) {
                d.scheme = arrS[0];
                d.path = arrS[1];
            } else {
                return null;
            }
            if (arrDecode.length == 2) {
                String[] arrP = arrDecode[1].split("&");
                Map<String, String> params = new HashMap<>(arrP.length);
                for (String p : arrP) {
                    String[] arrK = p.split("=");
                    if (arrK.length == 2) {
                        params.put(arrK[0], arrK[1]);
                    }
                }
                d.params = params;
            }
            return d;
        } else {
            String[] arrS = uri.split(":");
            if (arrS.length == 2) {
                d.scheme = arrS[0];
                d.path = arrS[1];
            } else {
                return null;
            }
            return d;
        }
    }

    public static List<String> encodePage(String text) {
        List<String> pageList = new ArrayList<>();
        int num = getNumOfQrCodeString(text.length());
        if (num <= 1) {
            pageList.add(text);
            return pageList;
        }
        int pageSize = QRQuality.Normal.getQuality();
        for (int i = 0; i < num; i++) {
            int start = i * pageSize;
            int end = (i + 1) * pageSize;
            if (end > text.length()) {
                end = text.length();
            }
            String splitStr = text.substring(start, end);
            String pageString;
            pageString = Integer.toString(num - 1) + QR_CODE_PAGE_SPLIT + Integer.toString(i) + QR_CODE_PAGE_SPLIT;
            pageList.add(pageString + splitStr);
        }
        return pageList;
    }

    public static String decodePage(List<QRCodePage> pageList) throws Exception {
        List<QRCodePage> tempList = new ArrayList<>();
        for (QRCodePage item : pageList) {
            tempList.add(item);
        }
        Collections.sort(tempList);
        StringBuilder mBuilder = new StringBuilder();
        for (QRCodePage qrCodePage : tempList) {
            mBuilder.append(qrCodePage.getContent());
        }
        return mBuilder.toString();
    }

    public static List<String> decodeSeedPassword(String text) {
        if (text.startsWith("*SEED:")) {
            String subStr = text.substring("*SEED:".length(), text.length());
            String arr[] = subStr.split(" ");
            return Arrays.asList(arr);
        }
        return null;
    }

    public static String encodeWalletInfo(String walletName, String walletSerialNumber, String divNumber) {
        return ProtoUtils.encodeColdWalletInfo(
                new TransColdWalletInfo(walletSerialNumber, walletName, divNumber)
        );
    }

    public static boolean scanIsDone(List<QRCodePage> pageList) {
        if (pageList == null || pageList.size() == 0) return false;
        boolean ret;
        int pageCount = 0;
        Set set = new HashSet<>();
        for (QRCodePage item : pageList) {
            set.add(item.getPageIndex());
            pageCount = item.getPageCount();
        }
        if (set.size() == pageCount) {
            ret = true;
        } else {
            ret = false;
        }
        return ret;
    }

    private static int getNumOfQrCodeString(int length) {
        int quality = QRQuality.Normal.getQuality();
        return (length + quality - 1) / quality;
    }

    public enum QRQuality {

        Normal(320), LOW(210);
        private int quality;

        QRQuality(int quality) {
            this.quality = quality;
        }

        private int getQuality() {
            return this.quality;
        }
    }
}
