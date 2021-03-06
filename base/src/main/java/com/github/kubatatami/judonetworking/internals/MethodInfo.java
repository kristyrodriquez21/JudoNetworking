package com.github.kubatatami.judonetworking.internals;

import com.github.kubatatami.judonetworking.callbacks.Callback;

import java.lang.reflect.Type;

public class MethodInfo {

    private final Callback<?> callback;
    private final Type resultType;
    private final Object[] args;
    private final Object returnObject;

    public MethodInfo(Callback<?> callback, Type resultType, Object[] args, Object returnObject) {
        this.callback = callback;
        this.resultType = resultType;
        this.args = args;
        this.returnObject = returnObject;
    }

    public Callback<?> getCallback() {
        return callback;
    }

    public Type getResultType() {
        return resultType;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getReturnObject() {
        return returnObject;
    }
}
