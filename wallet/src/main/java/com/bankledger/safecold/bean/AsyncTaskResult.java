package com.bankledger.safecold.bean;

/**
 * @author bankledger
 * @time 2018/8/10 14:43
 */
public class AsyncTaskResult<T> {
    private boolean success;
    private T result;
    private Exception exception;

    public AsyncTaskResult() {
        this.success = true;
    }

    public AsyncTaskResult(T result) {
        this.success = true;
        this.result = result;
    }

    public AsyncTaskResult(boolean success, T result) {
        this.success = success;
        this.result = result;
    }

    public AsyncTaskResult(Exception exception) {
        this.success = false;
        this.exception = exception;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getResult() {
        return result;
    }

    public Exception getException() {
        return exception;
    }
}
