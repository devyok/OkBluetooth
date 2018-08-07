package com.devyok.bluetooth.base;

import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.hfp.BluetoothHeadsetProfileService;
import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * @author wei.deng
 */
public abstract class BluetoothProfileServiceTemplate implements BluetoothProfileService{


	protected final String TAG = BluetoothProfileServiceTemplate.class.getSimpleName();
	
	protected int profileType;
	protected BluetoothProfileService decorater = BluetoothProfileService.EMPTY;
	protected BluetoothProfileConnectionStateChangedListener profileConnectionStateListener = BluetoothProfileConnectionStateChangedListener.EMPTY;
	private BluetoothProfileServiceStateListener headsetServiceListener = BluetoothProfileServiceStateListener.EMPTY;
	protected BluetoothProfile realService;
	protected volatile boolean requesting = false;
	
	protected ConnectionStateListenerArgs listenerArgs;
	
	protected ConnectionStateListenerArgs make() {
		return ConnectionStateListenerArgs.EMPTY;
	}
	
	public interface ConnectionStateListenerArgs {
		
		public static ConnectionStateListenerArgs EMPTY = new ConnectionStateListenerArgs() {
			@Override
			public String extraPreState() {
				return BluetoothUtils.EMPTY_STRING;
			}
			
			@Override
			public String extraNewState() {
				return BluetoothUtils.EMPTY_STRING;
			}
			
			@Override
			public String extraDevice() {
				return BluetoothUtils.EMPTY_STRING;
			}
			
			@Override
			public String action() {
				return BluetoothUtils.EMPTY_STRING;
			}
		};
		
		public String action();
		public String extraNewState();
		public String extraPreState();
		public String extraDevice();
	}
	
	public BluetoothProfileServiceTemplate(int profileType){
		this.profileType = profileType;
		listenerArgs = make();
	}
	
	@Override
	public boolean init() throws BluetoothException {
		requestProfileService();
		return true;
	}
	
	@Override
	public boolean destory() {
		if(realService!=null){
			OkBluetooth.closeBluetoothProfile(profileType, realService);
		}
		return true;
	}
	
	public BluetoothProfileServiceTemplate(int profileType,BluetoothProfileService decorater) {
		this(profileType);
		this.decorater = decorater == null ? BluetoothProfileService.EMPTY : decorater;
	}
	
	protected void requestProfileService(){
		
		if(requesting) {
			Log.i(TAG, "[devybt connect] requesting profile("+profileType+") service");
			return;
		}
		
		Log.i(TAG, "[devybt connect] start request profile("+profileType+") service");
		
		requesting = true;
		OkBluetooth.getProfileService(profileType, serviceListener);
	}
	
	ServiceListener serviceListener = new ServiceListener() {
		
		@Override
		public void onServiceDisconnected(int profile) {
			
			String profileString = BluetoothUtils.getProfileString(profile);
			Log.i(TAG, "[devybt connect] onServiceDisconnected profile = " + profileString);
			
			requesting = false;
			realService = null;
			
			onRealServiceDisconnected();
			
		}
		
		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			final String profileString = BluetoothUtils.getProfileString(profile);
			Log.i(TAG, "[devybt connect] onServiceConnected profile = " + profileString);
			requesting = false;
			realService = proxy;
			
			onRealServiceConnected();
			
		}
	};
	
	protected ProfileConnectionStateListenerImpl profileConnectionStateListenerImpl;
	
	private class ProfileConnectionStateListenerImpl extends BroadcastReceiver{
		
		private int profile;
		
		public ProfileConnectionStateListenerImpl(int profile){
			this.profile = profile;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			final String profileString = BluetoothUtils.getProfileString(profile);
			
			int newState = intent.getIntExtra(listenerArgs.extraNewState(), -1);
			int previousState = intent.getIntExtra(listenerArgs.extraPreState(), -1);
			
			Log.i(TAG, "[devybt connect] onServiceConnected profile = " + profileString + " , registerReceiver connection state changed");
			
			Log.i(TAG, "[devybt connect] onServiceConnected profile = " + profileString + " , newState = " + BluetoothUtils.getConnectionStateString(newState) + " , pre state = " + BluetoothUtils.getConnectionStateString(previousState));
			
			BluetoothDevice device = intent.getParcelableExtra(listenerArgs.extraDevice());
			
			BluetoothUtils.dumpBluetoothDevice("BluetoothProfileServiceDecorater", device);
			
			BluetoothProfileConnectionStateChangedListener lis = getListener();
			
			if(lis!=null){
				if(newState == BluetoothHeadset.STATE_CONNECTED) {
					lis.onConnected(profile, newState, previousState, device);
				} else if(newState == BluetoothHeadset.STATE_DISCONNECTED) {
					lis.onDisconnected(profile, newState, previousState, device);
				}
				
			}
			
		}
	}
	
	
	protected void onRealServiceDisconnected() {
		Log.i(TAG, "[devybt connect] onRealServiceDisconnected enter , listener impl = " + profileConnectionStateListenerImpl);
		if(profileConnectionStateListenerImpl!=null){
			OkBluetooth.getContext().unregisterReceiver(profileConnectionStateListenerImpl);
			profileConnectionStateListenerImpl = null;
		}
	}
	
	protected void onRealServiceConnected() {
		
		Log.i(TAG, "[devybt connect] onRealServiceConnected enter");
		if(!TextUtils.isEmpty(listenerArgs.action())) {
			profileConnectionStateListenerImpl = new ProfileConnectionStateListenerImpl(profileType);
			OkBluetooth.getContext().registerReceiver(profileConnectionStateListenerImpl,new IntentFilter(listenerArgs.action()));
		}
		
		if(headsetServiceListener!=null){
			headsetServiceListener.onServiceReady(profileType,this);
		}
		
	}
	
	@Override
	public void registerBluetoothProfileServiceListener(
			BluetoothProfileServiceStateListener lis) {
		// TODO Auto-generated method stub
		headsetServiceListener = (lis == null ? BluetoothProfileServiceStateListener.EMPTY : lis);
	}

	@Override
	public void unregisterBluetoothProfileServiceListener(
			BluetoothProfileServiceStateListener lis) {
		// TODO Auto-generated method stub
		headsetServiceListener = BluetoothProfileServiceStateListener.EMPTY;		
	}
	
	@Override
	public void registerProfileConnectionStateChangedListener(
			BluetoothProfileConnectionStateChangedListener lis) {
		
		if(this.decorater!=null){
			this.decorater.registerProfileConnectionStateChangedListener(lis);
		}
		
		this.profileConnectionStateListener = (lis == null ? BluetoothProfileConnectionStateChangedListener.EMPTY : lis);
	}

	@Override
	public void unregisterProfileConnectionStateChangedListener(
			BluetoothProfileConnectionStateChangedListener lis) {
		if(this.decorater!=null){
			this.decorater.unregisterProfileConnectionStateChangedListener(lis);
		}
		this.profileConnectionStateListener = BluetoothProfileConnectionStateChangedListener.EMPTY;
	}
	
	protected BluetoothProfileConnectionStateChangedListener getListener(){
		return profileConnectionStateListener;
	}

	@Override
	public ProfileConnectionState getConnectionState(final BluetoothDevice device) {
		
		if(realService == null) return ProfileConnectionState.DISCONNECTED;
		
		int state = realService.getConnectionState(device);
		
		return ProfileConnectionState.toState(state);
    }
	
	@Override
	public ProfileConnectionState getCurrentConnectionState() {
		
		List<BluetoothDevice> list = getConnectedBluetoothDeviceList();
		
		if(list.size() > 0){
			return getConnectionState(list.get(0));
		}
		
		return ProfileConnectionState.DISCONNECTED;
	}
	
	@Override
	public List<BluetoothDevice> getConnectedBluetoothDeviceList(){
		
		if(realService == null) return new ArrayList<BluetoothDevice>(0);
		
		return realService.getConnectedDevices();
	}
	
	
	@Override
	public boolean setPriority(final int priority,final BluetoothDevice device) {
		
		if(realService == null) return false;
		
		return setPriority2(priority, device, realService);
	}
	
	
	@Override
	public int getPriority(final BluetoothDevice device) {
		if(realService == null) return BluetoothProfile.PRIORITY_OFF;
		
		return getPriority2(device, realService);
	}
	
	@Override
	public List<BluetoothDevice> getConnectedBluetoothDeviceList(final String deviceName){
		
		if(realService == null) return new ArrayList<BluetoothDevice>(0);
		
		List<BluetoothDevice> bluetoothDevices = getConnectedBluetoothDeviceList();
		
		List<BluetoothDevice> result = new ArrayList<BluetoothDevice>();
		
		for(BluetoothDevice device : bluetoothDevices){
			if(device.getName().equals(deviceName)){
				
				result.add(device);
			}
		}
		
		return result;
	}
	
	@Override
	public boolean connect(final BluetoothDevice device) {
		
		if(realService == null) return false;
		
		return connect2(device, realService);
	}
	
	@Override
	public boolean disconnect(final BluetoothDevice device) {
		
		if(realService == null) return false;
		return disconnect2(device, realService);
	}
	
	
	protected boolean disconnect2(BluetoothDevice device, BluetoothProfile profile) {
		if(this.profileType == BluetoothProfile.A2DP) {
			
			BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) profile;
			return bluetoothA2dp.disconnect(device);
			
		} else if(this.profileType == BluetoothProfile.HEADSET) {
			BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) profile;
			return bluetoothHeadset.disconnect(device);
		}
		
		return false;
	}

	protected boolean connect2(BluetoothDevice device,BluetoothProfile profile){
		if(this.profileType == BluetoothProfile.A2DP) {
			
			BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) profile;
			return bluetoothA2dp.connect(device);
			
		} else if(this.profileType == BluetoothProfile.HEADSET) {
			BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) profile;
			return bluetoothHeadset.connect(device);
		}
		return false;
	}
	
	
	protected boolean setPriority2(int priority, BluetoothDevice device,BluetoothProfile profile){
		if(this.profileType == BluetoothProfile.A2DP) {
			
			BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) profile;
			return bluetoothA2dp.setPriority(device, priority);
			
		} else if(this.profileType == BluetoothProfile.HEADSET) {
			BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) profile;
			return bluetoothHeadset.setPriority(device, priority);
		}
		return false;
	}
	
	
	protected int getPriority2(BluetoothDevice device,BluetoothProfile profile){
		if(this.profileType == BluetoothProfile.A2DP) {
			
			BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) profile;
			return bluetoothA2dp.getPriority(device);
			
		} else if(this.profileType == BluetoothProfile.HEADSET) {
			BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) profile;
			return bluetoothHeadset.getPriority(device);
		}
		return -1;
	}
	
	
}
