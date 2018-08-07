package com.devyok.bluetooth.base;


import android.os.Handler;
import android.os.HandlerThread;

/**
 * @author deng.wei
 */
public final class BluetoothAndroidThread extends HandlerThread implements Executor{
    private static BluetoothAndroidThread sInstance;
    private static Handler sHandler;

    private BluetoothAndroidThread() {
        super("bt.runtime.base", android.os.Process.THREAD_PRIORITY_DEFAULT);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            sInstance = new BluetoothAndroidThread();
            sInstance.start();
            sHandler = new Handler(sInstance.getLooper());
        }
    }

    public static BluetoothAndroidThread get() {
        synchronized (BluetoothAndroidThread.class) {
            ensureThreadLocked();
            return sInstance;
        }
    }

    static Handler getHandler() {
        synchronized (BluetoothAndroidThread.class) {
            ensureThreadLocked();
            return sHandler;
        }
    }
    
    public boolean execute(Runnable runnable) {
    	if(runnable!=null){
    		synchronized (BluetoothAndroidThread.class) {
        		getHandler().post(runnable);
        	}
        	return true;
    	}
    	return false;
    }
    
}
