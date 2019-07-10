package com.bankledger.safecold.asynctask;

/**
 * @author bankledger
 * @time 2018/7/31 13:51
 */
public interface IPublishProgress<Progress> {
    void showProgress(Progress...values);
}
