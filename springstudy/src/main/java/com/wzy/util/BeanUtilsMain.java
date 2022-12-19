package com.wzy.util;

import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;

public class BeanUtilsMain {
    public static void main(String[] args) {
        PropertyDescriptor[] propertyDescriptors =
                BeanUtils.getPropertyDescriptors(Goods.class);

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            System.out.println(propertyDescriptor.getName() + ", " +
                    propertyDescriptor.getReadMethod() + ", " +
                    propertyDescriptor.getWriteMethod());
        }
    }
}
