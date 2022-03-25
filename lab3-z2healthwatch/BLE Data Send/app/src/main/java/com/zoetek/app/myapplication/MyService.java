package com.zoetek.app.myapplication;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by DFM on 2017/1/6.
 */

public class MyService extends Service {
    private MyBinder mBinder;
    public IBinder onBind(Intent intent) {
        mBinder = new MyBinder();
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
    public String getServiceName(){
        return MyService.class.getSimpleName();
    }
    public class MyBinder extends Binder {
        public MyService getService(){
            return MyService.this;
        }
    }
}
