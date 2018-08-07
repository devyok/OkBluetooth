package com.devyok.bluetooth.demo;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.devyok.bluetooth.AudioDevice;
import com.devyok.bluetooth.Configuration;
import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.OkBluetooth.BluetoothProtocolConnectionStateListener;
import com.devyok.bluetooth.OkBluetooth.ConnectionMode;
import com.devyok.bluetooth.OkBluetooth.Interceptor;
import com.devyok.bluetooth.connection.BluetoothConnection.Protocol;
import com.devyok.bluetooth.message.BluetoothMessage;
import com.devyok.bluetooth.message.BluetoothMessageReceiver;
import com.devyok.bluetooth.spp.SPPBluetoothMessageParser;
import com.devyok.bluetooth.utils.BluetoothUtils;

public final class OkBluetoothAdapter {

	private static final String TAG = OkBluetoothAdapter.class.getSimpleName();
	
	private static final ConcurrentHashMap<String, Interceptor> gInterceptors = new ConcurrentHashMap<>();
	
	static {
		gInterceptors.put("phoneModel", new DemoInterceptor());
	}
	
	public static void onAppReady(Context context){
		
		Log.i(TAG,"device mode = " + Build.MODEL);
		
		Configuration configuration = new Configuration.Builder()
														.setDebug(true)
														.setSupport(true)
														.setCompanyId(85)
														.setConnectionMode(ConnectionMode.BLUETOOTH_WIREDHEADSET_SPEAKER)
														.setForceTypes(OkBluetooth.FORCE_TYPE_PHONE_INCALL | 
																	   OkBluetooth.FORCE_TYPE_PHONE_RING   | 
																	   OkBluetooth.FORCE_TYPE_PHONE_INCALL_TO_IDLE)
														.build();
		

		OkBluetooth.init(context.getApplicationContext(), configuration);
		
		Interceptor interceptor = gInterceptors.get(Build.MODEL);
		
		OkBluetooth.setInterceptor(interceptor);
		OkBluetooth.registerBluetoothProtocolConnectionStateListener(new BluetoothProtocolConnectionStateListener() {

			@Override
			public void onDisconnected(Protocol protocol, BluetoothDevice device) {
				super.onDisconnected(protocol, device);
				Log.i("btMessage", "onDisconnected protocol = " + protocol.getName() + " , device = " + device.getName());
			}

			@Override
			public void onConnected(Protocol protocol, BluetoothDevice device) {
				super.onConnected(protocol, device);
				Log.i("btMessage", "onConnected protocol = " + protocol.getName() + " , device = " + device.getName());
			}
			
		});
		
		OkBluetooth.registerBluetoothMessageParser(new SavoxSppMessageParser());
		
		OkBluetooth.registerBluetoothMessageReceiver(new BluetoothMessageReceiver<String>() {

			@Override
			public boolean onReceive(BluetoothMessage<String> message) {
				String bodyData = message.getBodyData();
				
				Log.i("btMessage", "body data = " + bodyData);
				
				return false;
			}
			
		},new Protocol[]{Protocol.SPP,Protocol.HFP});
		
		context.registerReceiver(new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				
				int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
				
				Log.i("volumnTrace", "volumn changed stream type = " + BluetoothUtils.getAudioStreamTypeString(streamType));
				
			}
		}, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
		
	}
	
	static class SavoxSppMessageParser implements SPPBluetoothMessageParser<String> {

		final Pattern pattern = Pattern.compile("\\W?\\w+=\\w{1}");
		
		@Override
		public BluetoothMessage<String>[] parse(byte[] buffer,int readCount,Protocol protocol,BluetoothDevice device) {
			String data = new String(buffer, 0, readCount);
			Log.i("btMessage", "receiver data = " + data);
			Matcher matcher = pattern.matcher(data);
			ArrayList<BluetoothMessage<String>> result = new ArrayList<>();
			while(matcher.find()){
				result.add(BluetoothMessage.obtain(matcher.group(), protocol));
			}
			
			return result.toArray(new BluetoothMessage[]{});
		}
		
	}
	
	static class DemoInterceptor extends Interceptor {
		
		@Override
		public boolean beforeConnect(AudioDevice audioDevice) {
			
			if(AudioDevice.SCO == audioDevice) {
//				handleConnectSco();
//				return true;
			}
			
			return super.beforeConnect(audioDevice);
		}
		
		private static void handleConnectSco(){
			boolean isAudioConnected = OkBluetooth.HFP.isAudioConnected();
			boolean isBluetoothScoOn = OkBluetooth.isBluetoothScoOn();
			boolean isBluetoothA2dpOn = OkBluetooth.isBluetoothA2dpOn();
			boolean isSpeakerphoneOn = OkBluetooth.isSpeakerphoneOn();
			
			Log.i(TAG, "intercept tryBuildAudioConnection(SCO) isAudioConnected = " + isAudioConnected + " , isBluetoothScoOn = " + isBluetoothScoOn + " , isBluetoothA2dpOn = " + isBluetoothA2dpOn + " , isSpeakerphoneOn = " + isSpeakerphoneOn);
			if(!isAudioConnected) {
				syncSetMode(AudioManager.MODE_IN_CALL);
				
				OkBluetooth.setSpeakerphoneOn(false);
				OkBluetooth.setBluetoothA2dpOn(false);
				OkBluetooth.setBluetoothScoOn(false);
				syncConnectAudio();
				OkBluetooth.setScoStreamVolumn(OkBluetooth.getScoMaxStreamVolumn(), AudioManager.FLAG_PLAY_SOUND);
			} else {
				
				if(isBluetoothA2dpOn){
					OkBluetooth.setBluetoothA2dpOn(false);
				}
				if(isSpeakerphoneOn){
					OkBluetooth.setSpeakerphoneOn(false);
				}
				if(!isBluetoothScoOn) {
					OkBluetooth.setBluetoothScoOn(true);
				}
				
			}
		}
		
		private static void syncSetMode(int mode){
			Log.i(TAG, "syncSetMode("+BluetoothUtils.getAudioModeString(mode)+")");
			Intent intent = new Intent("com.zed3.action.SET_SYSTEM_AUDIO_MODE");
			intent.putExtra("com.zed3.extra.AUDIO_MODE", mode);
			OkBluetooth.getContext().sendBroadcast(intent);
			
			int newmode = OkBluetooth.getAudioMode();
			int max = 5;
			int cur = 0;
			while(newmode != mode) {
				if(cur >= max){
					break;
				}
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				newmode = OkBluetooth.getAudioMode();
				
				Log.i(TAG, "loop item wait new mode("+BluetoothUtils.getAudioModeString(newmode)+")");
				++cur;
			}
			
		}
		
		private static void syncConnectAudio(){
			OkBluetooth.HFP.connectAudio();
			
			int max = 5;
			int cur = 0;
			
			boolean isAudioConnected = OkBluetooth.HFP.isAudioConnected();
			
			Log.i(TAG, "syncConnectAudio("+isAudioConnected+")");
			
			while(!isAudioConnected) {
				if(cur >= max){
					break;
				}
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				isAudioConnected = OkBluetooth.HFP.isAudioConnected();
				
				Log.i(TAG, "loop item wait new audio state("+isAudioConnected+")");
				++cur;
			}
			
		}
		
	}
	
	
	
	
}
