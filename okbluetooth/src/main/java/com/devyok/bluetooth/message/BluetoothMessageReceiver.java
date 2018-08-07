package com.devyok.bluetooth.message;
/**
 * @author wei.deng
 */
public abstract class BluetoothMessageReceiver<DataType> {
	
	public abstract boolean onReceive(BluetoothMessage<DataType> message);
	
}
