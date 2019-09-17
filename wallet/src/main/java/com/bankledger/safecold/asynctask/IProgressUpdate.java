package com.bankledger.safecold.asynctask;

/**
 * @author bankledger
 * @time 2018/7/31 13:52
 */
public interface IProgressUpdate<Progress> {
    void onProgressUpdate(Progress... values);
}
