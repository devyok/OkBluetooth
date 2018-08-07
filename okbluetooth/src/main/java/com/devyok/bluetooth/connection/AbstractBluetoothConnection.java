package com.devyok.bluetooth.connection;

import java.io.Closeable;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.connection.BluetoothConnection.State;
import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * @author wei.deng
 */
public abstract class AbstractBluetoothConnection<TransactCore extends Closeable> implements Connection<Closeable> {

	static final String TAG = AbstractBluetoothConnection.class.getSimpleName();
	
	protected BluetoothDevice connectedDevice;
	protected UUID sppuuid;
	protected volatile boolean isCancel;
	protected volatile State currentState = State.INIT;
	protected TransactCore transactCore;
	protected long connectionTimeout = 6*1000;
	
	public AbstractBluetoothConnection(UUID sppuuid,BluetoothDevice connectedDevice){
		this.sppuuid = sppuuid;
		this.connectedDevice = connectedDevice;
	}
	
	@Override
	public void connect() throws BluetoothConnectionException,
			BluetoothConnectionTimeoutException, BluetoothException {
		connect(this.sppuuid, this.connectedDevice);
	}
	
	@Override
	public UUID getUuid() {
		return sppuuid;
	}

	@Override
	public BluetoothDevice getBluetoothDevice() {
		return connectedDevice;
	}
	
	@Override
	public long getTimeout() {
		return connectionTimeout;
	}
	
	public void setTimeout(long timeout) {
		this.connectionTimeout = timeout;
	}
	
	@Override
	public void reset() {
		isCancel = false;
		closeTransactCore();
	}
	
	@Override
	public void disconnect() {
		closeTransactCore();
	}
	
	@Override
	public State getState() {
		return this.currentState;
	}
	
	@Override
	public TransactCore getCore() {
		return this.transactCore;
	}
	
	@Override
	public void cancel() {
		Log.i(TAG, "[devybt sppconnection] cancel{"+this.getClass().getSimpleName()+"} enter , transactCore = " + transactCore);
		isCancel = true;
		closeTransactCore();
	}
	
	protected boolean isCancel(){
		return isCancel;
	}
	
	protected void closeTransactCore(){
		try {
			if(this.transactCore instanceof Closeable){
				Closeable closeable = (Closeable) this.transactCore;
				BluetoothUtils.close(closeable);
			}
		} finally {
			this.transactCore = null;
			currentState = State.CLOSED;
		}
	}
	
}
