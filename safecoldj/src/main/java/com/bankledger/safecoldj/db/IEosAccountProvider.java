package com.bankledger.safecoldj.db;

import com.bankledger.safecoldj.core.EosAccount;

import java.util.List;

/**
 * Created by zm on 2018/11/13.
 */
public interface IEosAccountProvider {

    void addEosAccount(EosAccount account);

    void addOrUpdateEosAccount(EosAccount account);

    void updateEosAccountAvailable(EosAccount account);

    boolean isExistPubkey(String ownerPubKey, String activePubKey);

    EosAccount queryCreateEosAccount();

    EosAccount queryEosAccount(EosAccount.OpType opType);

    EosAccount queryAvailableEosAccount();

    EosAccount matchEos(String matchStr);

}
