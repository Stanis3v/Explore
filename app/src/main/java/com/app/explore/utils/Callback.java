package com.app.explore.utils;

public interface Callback<T> {

    void onSuccess(T result);

    void onError(String msg);

    void onReject(String msg);

}
