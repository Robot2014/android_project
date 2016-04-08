package com.mstarsemi.mynetworkplayerapplication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class OTAService extends Service {

    private final String TAG = "OTAService";
    static OTAService mInstance = null;

    static OTAService GetInstance(){
        if(mInstance== null){
            mInstance = new OTAService();
        }
        return mInstance;
    }


    private OTAService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }
}
