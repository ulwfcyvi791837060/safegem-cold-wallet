package com.bankledger.safecold.asynctask;

/**
 * @author bankledger
 * @time 2018/7/31 13:49
 */
public interface IDoInBackground<Params, Progress, Result> {
    Result doInBackground(IPublishProgress<Progress> publishProgress, Params... params);
}
