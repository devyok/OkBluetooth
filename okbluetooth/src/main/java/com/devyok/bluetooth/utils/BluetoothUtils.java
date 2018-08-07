package com.devyok.bluetooth.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.a2dp.A2dpProfileService;
import com.devyok.bluetooth.base.BluetoothProfileService.ProfileConnectionState;
import com.devyok.bluetooth.base.BluetoothRuntimeException;
import com.devyok.bluetooth.connection.BluetoothConnection.State;
import com.devyok.bluetooth.connection.Connection;
import com.devyok.bluetooth.hfp.HeadsetProfileService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
/**
 * @author deng.wei
 */
public final class BluetoothUtils {

	public static final int UNKNOW = -1000;
	
	public static final String EMPTY_STRING = "";
	
	public static final Runnable EMPTY_TASK = new Runnable() {
		@Override
		public void run() {
		}
	};
	
	public static void ifNullThrowException(Object...objects){
		for(int i = 0;i < objects.length;i++){
			if(objects[i] == null){
				throw new BluetoothRuntimeException("method parametors is null");
			}
		}
	}
	
	public static ThreadFactory createThreadFactory(final String name){
		return new ThreadFactory() {
			int count = 0;
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r,name+"-"+(++count));
			}
		};
	}
	
	public static void close(Closeable closeable){
		if(closeable!=null){
			try {
				closeable.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static State getBluetoothSocketState(BluetoothSocket socket){
		if(socket==null) return State.UNKNOW;
		try {
			java.lang.reflect.Field f = socket.getClass().getDeclaredField("mSocketState");
			
			f.setAccessible(true);
			
			Object obj = f.get(socket);
			
			if(obj!=null){
				return State.valueOf(obj.toString());
			}
			
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return State.UNKNOW;
	}
	
	
	
	public static void dumpBluetoothSystemSwitchStateInfos(String tag,Intent intent){
		if(intent == null || tag == null) return ;
		int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
		int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
		
		String stateStr = BluetoothUtils.getBluetoothSwitchStateString(state);
		String preStateStr = BluetoothUtils.getBluetoothSwitchStateString(previousState);
		
		Log.i(tag, "[devybt connect switch] pre state = " + preStateStr + " , current state = " + stateStr);
		
	}
	
	public static void dumpBluetoothScoStateInfos(String tag,Intent intent){
		if(intent == null || tag == null) return ;
		int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
		int previousState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, -1);
		
		String stateStr = BluetoothUtils.getScoStateStringFromAudioManager(state);
		String preStateStr = BluetoothUtils.getScoStateStringFromAudioManager(previousState);
		
		Log.i(tag, "[devybt sco] pre state = " + preStateStr + " , current state" + stateStr);
		
	}
	
	public static void dumpAudioState(String tag){
		AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
		
		int audioMode = am.getMode();
		boolean isSpeakerphoneOn = am.isSpeakerphoneOn();
		boolean isBluetoothA2dpOn = am.isBluetoothA2dpOn();
		boolean isBluetoothScoOn = am.isBluetoothScoOn();
		
		boolean isBluetoothScoAvailableOffCall = am.isBluetoothScoAvailableOffCall();
		boolean isMicrophoneMute = am.isMicrophoneMute();
		boolean isMusicActive = am.isMusicActive();
		boolean isEnable = OkBluetooth.isBluetoothEnable();
		
		int callVolumn = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
		int callMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		
		int systemVolumn = am.getStreamVolume(AudioManager.STREAM_SYSTEM);
		int systemMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
		
		int ringVolumn = am.getStreamVolume(AudioManager.STREAM_RING);
		int ringMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_RING);
		
		int musicVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		int musicMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		
		int notificationVolumn = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
		int notificationMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
		
		int alarmVolumn = am.getStreamVolume(AudioManager.STREAM_ALARM);
		int alarmMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
		
		int scoBluetoothVolumn = am.getStreamVolume(AudioSystem.STREAM_BLUETOOTH_SCO);
		int scoBluetoothMaxVolumn = am.getStreamMaxVolume(AudioSystem.STREAM_BLUETOOTH_SCO);
		
		
		
		int systemEnf = am.getStreamVolume(AudioSystem.STREAM_SYSTEM_ENFORCED);
		int maxsystemEnf = am.getStreamMaxVolume(AudioSystem.STREAM_SYSTEM_ENFORCED);
		
		int dtmf = am.getStreamVolume(AudioSystem.STREAM_DTMF);
		int maxdtmf = am.getStreamVolume(AudioSystem.STREAM_DTMF);
		
		
		Log.i(tag, "[devybt audio] [ dumpAudioState enter ]");
		
		Log.i(tag, "[devybt audio] audioMode = " + getAudioModeString(audioMode));
		Log.i(tag, "[devybt audio] isSpeakerphoneOn = " + isSpeakerphoneOn);
		Log.i(tag, "[devybt audio] bluetoothIsEnable = "+isEnable);
		Log.i(tag, "[devybt audio] isBluetoothA2dpOn = "+ isBluetoothA2dpOn);
		Log.i(tag, "[devybt audio] isBluetoothScoOn = "+ isBluetoothScoOn);
		Log.i(tag, "[devybt audio] isBluetoothScoAvailableOffCall = "+ isBluetoothScoAvailableOffCall);
		Log.i(tag, "[devybt audio] isMicrophoneMute = "+ isMicrophoneMute);
		Log.i(tag, "[devybt audio] isMusicActive = "+ isMusicActive);
		Log.i(tag, "[devybt audio] voiceCall Volumn = "+ callVolumn + " , MaxVolumn = " + callMaxVolumn);
		Log.i(tag, "[devybt audio] music Volumn = "+ musicVolumn + " , MaxVolumn = " + musicMaxVolumn);
		Log.i(tag, "[devybt audio] system Volumn = "+ systemVolumn + " , MaxVolumn = " + systemMaxVolumn);
		Log.i(tag, "[devybt audio] ring Volumn = "+ ringVolumn + " , MaxVolumn = " + ringMaxVolumn);
		Log.i(tag, "[devybt audio] notification Volumn = "+ notificationVolumn + " , MaxVolumn = " + notificationMaxVolumn);
		Log.i(tag, "[devybt audio] alarm Volumn = "+ alarmVolumn + " , MaxVolumn = " + alarmMaxVolumn);
		Log.i(tag, "[devybt audio] sco Volumn = "+ scoBluetoothVolumn + " , MaxVolumn = " + scoBluetoothMaxVolumn);
		Log.i(tag, "[devybt audio] dtmf Volumn = "+ dtmf + " , MaxVolumn = " + maxdtmf);
		Log.i(tag, "[devybt audio] systemEnf Volumn = "+ systemEnf + " , MaxVolumn = " + maxsystemEnf);
		
		Log.i(tag, "[devybt audio] [ dumpAudioState exit ]");
		
	}
	
	
	public static void dumpBluetoothConnectionInfos(String tag,Intent intent){
		if(intent == null) return ;
		int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
		int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE, -1);
		BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		String stateStr = BluetoothUtils.getConnectionStateString(state);
		String preStateStr = BluetoothUtils.getConnectionStateString(previousState);
		
		String deviceName = "unkown device";
		if (device != null) {
			deviceName = device.getName();
		}
		Log.i(tag, "[devybt connect] pre state = " + preStateStr + " , current state = " + stateStr + " , device = " + deviceName);
	}
	
	@Deprecated
	public static void dumpBluetoothStates(String tag){
		AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
		
		boolean isEnable = OkBluetooth.isBluetoothEnable();
		boolean isBluetoothScoOn = am.isBluetoothScoOn();
		
		Log.i(tag, "[devybt device] dumpBluetoothStates enter --");
		Log.i(tag, "[devybt device] isEnable = "+isEnable);
		Log.i(tag, "[devybt device] isBluetoothScoOn = " + isBluetoothScoOn);
		Log.i(tag, "[devybt device] dumpBluetoothStates enter --");
	}
	
	public static void dumpBluetoothDevices(String tag, List<BluetoothDevice> connectedDevices) {
		if(connectedDevices!=null && connectedDevices.size() > 0){
			
			Log.i(tag, "[devybt device] [ dumpBluetoothDeviceList enter ]");
			int size = connectedDevices.size();
			boolean br = size > 1;
			for (int i = 0;i < size;i++) {
				
				BluetoothDevice bluetoothDevice = connectedDevices.get(i);
				dumpBluetoothDevice(tag,bluetoothDevice);
				if(br && (i > size - 1)) Log.i(tag, "\n");
			}
			
			Log.i(tag, "[devybt device] [ dumpBluetoothDeviceList exit ");
			
		}else {
			Log.i(tag, "[devybt connect] dumpBluetoothDeviceList enter , but connected devices is empty");
		}
	}
	
	public static void dumpBluetoothConnection(String tag,Connection connection) {
		
		if(connection!=null){
			
			Log.i(tag, "[devybt connection] dumpBluetoothConnection enter --------------");
			
			Log.i(tag, "[devybt connection] isConnected = " + connection.isConnected());
			Log.i(tag, "[devybt connection] getTimeout = " + connection.getTimeout());
			Log.i(tag, "[devybt connection] getState = " + connection.getState());
			
			dumpBluetoothDevice(tag, connection.getBluetoothDevice(), false);
			
			Log.i(tag, "[devybt connection] dumpBluetoothConnection exit --------------");
		}
		
	}
	
	public static void dumpBluetoothDevice(String tag, BluetoothDevice bluetoothDevice) {
		dumpBluetoothDevice(tag,bluetoothDevice,false);
	}
	
	public static void dumpBluetoothDevice(String tag, BluetoothDevice bluetoothDevice,boolean isDumpUUIDS) {
		if(bluetoothDevice!=null){
			
			Log.i(tag, "[devybt device] [ dumpBluetoothDevice enter ]");
			
			Log.i(tag, "[devybt device] =============================getName = " + bluetoothDevice.getName());
			Log.i(tag, "[devybt device] getDeviceBondState = " + getDeviceBondState(bluetoothDevice.getBondState()));
			Log.i(tag, "[devybt device] getAddress = " + bluetoothDevice.getAddress());
			Log.i(tag, "[devybt device] getBluetoothClass = " + getBluetoothClassString(bluetoothDevice.getBluetoothClass()));
			
			boolean isSupportA2DP = OkBluetooth.isSupportA2DP(bluetoothDevice);
			boolean isSuuportHFP = OkBluetooth.isSupportHFP(bluetoothDevice);
			
			Log.i(tag, "[devybt device] isSupportA2DP = " + isSupportA2DP);
			Log.i(tag, "[devybt device] isSuuportHFP = " + isSuuportHFP);
			
			
			if(isSuuportHFP){
				ProfileConnectionState connectionState = OkBluetooth.HFP.getConnectionState(bluetoothDevice);
				boolean isAudioConnected = OkBluetooth.HFP.isAudioConnected(bluetoothDevice);
				int priority = OkBluetooth.getPriority(HeadsetProfileService.PROFILE,bluetoothDevice);
				
				Log.i(tag, "[devybt device] HFP connectionState = " + connectionState);
				Log.i(tag, "[devybt device] HFP isAudioConnected = " + isAudioConnected);
				Log.i(tag, "[devybt device] HFP priority = " + BluetoothUtils.getDevicePriority(priority));
				
			} 
			
			if(isSupportA2DP){
				ProfileConnectionState connectionState = OkBluetooth.getConnectionState(A2dpProfileService.PROFILE, bluetoothDevice);
				boolean isA2dpPlaying = OkBluetooth.A2DP.isA2dpPlaying(bluetoothDevice);
				int priority = OkBluetooth.getPriority(A2dpProfileService.PROFILE,bluetoothDevice);
				
				Log.i(tag, "[devybt device] A2DP connectionState = " + connectionState);
				Log.i(tag, "[devybt device] A2DP isA2dpPlaying = " + isA2dpPlaying);
				Log.i(tag, "[devybt device] A2DP priority = " + BluetoothUtils.getDevicePriority(priority));
				
			} 
			
			
			if(isDumpUUIDS){
				ParcelUuid[] parcelUuidArray = bluetoothDevice.getUuids();
				
				if(parcelUuidArray!=null){
					for(int i = 0;i < parcelUuidArray.length;i++) {
						Log.i(tag, "[devybt device] getUuids index("+i+") = " + parcelUuidArray[i].toString());
					}
				}
			}
			
			Log.i(tag, "[devybt device] [ dumpBluetoothDevice exit ]");
		} else {
			Log.i(tag, "[devybt device] [ dumpBluetoothDevice enter ,  bluetoothDevice null]");
		}
	}
	
	public static void dumpBluetoothSocket(String tag, BluetoothSocket socket){
		Log.i(tag, "[devybt connection] dumpBluetoothSocket enter --");
		
		Log.i(tag, "[devybt connection] isConnected = " + socket.isConnected());
		Log.i(tag, "[devybt connection] state = " + getBluetoothSocketState(socket));
//		Log.i(tag, "[devybt connection] connectionType = " + socket.getConnectionType());
//		Log.i(tag, "[devybt connection] maxReceivePacketSize = " + socket.getMaxReceivePacketSize());
//		Log.i(tag, "[devybt connection] maxTransmitPacketSize = " + socket.getMaxTransmitPacketSize());
		
		Log.i(tag, "[devybt connection] dumpBluetoothSocket exit --");
	}
	
	public static String getBluetoothClassString(BluetoothClass btClass){
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case BluetoothClass.Device.Major.COMPUTER:
                    return "COMPUTER";

                case BluetoothClass.Device.Major.PHONE:
                	return "PHONE";

                case BluetoothClass.Device.Major.PERIPHERAL:
                	return "PERIPHERAL";

                case BluetoothClass.Device.Major.IMAGING:
                	return "IMAGING";

                default:
                	return "UNKNOW";
            }
        } else {
        	return "UNKNOW2";
        }
	}
	
	public static String getProfileString(int profile){
		switch (profile) {
		case BluetoothProfile.A2DP:
			return "A2DP";
		case BluetoothProfile.HEADSET:
			return "HEADSET";
		case BluetoothProfile.HEALTH:
			return "HEALTH";
		default:
			return "UNKNOW";
		}
	}
	
	public static String getConnectionStateString(int state) {
		String stateStr = "UNKNOW";
		switch (state) {
		case BluetoothAdapter.STATE_CONNECTING:
			stateStr = "STATE_CONNECTING";
			break;
		case BluetoothAdapter.STATE_CONNECTED:
			stateStr = "STATE_CONNECTED";
			break;
		case BluetoothAdapter.STATE_DISCONNECTING:
			stateStr = "STATE_DISCONNECTING";
			break;
		case BluetoothAdapter.STATE_DISCONNECTED:
			stateStr = "STATE_DISCONNECTED";
			break;
		}
		return stateStr;
	} 
	
	public static String getScoStateStringFromAudioManager(int state) {
		String stateStr = "UNKNOW";
		switch (state) {
		case AudioManager.SCO_AUDIO_STATE_CONNECTING:
			stateStr = "SCO_AUDIO_STATE_CONNECTING";
			break;
		case AudioManager.SCO_AUDIO_STATE_CONNECTED:
			stateStr = "SCO_AUDIO_STATE_CONNECTED";
			break;
		case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
			stateStr = "SCO_AUDIO_STATE_DISCONNECTED";
			break;
		case AudioManager.SCO_AUDIO_STATE_ERROR:
			stateStr = "SCO_AUDIO_STATE_ERROR";
			break;
		}
		return stateStr;
	} 
	
	public static String getScoStateStringFromHeadsetProfile(int state) {
		String stateStr = "";
		switch (state) {
		case BluetoothHeadset.STATE_AUDIO_CONNECTED:
			stateStr = "CONNECTED";
			break;
		case BluetoothHeadset.STATE_AUDIO_CONNECTING:
			stateStr = "CONNECTING";
			break;
		case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
			stateStr = "DISCONNECTED";
			break;
		default:
			stateStr = "UNKNOW";
			break;
		}
		return stateStr;
	} 
	
	public static String getBluetoothSwitchStateString(int state){
		String stateStr = "";
		switch (state) {
		case BluetoothAdapter.STATE_TURNING_ON:
			stateStr = "STATE_TURNING_ON";
			break;
		case BluetoothAdapter.STATE_ON:
			stateStr = "STATE_ON";
			break;
		case BluetoothAdapter.STATE_TURNING_OFF:
			stateStr = "STATE_TURNING_OFF";
		case BluetoothAdapter.STATE_OFF:
			stateStr = "STATE_OFF";
		default:
			break;
		}
		return stateStr;
	}

	public static String getAudioModeString(int mode) {
		
		String modeStr = null;
		switch (mode) {
		case AudioManager.MODE_CURRENT:
			modeStr = "MODE_CURRENT";
			break;
		case AudioManager.MODE_IN_CALL:
			modeStr = "MODE_IN_CALL";
			break;
		case AudioManager.MODE_IN_COMMUNICATION:
			modeStr = "MODE_IN_COMMUNICATION";
			break;
		case AudioManager.MODE_INVALID:
			modeStr = "MODE_INVALID";
			break;
		case AudioManager.MODE_NORMAL:
			modeStr = "MODE_NORMAL";
			break;
		case AudioManager.MODE_RINGTONE:
			modeStr = "MODE_RINGTONE";
			break;
		default:
			modeStr = "mode("+mode+") MODE_???????";
			break;
		}
		return modeStr;
	}
	

	public static String getDevicePriority(int priority) {
		
		String result = null;
		switch (priority) {
		case BluetoothProfile.PRIORITY_AUTO_CONNECT:
			result = "PRIORITY_AUTO_CONNECT";
			break;
		case BluetoothProfile.PRIORITY_ON:
			result = "PRIORITY_ON";
			break;
		case BluetoothProfile.PRIORITY_OFF:
			result = "PRIORITY_OFF";
			break;
		case BluetoothProfile.PRIORITY_UNDEFINED:
			result = "PRIORITY_UNDEFINED";
			break;
		default:
			result = "UNKNOW";
			break;
		}
		return result;
	}
	
	public static String getDeviceBondState(int mode) {
		
		String modeStr = null;
		switch (mode) {
		case BluetoothDevice.BOND_NONE:
			modeStr = "BOND_NONE";
			break;
		case BluetoothDevice.BOND_BONDING:
			modeStr = "BOND_BONDING";
			break;
		case BluetoothDevice.BOND_BONDED:
			modeStr = "BOND_BONDED";
			break;
		default:
			modeStr = "mode("+mode+") MODE_???????";
			break;
		}
		return modeStr;
	}

	public static void dumpBluetoothAllSystemInfos(final String TAG) {
		
		Log.i(TAG, "\n");
		Log.i(TAG, "\n");
		Log.i(TAG, "****************************************************");
		Log.i(TAG, "## audioStates --");
		BluetoothUtils.dumpAudioState(TAG);
		
		Log.i(TAG, "\n");
		Log.i(TAG, "## bondedDevices --");
		Set<BluetoothDevice> set = OkBluetooth.getBondedDevices();
		BluetoothUtils.dumpBluetoothDevices(TAG, new ArrayList<>(set));
		
		Log.i(TAG, "\n");
		Log.i(TAG, "## systemProperties --");
		Log.i(TAG, "[devybt property] max hfpclient connection count = " + BluetoothUtils.getMaxHfpConnectionCount());

		Log.i(TAG, "\n");
		Log.i(TAG, "## deviceProfileConnectionStates HFP or A2DP --------");
		LinkedHashMap<BluetoothDevice,ProfileConnectionState> map = OkBluetooth.getAllProfileConnectionState();
		for(Iterator<Map.Entry<BluetoothDevice, ProfileConnectionState>> iter = map.entrySet().iterator();iter.hasNext();){
			
			Map.Entry<BluetoothDevice, ProfileConnectionState> item = iter.next();
			
			BluetoothDevice bluetoothDevice = item.getKey();
			ProfileConnectionState state = item.getValue();
			
			boolean isSupportHFP = OkBluetooth.isSupportHFP(bluetoothDevice);
			boolean isSupportA2dp = OkBluetooth.isSupportA2DP(bluetoothDevice);
			
			boolean isSupportSPP = OkBluetooth.isSupportSPP(bluetoothDevice);
			
			Log.i(TAG, "[devybt connection] device name = " + bluetoothDevice.getName() + " , state = " + state + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp + " ,isSupportSPP = " + isSupportSPP);
			
		}

	}

	public static String toString(Object[] array) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("[");
		for (int i = 0; i < array.length; i++) {
			stringBuffer.append(array[i]).append(",");
		}
		
		if(stringBuffer.length() > 1){
			stringBuffer = stringBuffer.deleteCharAt(stringBuffer.length() - 1);
		}
		
		stringBuffer.append("]");
		
		return stringBuffer.toString();
	}
	
	public static String getAudioStreamTypeString(int streamType) {
		
		String cmdTypeString = "UNKNOW("+streamType+")";
		switch (streamType) {
		case AudioManager.STREAM_MUSIC:
			cmdTypeString = "STREAM_MUSIC";
			break;
		case AudioManager.STREAM_VOICE_CALL:
			cmdTypeString = "STREAM_VOICE_CALL";
			break;
		case AudioManager.STREAM_RING:
			cmdTypeString = "STREAM_RING";
			break;
		case AudioManager.STREAM_NOTIFICATION:
			cmdTypeString = "STREAM_NOTIFICATION";
			break;
		case AudioManager.STREAM_ALARM:
			cmdTypeString = "STREAM_ALARM";
			break;
		}
		
		return cmdTypeString;
	}

	public static String getHeadsetEventTypeString(int cmdType) {
		
		String cmdTypeString = "UNKNOW";
		switch (cmdType) {
		case BluetoothHeadset.AT_CMD_TYPE_ACTION:
			cmdTypeString = "AT_CMD_TYPE_ACTION";
			break;
		case BluetoothHeadset.AT_CMD_TYPE_BASIC:
			cmdTypeString = "AT_CMD_TYPE_BASIC";
			break;
		case BluetoothHeadset.AT_CMD_TYPE_READ:
			cmdTypeString = "AT_CMD_TYPE_READ";
			break;
		case BluetoothHeadset.AT_CMD_TYPE_SET:
			cmdTypeString = "AT_CMD_TYPE_SET";
			break;
		case BluetoothHeadset.AT_CMD_TYPE_TEST:
			cmdTypeString = "AT_CMD_TYPE_TEST";
			break;
		}
		
		return cmdTypeString;
	}

	public static void setAudioMode(int mode) {
		AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
		am.setMode(mode);
	}
	
	public static String getCurrentAudioModeString(){
		AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
		
		return getAudioModeString(am.getMode());
	}

	public static void openSpeaker() {
		setSpeakerphoneOn(true);
	}

	public static void closeSpeaker() {
		setSpeakerphoneOn(false);
	}

	public static void setSpeakerphoneOn(boolean on){
		AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
		am.setSpeakerphoneOn(on);
	}
	
	public static void setBluetoothA2dpOn(boolean on){
		AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
		am.setBluetoothA2dpOn(on);
	}

	public static void dumpProfileConnectionMap(String TAG,LinkedHashMap<BluetoothDevice, ProfileConnectionState> map){
		for(Iterator<Map.Entry<BluetoothDevice, ProfileConnectionState>> iter = map.entrySet().iterator();iter.hasNext();){
			
			Map.Entry<BluetoothDevice, ProfileConnectionState> item = iter.next();
			
			final BluetoothDevice bluetoothDevice = item.getKey();
			ProfileConnectionState state = item.getValue();
			
			boolean isSupportHFP = OkBluetooth.isSupportHFP(bluetoothDevice);
			boolean isSupportA2dp = OkBluetooth.isSupportA2DP(bluetoothDevice);
			
			Log.i(TAG, "allProfileConnectionState device name = " + bluetoothDevice.getName() + " , state = " + state + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);
			
		}
	}
	
	public static int getMaxHfpConnectionCount(){
		String maxHfpclientConnectionCount = SystemProperties.get("bt.max.hfpclient.connections");
		int maxCount = 1;
		if(!TextUtils.isEmpty(maxHfpclientConnectionCount)) {
			maxCount = Integer.parseInt(maxHfpclientConnectionCount);
		}
		return maxCount;
	}

	public static int search(String[] array,String item){
		for (int i = 0; i < array.length; i++) {
			if(item.equals(array[i])) {
				return i;
			}
		}
		
		return -1;
	}
	
}
