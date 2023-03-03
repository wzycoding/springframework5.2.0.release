package com.wzy.service;

public class StaticFactory {

    public static UserService getUser() {
        return new UserService();
    }
}
