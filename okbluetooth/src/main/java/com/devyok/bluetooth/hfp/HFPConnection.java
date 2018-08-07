package com.devyok.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;

import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.connection.AbstractBluetoothConnection;
import com.devyok.bluetooth.connection.BluetoothConnection.State;
import com.devyok.bluetooth.connection.BluetoothConnectionException;
import com.devyok.bluetooth.connection.BluetoothConnectionTimeoutException;
import com.devyok.bluetooth.utils.BluetoothUtils;

import java.util.UUID;
/**
 * @author wei.deng
 */
public final class HFPConnection extends AbstractBluetoothConnection<HFPConnectionImpl>{

	private int companyid;
	volatile boolean isConnected = false;
	
	public HFPConnection(){
		this(BluetoothUtils.UNKNOW);
	}
	
	public HFPConnection(int companyid) {
		this(null,null,companyid);
	}
	
	public boolean checkCompanyId(){
		if(companyid == BluetoothUtils.UNKNOW){
			return false;
		}
		return true;
	}
	
	public void setCompanyid(int companyid) {
		this.companyid = companyid;
	}
	
	public HFPConnection(UUID sppuuid, BluetoothDevice connectedDevice) {
		this(sppuuid,connectedDevice,-1);
	}
	
	public HFPConnection(UUID sppuuid, BluetoothDevice connectedDevice,int companyid) {
		super(sppuuid, connectedDevice);
		this.companyid = companyid;
	}

	@Override
	public void connect(UUID sppuuid, BluetoothDevice connectedDevice) throws BluetoothConnectionException,
			BluetoothConnectionTimeoutException, BluetoothException {
		try {
			HFPConnectionImpl impl = HFPConnectionImpl.connect(companyid);
			this.transactCore = impl;
			isConnected = true;
			this.currentState = State.CONNECTED;
		} catch(Exception e){
			isConnected = false;
			throw e;
		}
	}
	
	@Override
	public void disconnect() {
		try {
			super.disconnect();
		} finally {
			isConnected = false;
		}
	}
	
	@Override
	public boolean isConnected() {
		return isConnected;
	}

}
