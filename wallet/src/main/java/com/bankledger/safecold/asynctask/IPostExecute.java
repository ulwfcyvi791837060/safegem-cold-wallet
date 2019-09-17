package com.bankledger.safecold.asynctask;

/**
 * @author bankledger
 * @time 2018/7/31 13:48
 */
public interface IPostExecute<Result> {
    void onPostExecute(Result result);
}
