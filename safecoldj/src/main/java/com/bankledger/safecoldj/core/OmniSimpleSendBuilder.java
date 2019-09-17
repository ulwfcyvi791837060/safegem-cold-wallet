package com.bankledger.safecoldj.core;


import com.bankledger.safecoldj.SafeColdSettings;

import org.spongycastle.util.encoders.Hex;

/**
 * @author bankledger
 * @time 2018/11/15 11:04
 */
public class OmniSimpleSendBuilder {
    private static final long USDT_VALUE = SafeColdSettings.DEV_DEBUG ? 2 : 31;
    private static final String OMNI = "omni";

    private static String createSimpleSendHex(long amount) {
        return String.format("00000000%08x%016x", USDT_VALUE, amount);
    }

    public static byte[] completeSimpleSend(long amount) {
        byte[] omni = OMNI.getBytes();
        String omniHex = Hex.toHexString(omni);
        return Hex.decode(omniHex + createSimpleSendHex(amount));
    }
}
