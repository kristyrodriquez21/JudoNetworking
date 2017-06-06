package com.github.kubatatami.judonetworking.observers;

import android.content.Context;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import com.github.kubatatami.judonetworking.callbacks.DefaultCallback;
import com.github.kubatatami.judonetworking.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ObservableWrapper<T> extends DefaultCallback<T> {

    protected T object = null;

    protected final Handler handler = new Handler(Looper.getMainLooper());

    protected final List<WrapperObserver<T>> observers = new ArrayList<>();

    protected ObservableWrapperListener<T> listener = null;

    protected boolean notifyInUiThread = true;

    protected long dataSetTime = 0;

    protected long updateTime = 0;

    protected boolean notifyOnError = true;

    protected boolean notifyOnNull = false;

    protected boolean forceUpdateOnNetworkStateChange = false;

    protected boolean checkNetworkState = false;

    protected boolean checkUpdateOnGet = false;

    protected boolean firstNetworkState = true;

    protected boolean setOnlyWhenDifferentHash = false;

    protected long period = 0;

    protected Timer timer;

    protected NetworkUtils.NetworkStateListener networkStateListener = new NetworkUtils.NetworkStateListener() {
        @Override
        public void onNetworkStateChange(NetworkInfo activeNetworkInfo) {
            if (firstNetworkState) {
                firstNetworkState = false;
                return;
            }
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                if (forceUpdateOnNetworkStateChange) {
                    forceUpdate();
                } else {
                    checkUpdate();
                }
            }
        }
    };

    @Override
    public void onSuccess(T result) {
        set(result, !notifyOnError);
    }

    @Override
    public void onFinish() {
        if (notifyOnError && !getAsyncResult().isCancelled()) {
            notifyObservers();
        }
    }

    protected final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            listener.onUpdate(ObservableWrapper.this);
        }
    };

    public boolean isLoaded() {
        return true;
    }

    public AddObserverResult addObserver(WrapperObserver<T> observer) {
        return addObserver(observer, false);
    }

    public AddObserverResult addObserver(WrapperObserver<T> observer, boolean notify) {
        boolean add = true;
        if (listener != null) {
            add = listener.onAddObserver(this, observer);
        }
        if (add) {
            observers.add(observer);
            if (notify) {
                T obj = get();
                if (obj != null || notifyOnNull) {
                    observer.onUpdate(obj);
                }
            }
        }
        return new AddObserverResult<>(this, observer);
    }

    public void deleteObserver(WrapperObserver<T> observer) {
        boolean delete = true;
        if (listener != null) {
            delete = listener.onDeleteObserver(this, observer);
        }
        if (delete) {
            if (!observers.remove(observer)) {
                throw new IllegalStateException("This is not the observer added earlier.");
            }
        }
    }

    public void startCheckUpdateOnChangeNetworkState(Context context) {
        startCheckUpdateOnChangeNetworkState(context, false);
    }

    public void startCheckUpdateOnChangeNetworkState(Context context, boolean forceUpdate) {
        if (!checkNetworkState) {
            forceUpdateOnNetworkStateChange = forceUpdate;
            NetworkUtils.addNetworkStateListener(context, networkStateListener);
        }
    }

    public void stopCheckUpdateOnChangeNetworkState(Context context) {
        if (checkNetworkState) {
            NetworkUtils.removeNetworkStateListener(context, networkStateListener);
        }
    }

    public boolean isCheckUpdateOnGet() {
        return checkUpdateOnGet;
    }

    public ObservableWrapper<T> setCheckUpdateOnGet(boolean checkUpdateOnGet) {
        this.checkUpdateOnGet = checkUpdateOnGet;
        return this;
    }

    public boolean isSet() {
        return object != null;
    }

    public T get() {
        if (checkUpdateOnGet) {
            checkUpdate();
        }
        if (listener != null) {
            listener.onGet(this);
        }
        return object;
    }

    public void checkUpdate() {
        if (listener != null && !isDataActual()) {
            handler.post(updateRunnable);
        }
    }

    public void forceUpdate() {
        if (listener != null) {
            handler.post(updateRunnable);
        }
    }

    public boolean isDataActual() {
        return updateTime == 0 || System.currentTimeMillis() - getDataSetTime() <= updateTime;
    }

    public boolean set(T object) {
        return set(object, true);
    }

    public boolean set(T object, boolean notify) {
        return set(object, notify, System.currentTimeMillis());
    }

    public boolean set(T object, boolean notify, long dataSetTime) {
        if (setOnlyWhenDifferentHash && isSet() && object != null && object.hashCode() == this.object.hashCode()) {
            return false;
        }

        this.dataSetTime = dataSetTime;
        this.object = object;


        if (notify) {
            notifyObservers();
        }
        if (listener != null) {
            listener.onSet(this, object);
        }
        return true;
    }

    public void set(T object, ObservableTransaction transaction) {
        transaction.add(this, object);
    }

    public ObservableWrapper<T> startCheckUpdatePeriodically(long period) {
        startCheckUpdatePeriodically(period, false);
        return this;
    }

    public ObservableWrapper<T> startCheckUpdatePeriodically(long period, final boolean forceUpdate) {
        if (timer != null && this.period == period) {
            return this;
        }
        stopCheckUpdatePeriodically();
        timer = new Timer(true);
        this.period = period;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (forceUpdate) {
                    forceUpdate();
                } else {
                    checkUpdate();
                }
            }
        }, period, period);
        return this;
    }

    public ObservableWrapper<T> stopCheckUpdatePeriodically() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        return this;
    }

    public long getDataSetTime() {
        return dataSetTime;
    }

    public void notifyObservers() {
        notifyObservers(null);
    }

    public void notifyObservers(ObservableTransaction transaction) {
        if (transaction == null) {
            if (isSet() || notifyOnNull) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = observers.size() - 1; i >= 0; i--) {
                            observers.get(i).onUpdate(object);
                        }
                    }
                };

                if (Looper.getMainLooper().getThread().equals(Thread.currentThread()) || !notifyInUiThread) {
                    runnable.run();
                } else {
                    handler.post(runnable);
                }
            }
        } else {
            transaction.add(this, object);
        }
    }

    public ObservableWrapper<T> connect(ObservableController controller, WrapperObserver<T> observer) {
        return connect(controller, observer, false);
    }

    public ObservableWrapper<T> connectAndNotify(ObservableController controller, WrapperObserver<T> observer) {
        return connect(controller, observer, true);
    }

    private ObservableWrapper<T> connect(ObservableController controller, WrapperObserver<T> observer, boolean notify) {
        addObserver(observer, notify).deleteOnDestroy(controller);
        return this;
    }

    public ObservableWrapper<T> setListener(ObservableWrapperListener<T> listener) {
        this.listener = listener;
        return this;
    }

    public int getObserversCount() {
        return observers.size();
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public ObservableWrapper<T> setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public boolean isNotifyOnNull() {
        return notifyOnNull;
    }

    public ObservableWrapper<T> setNotifyOnNull(boolean notifyOnNull) {
        this.notifyOnNull = notifyOnNull;
        return this;
    }

    public ObservableWrapper<T> setOnlyWhenDifferentHash(boolean setOnlyWhenDifferentHash) {
        this.setOnlyWhenDifferentHash = setOnlyWhenDifferentHash;
        return this;
    }

    public ObservableWrapper<T> setNotifyInUiThread(boolean notifyInUiThread) {
        this.notifyInUiThread = notifyInUiThread;
        return this;
    }

    public boolean isNotifyOnError() {
        return notifyOnError;
    }

    public ObservableWrapper<T> setNotifyOnError(boolean notifyOnError) {
        this.notifyOnError = notifyOnError;
        return this;
    }
}