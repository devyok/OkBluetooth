package com.devyok.bluetooth.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.devyok.bluetooth.OkBluetooth;
/**
 * @author wei.deng
 */
public abstract class BaseBluetoothStateChangedListener extends BroadcastReceiver{

	private volatile boolean mCalled;
	
	public BaseBluetoothStateChangedListener(){
	}
	
	public abstract boolean onChanged(StateInformation information);
	public abstract String[] actions();
	
	void check(){
		
	}
	
	public boolean isStarted() {
		return mCalled;
	}
	
	public void startListener() throws BluetoothException {
		
		check();
		String[] actions = actions();
		
		if(actions == null || actions.length == 0){
			throw new BluetoothException("listener actions is empty");
		}
		
		synchronized (this) {
			IntentFilter stateFilter = new IntentFilter();
			for(int i = 0;i < actions.length;i++){
				stateFilter.addAction(actions[i]);
			}
			OkBluetooth.getContext().registerReceiver(this, stateFilter);
			mCalled = true;
		}
	}

	public void stopListener() {
		
		check();
		synchronized (this) {
			if (isStarted()) {
				try {
					OkBluetooth.getContext().unregisterReceiver(this);
				} finally {
					recycle();
				}
			}
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		StateInformation information = StateInformation.toInformation(intent);
		
		onChanged(information);
		
	}
	
	public void recycle(){
		mCalled = false;
	}

}
