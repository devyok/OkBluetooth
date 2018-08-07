package com.devyok.bluetooth.spp;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.devyok.bluetooth.connection.BluetoothConnectionException;
/**
 * @author wei.deng
 */
public class SppConnectionLoopChannelPolicy extends AbstractSPPConnection {
	
	static final String TAG = SppConnectionLoopChannelPolicy.class.getSimpleName();
	
	int maxChannel = 3;
	
	public SppConnectionLoopChannelPolicy(UUID sppuuid,BluetoothDevice connectedDevice,int maxChannel){
		super(sppuuid,connectedDevice);
		this.maxChannel = maxChannel;
	}
	
	public SppConnectionLoopChannelPolicy(){
		this(null,null,10);
	}
	
	public SppConnectionLoopChannelPolicy(UUID sppuuid,BluetoothDevice connectedDevice){
		super(sppuuid,connectedDevice);
	}
	
	@Override
	public void connect(UUID sppuuid,BluetoothDevice connectedDevice) throws BluetoothConnectionException {
		BluetoothConnectionException excp = null;
		
		for (int channel = 1; channel < maxChannel; channel++) {
			try {
				
				if(isCancel()){
					break;
				}
				
				Method m = connectedDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
				transactCore = (BluetoothSocket) m.invoke(connectedDevice, channel);

				if(transactCore != null){
					Log.i(TAG, "[devybt sppconnection] LoopChannelSppConnectPolicy start try connect channel("+channel+") , isCancel("+isCancel()+")");
					
					transactCore.connect();
					
					Log.i(TAG,"[devybt sppconnection] connect channel("+channel+") result" + (transactCore!=null && transactCore.isConnected() ? "success" : "fail"));
					
				} else {
					Log.i(TAG,"[devybt sppconnection] connect channel("+channel+") bluetoothSocket(null)");
				}
				
			} catch (IOException e) {
				Log.i(TAG, "[devybt sppconnection] IOException try connect channel("+channel+") fail");
				if(excp == null) {
					excp = new BluetoothConnectionException("try connect channel fail",e);
				}
				
				closeTransactCore();
				
			} catch (Exception e){
				Log.i(TAG, "[devybt sppconnection] Exception try connect channel("+channel+") fail");
				throw new BluetoothConnectionException("try connect channel exception",e);
			}
		}
		throw excp == null ? new BluetoothConnectionException("LoopChannelSppConnectPolicy fail") : excp;
	}
	
}
