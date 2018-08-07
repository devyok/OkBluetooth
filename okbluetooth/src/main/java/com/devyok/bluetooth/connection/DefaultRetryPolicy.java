package com.devyok.bluetooth.connection;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.devyok.bluetooth.base.BluetoothException;

import java.util.UUID;


/**
 * 默认的重连实现
 * @author wei.deng
 *
 */
public final class DefaultRetryPolicy implements RetryPolicy{

	static final String TAG = DefaultRetryPolicy.class.getSimpleName();

	static final int DEFAULT_RETRY_COUNT = 3;

	private int maxRetryCount = DEFAULT_RETRY_COUNT;

	private int currentRetryCount = 0;

	static int[] TIMEOUT = new int[]{100,500,1000};

	public DefaultRetryPolicy(){
		this(DEFAULT_RETRY_COUNT);
	}

	public DefaultRetryPolicy(int maxCount){
		maxRetryCount = maxCount;
	}

	@Override
	public int getCurrentRetryCount() {
		return currentRetryCount;
	}

	@Override
	public void retry(UUID sppuuid,BluetoothDevice connectedDevice,Exception error) throws BluetoothConnectionException,BluetoothException {

		if(currentRetryCount>=maxRetryCount){
			throw new BluetoothConnectionException("spp retry connect fail",error);
		}

		try {
			Thread.sleep(TIMEOUT[getCurrentRetryCount()]);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		currentRetryCount++;
		Log.i(TAG, "[devybt sppconnection] retryconnect count = " + currentRetryCount);

		BluetoothConnectionImpl sppConnection = BluetoothConnectionImpl.open(sppuuid,connectedDevice,null);
		try {
			sppConnection.connect();
		} catch (BluetoothConnectionException e) {
			Log.i(TAG, "[devybt sppconnection] retryconnect BluetoothConnectionException");

			retry(sppuuid, connectedDevice, error);

		} catch(BluetoothConnectionTimeoutException e){
			Log.i(TAG, "[devybt sppconnection] retryconnect BluetoothConnectionTimeoutException");

			retry(sppuuid, connectedDevice, error);

		} catch (BluetoothException e) {
			throw e;
		}

	}

	@Override
	public void reset() {
		currentRetryCount = 0;
	}

}
