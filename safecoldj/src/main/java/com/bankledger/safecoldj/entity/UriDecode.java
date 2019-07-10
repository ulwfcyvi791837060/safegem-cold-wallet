package com.bankledger.safecoldj.entity;

import com.bankledger.safecoldj.utils.GsonUtils;

import java.util.Map;

/**
 * @author bankledger
 * @time 2018/9/14 11:11
 */
public class UriDecode {

    public String scheme;
    public String path;

    public Map<String, String> params;

    public UriDecode() {
    }

    public UriDecode(String scheme, String path, Map<String, String> params) {
        this.scheme = scheme;
        this.path = path;
        this.params = params;
    }

    @Override
    public String toString() {
        return GsonUtils.toString(this);
    }
}
