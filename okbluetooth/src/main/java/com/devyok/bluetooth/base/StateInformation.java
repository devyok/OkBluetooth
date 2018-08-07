package com.devyok.bluetooth.base;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * @author wei.deng
 */
public final class StateInformation {

	static final StateInformation EMPTY = new StateInformation();
	
	private int currentState = BluetoothUtils.UNKNOW;
	private int previousState = BluetoothUtils.UNKNOW;
	private BluetoothDevice device;
	private String broadcastAction = BluetoothUtils.EMPTY_STRING;
	private Intent intent;
	
	private StateInformation(){
	}
	
	public boolean isUnknow(){
		return (currentState == BluetoothUtils.UNKNOW || previousState == BluetoothUtils.UNKNOW);
	}
	
	public boolean isEmpty(){
		return (this == EMPTY) || (isUnknow());
	}
	
	public static StateInformation toInformation(Intent intent){
		
		if(intent == null) return StateInformation.EMPTY;
		
		int currentState = BluetoothUtils.UNKNOW;
		if(intent.hasExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE)){
			currentState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothUtils.UNKNOW);
		}
		
		int previousState = BluetoothUtils.UNKNOW;
		
		if(intent.hasExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE)) {
			previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE, BluetoothUtils.UNKNOW);
		}
		
		BluetoothDevice device = null;
		if(intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
			device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		}
		
		String broadcastAction = intent.getAction();
		return obtain(currentState, previousState, device, broadcastAction,intent);
	}
	
	public static StateInformation obtain(int currentState,int previousState,BluetoothDevice device,String broadcastAction,Intent intent){
		
		StateInformation information = new StateInformation();
		
		information.currentState = currentState;
		information.previousState = previousState;
		information.device = device;
		information.broadcastAction = broadcastAction;
		information.intent = intent;
	
		return information;
	}

	public int getCurrentState() {
		return currentState;
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public String getBroadcastAction() {
		return broadcastAction;
	}

	public int getPreviousState() {
		return previousState;
	}

	public Intent getIntent() {
		return intent;
	}
	
}
