package com.devyok.bluetooth.connection;

import com.devyok.bluetooth.base.BluetoothException;


/**
 * @author wei.deng
 */
public class BluetoothConnectionTimeoutException extends BluetoothException {

	private static final long serialVersionUID = 1L;
	public BluetoothConnectionTimeoutException(String message) {
		super(message);
	}
	
	public BluetoothConnectionTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
