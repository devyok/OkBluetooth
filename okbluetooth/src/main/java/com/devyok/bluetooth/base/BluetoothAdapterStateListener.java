package com.devyok.bluetooth.base;

/**
 * @author deng.wei
 */
public abstract class BluetoothAdapterStateListener{

	public static final BluetoothAdapterStateListener EMPTY = new BluetoothAdapterStateListener() {};

	/**
	 * 正在开启
	 */
	public void onOpening(StateInformation stateInformation){}
	/**
	 * 已开启
	 */
	public void onOpened(StateInformation stateInformation){}
	/**
	 * 关闭中
	 */
	public void onCloseing(StateInformation stateInformation){}
	/**
	 * 已关闭
	 */
	public void onClosed(StateInformation stateInformation){}

}
