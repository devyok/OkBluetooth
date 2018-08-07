package com.devyok.bluetooth.base;
/**
 * @author deng.wei
 */
//adapter
public abstract class BluetoothService implements BluetoothServiceLifecycle{

	public boolean init() throws BluetoothException{
		
		return false;
	}
	
	public boolean destory(){
		return false;
	}
	
}
