package com.devyok.bluetooth.connection;


import com.devyok.bluetooth.base.StateInformation;
/**
 * @author deng.wei
 */
public abstract class BluetoothConnectionStateListener {

	public static final BluetoothConnectionStateListener EMTPY = new BluetoothConnectionStateListener() {
	}; 
	
	public void onConnecting(StateInformation stateInformation) {
	}

	public void onConnected(StateInformation stateInformation) {
	}

	public void onDisconnected(StateInformation stateInformation) {
	}
	
	public void onDisconnecting(StateInformation stateInformation) {
	}
	
	public void onError(StateInformation stateInformation) {
	}
	
}
