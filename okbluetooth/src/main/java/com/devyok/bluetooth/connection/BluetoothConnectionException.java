package com.devyok.bluetooth.connection;

import com.devyok.bluetooth.base.BluetoothException;

/**
 * @author wei.deng
 */
public class BluetoothConnectionException extends BluetoothException {

	public BluetoothConnectionException(String message) {
		super(message);
	}
	
	public BluetoothConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public BluetoothConnectionException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;

}