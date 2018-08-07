package com.devyok.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.base.BluetoothProfileService;
import com.devyok.bluetooth.base.BluetoothProfileServiceTemplate;
import com.devyok.bluetooth.base.BluetoothRuntimeException;
import com.devyok.bluetooth.utils.BluetoothUtils;

/**
 * @author wei.deng
 */
public class HeadsetProfileService extends BluetoothProfileServiceTemplate implements BluetoothHeadsetProfileService{

	public static final ParcelUuid[] UUIDS = {
			BluetoothUuid.HSP,
			BluetoothUuid.Handsfree,
	};

	public static final int PROFILE = BluetoothProfile.HEADSET;

	private BluetoothHeadsetProfileService.BluetoothHeadsetAudioStateListener audioStateListener;
	private BroadcastReceiver audioStateReceiverImpl;

	public HeadsetProfileService() {
		super(PROFILE);
	}

	public HeadsetProfileService(BluetoothProfileService decorater) {
		super(PROFILE,decorater);
	}

	public HeadsetProfileService(int profileType) {
		super(profileType);
		throw new BluetoothRuntimeException("not support");
	}

	/**
	 * 如果sco已经连接上，则callback返回true
	 * @param device
	 */
	@Override
	public boolean isAudioConnected(final BluetoothDevice device) {

		if(realService == null) return false;

		return ((BluetoothHeadset) realService).isAudioConnected(device);

	}

	@Override
	public boolean disconnectAudio() {
		if(realService == null) return false;

		return ((BluetoothHeadset) realService).disconnectAudio();
	}

	@Override
	public boolean connectAudio() {

		if(realService == null) return false;

		return ((BluetoothHeadset) realService).connectAudio();

	}

	@Override
	public boolean isAudioOn(){
		if(realService == null) return false;
		return ((BluetoothHeadset) realService).isAudioOn();
	}

	@Override
	public int getAudioState(final BluetoothDevice device){
		if(realService == null) return BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
		return ((BluetoothHeadset) realService).getAudioState(device);
	}

	@Override
	protected void onRealServiceConnected() {
		super.onRealServiceConnected();

		if(audioStateReceiverImpl == null){
			OkBluetooth.getContext().registerReceiver(audioStateReceiverImpl = new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {

					int newState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
					int previousState = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);

					Log.i(TAG, "[devybt connect] hfp audio new state = " + BluetoothUtils.getScoStateStringFromHeadsetProfile(newState) + " , pre state = " + BluetoothUtils.getScoStateStringFromHeadsetProfile(previousState));

					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

					BluetoothUtils.dumpBluetoothDevice(TAG, device);

					if(audioStateListener!=null){

						if(newState == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
							audioStateListener.onAudioConnected(device,HeadsetProfileService.this);
						} else if(newState == BluetoothHeadset.STATE_AUDIO_DISCONNECTED){
							audioStateListener.onAudioDisconnected(device, HeadsetProfileService.this);
						} else if(newState == BluetoothHeadset.STATE_AUDIO_CONNECTING) {
							audioStateListener.onAudioConnecting(device, HeadsetProfileService.this);
						}

					}

				}
			}, new IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED));
		}
	}

	@Override
	protected void onRealServiceDisconnected() {
		super.onRealServiceDisconnected();
		if(audioStateReceiverImpl!=null){
			OkBluetooth.getContext().unregisterReceiver(audioStateReceiverImpl);
			audioStateReceiverImpl = null;
		}
	}

	@Override
	protected ConnectionStateListenerArgs make() {
		return new ConnectionStateListenerArgs() {

			@Override
			public String extraPreState() {
				return BluetoothHeadset.EXTRA_PREVIOUS_STATE;
			}

			@Override
			public String extraNewState() {
				return BluetoothHeadset.EXTRA_STATE;
			}

			@Override
			public String extraDevice() {
				return BluetoothDevice.EXTRA_DEVICE;
			}

			@Override
			public String action() {
				return BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED;
			}
		};
	}

	@Override
	public void registerAudioStateChangedListener(
			BluetoothHeadsetAudioStateListener lis) {
		audioStateListener = lis;
	}

	@Override
	public void unregisterAudioStateChangedListener(
			BluetoothHeadsetAudioStateListener lis) {
		audioStateListener = null;
	}

}
