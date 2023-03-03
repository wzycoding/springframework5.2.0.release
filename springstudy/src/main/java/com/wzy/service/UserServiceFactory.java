package com.wzy.service;

public class UserServiceFactory {
    public UserService getUser() {
        return new UserService();
    }
}
