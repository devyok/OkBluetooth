package com.devyok.bluetooth.spp;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.connection.AbstractBluetoothConnection;
import com.devyok.bluetooth.connection.BluetoothConnection.State;
import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * @author wei.deng
 */
public abstract class AbstractSPPConnection extends AbstractBluetoothConnection<BluetoothSocket> {

	static final String TAG = AbstractSPPConnection.class.getSimpleName();
	
	protected SPPMessageReceiver messageReceiver;
	
	public AbstractSPPConnection(UUID sppuuid,BluetoothDevice connectedDevice){
		super(sppuuid,connectedDevice);
	}
	
	@Override
	public boolean isConnected(){
		return transactCore!=null ? transactCore.isConnected() : false;
	}
	
	@Override
	public State getState(){
		return BluetoothUtils.getBluetoothSocketState(this.transactCore);
	}
	
	public void onConnected(){
		
		if(messageReceiver!=null){
			messageReceiver.stopReceiver();
		}
		
		SPPBluetoothMessageParser<String> parser = OkBluetooth.getSppMessageParser();
		messageReceiver = new SPPMessageReceiver(this.transactCore,parser == null ? new DefaultSPPMessageParser() : parser,this.getBluetoothDevice());
		messageReceiver.start();
	}
	
	@Override
	protected void closeTransactCore() {
		super.closeTransactCore();
		
		if(messageReceiver!=null){
			messageReceiver.closeStream();
			messageReceiver = null;
		}
		
	}
	
}
