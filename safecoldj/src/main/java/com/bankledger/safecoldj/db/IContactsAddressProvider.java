package com.bankledger.safecoldj.db;

import com.bankledger.safecoldj.entity.ContactsAddress;

import java.util.List;

/**
 * @author bankledger
 * @time 2018/9/8 16:30
 */
public interface IContactsAddressProvider {

    void addContactsAddress(ContactsAddress contactsAddress);

    void deleteContactsAddress(ContactsAddress contactsAddress);

    boolean checkContactsAddressExist(ContactsAddress contactsAddress);

    void replaceAddressAlias(ContactsAddress contactsAddress, String alias);

    List<ContactsAddress> matchAddress(String matchStr);

    List<ContactsAddress> getSafeAssetAddressWithCoin(String assetName);

    List<ContactsAddress> getCurrencyAddressWithCoin(String coin);

    List<ContactsAddress> getAddressWithContractAddress(String contractAddress);

    List<ContactsAddress> getEosAddress();

    List<ContactsAddress> getEosTokenAddress();

    String getAliasWithAddress(String coin, String address);
}
