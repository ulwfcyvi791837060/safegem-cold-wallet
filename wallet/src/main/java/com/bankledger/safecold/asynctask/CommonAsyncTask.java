package com.bankledger.safecold.asynctask;

import android.os.AsyncTask;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * AsyncTask封装
 * @author bankledger
 * @time 2018/7/31 11:22
 */
public class CommonAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements IPublishProgress<Progress> {

    private IPreExecute iPreExecute;
    private IProgressUpdate<Progress> iProgressUpdate;
    private IDoInBackground<Params, Progress, Result> iDoInBackground;
    private IPostExecute<Result> iPostExecute;
    private IViewActive iViewActive;

    public CommonAsyncTask() {
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (iPreExecute != null) iPreExecute.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        super.onProgressUpdate(values);
        if (iProgressUpdate != null) iProgressUpdate.onProgressUpdate(values);
    }

    @Override
    protected Result doInBackground(Params... params) {
        return iDoInBackground == null ? null : iDoInBackground.doInBackground(this, params);
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        if (iPostExecute != null && iViewActive == null || iViewActive.isViewActive())
            iPostExecute.onPostExecute(result);
    }

    @Override
    public void showProgress(Progress... values) {
        publishProgress(values);
    }

    @SafeVarargs
    public final AsyncTask<Params, Progress, Result> start(boolean singleThread, Params... params) {
        if (singleThread) {
            return super.execute(params);
        } else {
            return super.executeOnExecutor(THREAD_POOL_EXECUTOR, params);
        }

    }

    public static class Builder<Params, Progress, Result> {

        private final CommonAsyncTask commonAsyncTask;

        public Builder() {
            commonAsyncTask = new CommonAsyncTask();
        }

        public Builder<Params, Progress, Result> setIPreExecute(IPreExecute iPreExecute) {
            commonAsyncTask.iPreExecute = iPreExecute;
            return this;
        }

        public Builder<Params, Progress, Result> setIProgressUpdate(IProgressUpdate<Progress> iProgressUpdate) {
            commonAsyncTask.iProgressUpdate = iProgressUpdate;
            return this;
        }

        public Builder<Params, Progress, Result> setIDoInBackground(IDoInBackground<Params, Progress, Result> iDoInBackground) {
            commonAsyncTask.iDoInBackground = iDoInBackground;
            return this;
        }

        public Builder<Params, Progress, Result> setIPostExecute(IPostExecute<Result> iPostExecute) {
            commonAsyncTask.iPostExecute = iPostExecute;
            return this;
        }


        public Builder<Params, Progress, Result> setIViewActive(IViewActive iViewActive) {
            commonAsyncTask.iViewActive = iViewActive;
            return this;
        }

        public AsyncTask<Params, Progress, Result> startOnSingleThread(Params... params) {
            return commonAsyncTask.start(true, params);
        }

        public AsyncTask<Params, Progress, Result> start(Params... params) {
            return commonAsyncTask.start(false, params);
        }

    }
}
