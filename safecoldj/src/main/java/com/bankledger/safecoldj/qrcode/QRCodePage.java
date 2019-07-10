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

package com.bankledger.safecoldj.qrcode;


import com.bankledger.safecoldj.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QRCodePage implements Comparable<QRCodePage> {

    private static final Logger log = LoggerFactory.getLogger(QRCodePage.class);
    private int pageIndex;
    private int pageCount;
    private String content;

    public static QRCodePage formatQrCodePage(String text) {
        QRCodePage page = new QRCodePage();
        String[] strArray = QRCodeUtil.splitPage(text);
        if (Utils.isInteger(strArray[0]) && Utils.isInteger(strArray[1])) {
            int length = strArray[0].length() + strArray[1].length() + 2;
            page.setPageCount(Integer.valueOf(strArray[0]) + 1);
            page.setPageIndex(Integer.valueOf(strArray[1]));
            page.setContent(text.substring(length));
        } else {
            page.setContent(text);
        }
        return page;
    }

    public static boolean isQrCodePage(String text) {
        String[] strArray = QRCodeUtil.splitPage(text);
        if (strArray.length >= 2) {
            if (Utils.isInteger(strArray[0]) && Utils.isInteger(strArray[1])) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "QRCodePage{" +
                "pageIndex=" + pageIndex +
                ", pageCount=" + pageCount +
                ", content='" + content + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return pageIndex == ((QRCodePage) o).getPageIndex() &&
                pageCount == ((QRCodePage) o).getPageCount() &&
                content.equals(((QRCodePage) o).getContent());
    }

    @Override
    public int compareTo(QRCodePage qrCodePage) {
        if (pageIndex > qrCodePage.getPageIndex()) {
            return 1;
        } else if (pageIndex == qrCodePage.getPageIndex()) {
            return 0;
        } else {
            return -1;
        }
    }
}
