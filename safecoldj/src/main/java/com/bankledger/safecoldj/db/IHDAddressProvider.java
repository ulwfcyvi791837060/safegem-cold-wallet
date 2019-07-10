/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bankledger.safecoldj.db;

import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.core.AbstractHD;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.In;
import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.db.impl.base.ICursor;
import com.google.common.base.Function;

import java.util.List;

import javax.annotation.Nullable;

public interface IHDAddressProvider {

    void addHDAddress(List<HDAddress> hdAccountAddresses);

    List<HDAddress> getHDAddressList(Currency currency);

    List<HDAddress> getHDAddressListWithAvailable(Currency currency, HDAddress.AvailableState availableState);

    List<HDAddress> getHDAddressesForInputs(List<In> ins, String coin);

    int getAddressCount(AbstractHD.PathType pathType, String coin);

    int getAvailableAddressCount(Currency currency);

    void replaceAddressAlias(String coin, String address, String alias);

    boolean checkAddressExist(String coin, String address);

    void setHDAddressAvailable(String coin, String address, HDAddress.AvailableState state);

    HDAddress getHDAddressWithAddress(String coin, String address);

    List<HDAddress> matchHDAddress(String matchStr);

}
