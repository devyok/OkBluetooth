package com.devyok.bluetooth.spp;

import com.devyok.bluetooth.connection.BluetoothConnection.Protocol;
import com.devyok.bluetooth.message.BluetoothMessage;

import android.bluetooth.BluetoothDevice;
/**
 * @author wei.deng
 */
public interface SPPBluetoothMessageParser<DataType> {

	public static final SPPBluetoothMessageParser<String> DEFAULT = new SPPBluetoothMessageParser<String>() {
		
		final StringBuilder readMessage = new StringBuilder();
		
		@Override
		public BluetoothMessage<String>[] parse(byte[] buffer, int readCount,Protocol protocol,BluetoothDevice device) {
			String readed = new String(buffer, 0, readCount);
			readMessage.append(readed);
			if (readed.contains("\n")) {
				BluetoothMessage<String> message = BluetoothMessage.obtain(new String(readMessage.toString()), protocol);
				readMessage.setLength(0);
				return new BluetoothMessage[]{message};
			}
			return null;
		}
		
	};
	
	public BluetoothMessage<DataType>[] parse(byte[] buffer, int readCount, Protocol protocol, BluetoothDevice device);
	
}
