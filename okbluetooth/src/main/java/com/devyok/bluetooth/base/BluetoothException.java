package com.devyok.bluetooth.base;
/**
 * 
 * @author wei.deng
 *
 */
public class BluetoothException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BluetoothException(String message, Throwable cause) {
		super(message, cause);
	}

	public BluetoothException(String message) {
		super(message);
	}

	public BluetoothException(Throwable cause) {
		super(cause);
	}
	
	

}
