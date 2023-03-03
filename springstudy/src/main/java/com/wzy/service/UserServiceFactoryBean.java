package com.wzy.service;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author wzy
 */
public class UserServiceFactoryBean implements FactoryBean<UserService> {
    @Override
    public UserService getObject() throws Exception {
        return new UserService();
    }

    @Override
    public Class<?> getObjectType() {
        return UserService.class;
    }
}
