package com.devyok.bluetooth.message;
/**
 * @author wei.deng
 */
public interface BluetoothMessageHandler<DataType> {

	public void handle(BluetoothMessage<DataType> bluetoothMessage);
	
}
