package com.orbismc.townyPolitics.utils;

public interface TestCallback {
    void onSuccess(String message);
    void onFailure(String message, Exception e);
}
