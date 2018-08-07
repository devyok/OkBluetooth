package com.devyok.bluetooth.message;

import java.util.UUID;

import com.devyok.bluetooth.connection.BluetoothConnection.Protocol;

import android.bluetooth.BluetoothDevice;

/**
 * @author wei.deng
 */
public class BluetoothMessage<DataType> {
	private String id; 

	private long time;
	private DataType dataBody;
	private Protocol protocol;
	private BluetoothDevice bluetoothDevice;
	
	public BluetoothMessage(){
		this.id = UUID.randomUUID().toString();
	}
	
	public static <DataType> BluetoothMessage<DataType> obtain(DataType data,Protocol protocol){
		return obtain(System.currentTimeMillis(), data,protocol);
	}
	
	public static <DataType> BluetoothMessage<DataType> obtain(long time,DataType data,Protocol protocol){
		BluetoothMessage<DataType> message = new BluetoothMessage<DataType>();
		message.time = time;
		message.dataBody = data;
		message.protocol = protocol;
		return message;
	}
	
	public BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}

	public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
	}

	public long getTime(){
		return time;
	}
	
	public DataType getBodyData(){
		return dataBody;
	}
	
	public Protocol getProtocol(){
		return protocol;
	}
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		BluetoothMessage<DataType> msg = (BluetoothMessage<DataType>) obj;
		
		if(this.id.equals(msg.id)){
			return true;
		}
		
		return false;
	}
	
}
