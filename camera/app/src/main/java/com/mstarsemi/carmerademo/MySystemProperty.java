package com.mstarsemi.mynetworkplayerapplication;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * Created by scott.cao on 2015/12/4.
 */
public class MySystemProperty {
    private String TAG = "MySystemPrpperty";
    private Class<?> mClassType = null;
    private Method mGetProperty = null;
    private Method mSetProperty = null;
    private static MySystemProperty mInstance = null;

    public static MySystemProperty GetInstance() {
        if (mInstance == null) {
            mInstance = new MySystemProperty();
        }
        return mInstance;
    }

    public String GetProperty(String key) {
        String value = null;
        try {
            value = (String) mGetProperty.invoke(mClassType, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public void SetProperty(String key, String value) {
        try {
            mSetProperty.invoke(mClassType, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mInstance == null) {
            mInstance = new MySystemProperty();
        }
        String[] args = new String[2];
        args[0] ="su";
        args[1] = "setprop ms.mstplayer.fast.start true";
        mInstance.excuteCommand(args);
    }

    public boolean IsMStarPlatform() {
        if (mInstance == null) {
            mInstance = new MySystemProperty();
        }
        String value = mInstance.GetProperty("ro.product.brand");
        if (value.equals("MStar")) {
            Log.d(TAG,"is mstar platform");
            return true;
        }
        Log.d(TAG,"is not mstar platform");
        return false;
    }

    public ChipType GetMStarChipType() {
        if (mInstance == null) {
            mInstance = new MySystemProperty();
        }
        String value = mInstance.GetProperty("ro.hardware");
        switch (value) {
            case "clippers": {
                return ChipType.MSTAR_CLIPPERS;
            }
            case "kano": {
                return ChipType.MSTAR_KANO;
            }
        }
        return ChipType.MSTAR_UNKNOW;
    }

    private MySystemProperty() {
        try {
            if (mClassType == null) {
                mClassType = Class.forName("android.os.SystemProperties");
                mGetProperty = mClassType.getDeclaredMethod("get", String.class);
                mSetProperty = mClassType.getDeclaredMethod("set", String.class, String.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ;
        }
    }

    private String excuteCommand(String[] command) {

        Log.i(TAG, "command:" + command);
        String result = "";
        Runtime r = Runtime.getRuntime();
        try {
            Process mp = null;
            mp = r.exec(command);
            mp.waitFor();//for test
            BufferedReader br = new BufferedReader(new InputStreamReader(mp.getInputStream()));
            String inline;
            while ((inline = br.readLine()) != null) {
                Log.i(TAG, "-----------------inline----------------------" + inline);
                result += inline;
            }
            br.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }
}