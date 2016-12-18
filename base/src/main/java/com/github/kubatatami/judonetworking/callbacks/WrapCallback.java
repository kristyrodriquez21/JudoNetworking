package com.github.kubatatami.judonetworking.callbacks;

import com.github.kubatatami.judonetworking.builders.WrapBuilder;

public class WrapCallback<T, S> extends WrapBuilder.LambdaCallback<T, S> {

    public WrapCallback() {
    }

    public WrapCallback(Callback<S> outerCallback) {
        super(outerCallback);
    }

    public WrapCallback(WrapBuilder<T, S, ?> builder) {
        super(builder);
    }

    public static class Builder<T, S> extends WrapBuilder<T, S, Builder<T, S>> {

        public Builder() {
        }

        public Builder(Callback<S> outerCallback) {
            super(outerCallback);
        }

        public WrapCallback<T, S> build() {
            return new WrapCallback<>(this);
        }

    }
}
