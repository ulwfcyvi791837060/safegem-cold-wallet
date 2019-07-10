package com.bankledger.safecoldj.entity;

import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.utils.GsonUtils;

/**
 * 地址
 * @author bankledger
 * @time 2018/9/10 09:48
 */
public class Address implements Comparable<Address> {

    private static final int TYPE_HD_ADDRESS = 0;
    private static final int TYPE_ETHTOKEN = 1;
    private static final int TYPE_CONTACTS_ADDRESS = 2;
    private static final int TYPE_EOS = 3;

    public int type;
    public HDAddress hdAddress;
    public ETHToken ethToken;
    public ContactsAddress contactsAddress;
    public EosAccount eosAccount;


    public static Address convertHDAddress(HDAddress hdAddress) {
        Address address = new Address();
        address.type = TYPE_HD_ADDRESS;
        address.hdAddress = hdAddress;
        return address;
    }

    public static Address convertETHToken(ETHToken ethToken) {
        Address address = new Address();
        address.type = TYPE_ETHTOKEN;
        address.ethToken = ethToken;
        return address;
    }

    public static Address convertContactsAddress(ContactsAddress contactsAddress) {
        Address address = new Address();
        address.type = TYPE_CONTACTS_ADDRESS;
        address.contactsAddress = contactsAddress;
        return address;
    }

    public static Address convertEosAccount(EosAccount eosAccount) {
        Address address = new Address();
        address.type = TYPE_EOS;
        address.eosAccount = eosAccount;
        return address;
    }

    public boolean isHDAddress() {
        return type == TYPE_HD_ADDRESS;
    }

    public boolean isETHToken() {
        return type == TYPE_ETHTOKEN;
    }

    public boolean isContactsAddress() {
        return type == TYPE_CONTACTS_ADDRESS;
    }

    public boolean isEos() {
        return type == TYPE_EOS;
    }

    public String getCoin() {
        return isHDAddress() ? hdAddress.getCoin() : isETHToken() ? ethToken.name : isContactsAddress() ? contactsAddress.getCoin() : isEos() ? SafeColdSettings.EOS : "";
    }

    @Override
    public int compareTo(Address o) {
        return getCoin().compareTo(o.getCoin());
    }

    @Override
    public String toString() {
        return GsonUtils.toString(this);
    }

}
