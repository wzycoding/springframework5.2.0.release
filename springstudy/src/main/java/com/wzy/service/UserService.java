package com.wzy.service;


import com.wzy.entity.Cat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author wzy
 */
@Service
public class UserService {

    public String hello(String name) {
        return "Hello " + name;
    }
}
