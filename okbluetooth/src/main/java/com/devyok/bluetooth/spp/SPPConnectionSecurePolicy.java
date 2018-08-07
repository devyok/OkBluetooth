package com.devyok.bluetooth.spp;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.devyok.bluetooth.connection.BluetoothConnectionException;
/**
 * @author wei.deng
 */
public class SPPConnectionSecurePolicy extends AbstractSPPConnection {

	static int exeCount = 0;
	
	public SPPConnectionSecurePolicy(){
		this(null,null);
	}
	
	public SPPConnectionSecurePolicy(UUID sppuuid, BluetoothDevice connectedDevice) {
		super(sppuuid, connectedDevice);
	}

	static final String TAG = SPPConnectionSecurePolicy.class.getSimpleName();
	
	@Override
	public void connect(UUID sppuuid,BluetoothDevice connectedDevice) throws BluetoothConnectionException{
		
		Log.i(TAG, "[devybt sppconnection] SPPConnectionSecurePolicy start try connect , isCancel("+isCancel()+") , sppuuid = " + sppuuid);
		
		try {
			transactCore = connectedDevice.createRfcommSocketToServiceRecord(sppuuid);
		} catch (IOException e) {
			Log.i(TAG, "[devybt sppconnection] createRfcommSocketToServiceRecord IOException");
			throw new BluetoothConnectionException("createRfcommSocketToServiceRecord exception", e);
		}
		
		// test code
//		if(exeCount % 2 == 0){
//			try {
//				Thread.sleep(24*1000);
//			} catch (InterruptedException e1) {
//				e1.printStackTrace();
//			}
//		}
//		
//		exeCount++;
		
		try {
			if(!isCancel() && transactCore != null){
				transactCore.connect();
				if(transactCore.isConnected()){
					onConnected();
				}
			}
		} catch (IOException e) {
			closeTransactCore();
			Log.i(TAG, "[devybt sppconnection] try connect IOException");
			throw new BluetoothConnectionException("spp connect exception" , e);
		} 
	}

}
