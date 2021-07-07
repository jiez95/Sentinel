package com.alibaba.csp.sentinel.property;

/**
 * @author : jiez
 * @date : 2021/7/7 21:48
 */
public interface GlobalPropertyListener<T> extends PropertyListener<T> {

    /**
     * The first time of the {@code value}'s load.
     *
     * @param value the value loaded.
     */
    void globalPropertyUpdate(T value);
}
