package com.devyok.bluetooth.a2dp;

import android.bluetooth.BluetoothDevice;

import com.devyok.bluetooth.base.BluetoothProfileService;
/**
 * 
 * @author wei.deng
 *
 */
public interface BluetoothA2dpProfileService extends BluetoothProfileService{

	public boolean isA2dpPlaying(BluetoothDevice device);
	
}
