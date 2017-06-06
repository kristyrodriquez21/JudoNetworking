package com.github.kubatatami.judonetworking.activities;

import android.app.Activity;
import android.os.Bundle;

import com.github.kubatatami.judonetworking.CallbacksConnector;
import com.github.kubatatami.judonetworking.batches.Batch;
import com.github.kubatatami.judonetworking.callbacks.BaseCallback;
import com.github.kubatatami.judonetworking.callbacks.Callback;
import com.github.kubatatami.judonetworking.stateful.StatefulBatch;
import com.github.kubatatami.judonetworking.stateful.StatefulCache;
import com.github.kubatatami.judonetworking.stateful.StatefulCallback;
import com.github.kubatatami.judonetworking.stateful.StatefulController;

/**
 * Created by Kuba on 01/07/15.
 */
public abstract class JudoActivity extends Activity implements StatefulController {

    private String id;

    private boolean active;

    static String UNIQUE_ACTIVITY_ID = "UNIQUE_ACTIVITY_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            id = JudoActivity.generateId(this);
        } else {
            id = savedInstanceState.getString(JudoActivity.UNIQUE_ACTIVITY_ID);
        }
    }

    static String generateId(Object object) {
        return object.hashCode() + "" + object.getClass().hashCode() + System.currentTimeMillis();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(UNIQUE_ACTIVITY_ID, id);
    }

    @Override
    public String getWho() {
        return "activity_" + id;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            StatefulCache.removeAllStatefulCallbacks(getWho());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        active = true;
        onConnectCallbacks(new CallbacksConnector(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        active = false;
        StatefulCache.removeAllControllersCallbacks(getWho());
    }

    @Override
    public void onConnectCallbacks(CallbacksConnector connector) {
    }

    protected boolean connectCallback(BaseCallback<?>... callbacks) {
        return StatefulCache.connectControllerCallbacks(this, callbacks);
    }

    protected boolean connectCallback(int id, BaseCallback<?> callback) {
        return StatefulCache.connectControllerCallback(this, id, callback);
    }

    protected <T> StatefulCallback<T> generateCallback(Callback<T> callback) {
        return new StatefulCallback<>(this, callback, active);
    }

    protected <T> StatefulCallback<T> generateCallback(int id, Callback<T> callback) {
        return new StatefulCallback<>(this, id, callback, active);
    }

    protected <T> StatefulBatch<T> generateCallback(Batch<T> batch) {
        return new StatefulBatch<>(this, batch, active);
    }

    protected <T> StatefulBatch<T> generateCallback(int id, Batch<T> batch) {
        return new StatefulBatch<>(this, id, batch, active);
    }

    public void cancelRequest(int id) {
        StatefulCache.cancelRequest(this, id);
    }
}