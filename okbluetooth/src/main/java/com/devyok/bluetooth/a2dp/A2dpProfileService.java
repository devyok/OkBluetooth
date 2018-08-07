package com.devyok.bluetooth.a2dp;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;

import com.devyok.bluetooth.base.BluetoothProfileService;
import com.devyok.bluetooth.base.BluetoothProfileServiceTemplate;
import com.devyok.bluetooth.base.BluetoothRuntimeException;
/**
 * @author wei.deng
 */
public class A2dpProfileService extends BluetoothProfileServiceTemplate implements BluetoothA2dpProfileService{
	//copy from 5.0 settings
	public static final ParcelUuid[] SINK_UUIDS = {
        BluetoothUuid.AudioSink,
        BluetoothUuid.AdvAudioDist,
    };
	
	public static final int PROFILE = BluetoothProfile.A2DP;
	
	public A2dpProfileService() {
		super(PROFILE);
	}
	
	public A2dpProfileService(BluetoothProfileService decorater) {
		super(PROFILE,decorater);
	}
	
	public A2dpProfileService(int profileType) {
		super(profileType);
		throw new BluetoothRuntimeException("not support");
	}

	@Override
	public boolean isA2dpPlaying(final BluetoothDevice device) {
		
		if(realService == null) return false;
		
		return ((BluetoothA2dp)realService).isA2dpPlaying(device);
		
	}

	@Override
	protected ConnectionStateListenerArgs make() {
		return new ConnectionStateListenerArgs() {
			
			@Override
			public String extraPreState() {
				return BluetoothA2dp.EXTRA_PREVIOUS_STATE;
			}
			
			@Override
			public String extraNewState() {
				return BluetoothA2dp.EXTRA_STATE;
			}
			
			@Override
			public String extraDevice() {
				return BluetoothDevice.EXTRA_DEVICE;
			}
			
			@Override
			public String action() {
				return BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED;
			}
		};
	}
	
}
