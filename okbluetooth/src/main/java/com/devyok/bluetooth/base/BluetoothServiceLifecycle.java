package com.devyok.bluetooth.base;
/**
 * @author wei.deng
 */
public interface BluetoothServiceLifecycle {

	public boolean init() throws BluetoothException;
	
	public boolean destory();
	
}
