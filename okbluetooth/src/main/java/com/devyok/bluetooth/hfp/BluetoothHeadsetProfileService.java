package com.devyok.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;

import com.devyok.bluetooth.base.BluetoothProfileService;
/**
 * @author wei.deng
 */
public interface BluetoothHeadsetProfileService extends BluetoothProfileService {
	
	public interface BluetoothHeadsetAudioStateListener {
		public void onAudioConnected(BluetoothDevice bluetoothDevice, BluetoothHeadsetProfileService service);
		public void onAudioDisconnected(BluetoothDevice bluetoothDevice, BluetoothHeadsetProfileService service);
		public void onAudioConnecting(BluetoothDevice bluetoothDevice, BluetoothHeadsetProfileService service);
	}
	
	public void registerAudioStateChangedListener(BluetoothHeadsetAudioStateListener lis); 
	public void unregisterAudioStateChangedListener(BluetoothHeadsetAudioStateListener lis);
	
	public boolean isAudioConnected(final BluetoothDevice device);
	
	public boolean disconnectAudio();
	
	public boolean connectAudio();
	
	public int getAudioState(final BluetoothDevice device);
	
	public boolean isAudioOn();
	
}
