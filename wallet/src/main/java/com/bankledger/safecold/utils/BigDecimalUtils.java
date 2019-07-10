package com.bankledger.safecold.utils;


import android.util.Log;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * 由于Java的简单类型不能够精确的对浮点数进行运算，这个工具类提供精 确的浮点数运算，包括加减乘除和四舍五入。
 */
public class BigDecimalUtils {

    // 精度
    public static final int BTC_SCALE = 8;

    // 默认除法运算精度
    private static final int DEF_DIV_SCALE = 10;

    // BTC精度
    private static final String BTC2SATOSHI = "100000000";

    // 这个类不能实例化
    private BigDecimalUtils() {
    }

    /**
     * 提供精确的加法运算。
     *
     * @param v1 被加数
     * @param v2 加数
     * @return 两个参数的和
     */
    public static String add(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.add(b2).toPlainString();
    }

    /**
     * 提供精确的减法运算。
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 两个参数的差
     */
    public static String sub(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.subtract(b2).toPlainString();
    }

    /**
     * v1是否大于v2
     */
    public static boolean greaterThan(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        double b3 = b1.subtract(b2).doubleValue();
        return b3 > 0;
    }

    /**
     * v1是否大于等于v2
     */
    public static boolean greaterThanEquals(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.subtract(b2).doubleValue() >= 0;
    }

    /**
     * 提供精确的乘法运算。
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @return 两个参数的积
     */
    public static String mul(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.multiply(b2).toPlainString();
    }

    /**
     * 提供精确的乘法运算。
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @return 两个参数的积
     */
    public static Long mulToLong(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.multiply(b2).longValue();
    }

    /**
     * 提供（相对）精确的除法运算，当发生除不尽的情况时，精确到 小数点以后10位，以后的数字四舍五入。
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     */
    public static String div(String v1, String v2) {
        return div(v1, v2, DEF_DIV_SCALE);
    }

    /**
     * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指 定精度，以后的数字四舍五入。
     *
     * @param v1    被除数
     * @param v2    除数
     * @param scale 表示表示需要精确到小数点以后几位。
     * @return 两个参数的商
     */
    public static String div(String v1, String v2, int scale) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    /**
     * 比较字符串大小
     *
     * @param v1
     * @param v2
     * @return
     */
    public static int compareTo(String v1, String v2) {
        BigDecimal b1 = new BigDecimal(v1);
        BigDecimal b2 = new BigDecimal(v2);
        return b1.compareTo(b2);
    }

    public static String unitToBtc(String satoshi) {
        return div(satoshi, BTC2SATOSHI, BTC_SCALE);
    }

    public static String formatSatoshi2Btc(String satoshi) {
        return StringUtil.subZeroAndDot(unitToBtc(satoshi));
    }

    public static String unitToSatoshi(String btc) {
        return mul(btc, BTC2SATOSHI);
    }

    public static long unitToSatoshiL(String btc) {
        BigDecimal b1 = new BigDecimal(btc);
        BigDecimal b2 = new BigDecimal(BTC2SATOSHI);
        return b1.multiply(b2).longValue();
    }

    public static String formatShowAmount(long amount, long decimals){
        long realDecimals = getRealDecimals(decimals);
        BigDecimal b1 = new BigDecimal(amount);
        BigDecimal b2 = new BigDecimal(realDecimals);
        return b1.divide(b2, (int)decimals, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    /**
     * 格式化资产金额
     * @param amount
     * @return
     */
    public static long formatAssetAmount(String amount, long decimals) {
        long realDecimals = getRealDecimals(decimals);
        BigDecimal b1 = new BigDecimal(amount);
        BigDecimal b2 = new BigDecimal(realDecimals);
        return b1.multiply(b2).longValue();
    }

    /**
     * 真实的精度
     * @param decimals
     * @return
     */
    public static long getRealDecimals(long decimals) {
        long ret = 1;
        for (int i = 0; i < decimals; i++) {
            ret = ret * 10;
        }
        return ret;
    }

}
