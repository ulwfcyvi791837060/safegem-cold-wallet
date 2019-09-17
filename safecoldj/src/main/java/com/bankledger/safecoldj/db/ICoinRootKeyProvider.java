package com.bankledger.safecoldj.db;

/**
 * @author bankledger
 * @time 2018/9/8 14:29
 */
public interface ICoinRootKeyProvider {

    void addRootKey(String coin, byte[] rootKey);

    byte[] getRootKey(String coin);
}
