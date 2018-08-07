package com.devyok.bluetooth.spp;

import com.devyok.bluetooth.connection.BluetoothConnection.Protocol;
import com.devyok.bluetooth.message.BluetoothMessage;

import android.bluetooth.BluetoothDevice;
/**
 * @author wei.deng
 */
public interface SPPMessageParser<DataType> {

	public BluetoothMessage<DataType>[] parse(byte[] buffer, int readCount, Protocol protocol, BluetoothDevice device);
	
}
