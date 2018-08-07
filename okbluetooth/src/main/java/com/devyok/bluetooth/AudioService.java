package com.devyok.bluetooth;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.devyok.bluetooth.OkBluetooth.Interceptor;
import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.base.BluetoothRuntimeException;
import com.devyok.bluetooth.base.BluetoothService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
/**
 * @author wei.deng
 */
final class AudioService extends BluetoothService{

	private static final String TAG = OkBluetooth.TAG;

	private AudioManager mAudioManager;
	private IAudioService mRealAudioService;
	private AudioModeModifier mAudioModeModifier = AudioModeModifier.DEFAULT;

	/**
	 * 实现音频模式的修改
	 */
	public interface AudioModeModifier {
		public static final AudioModeModifier DEFAULT = new AudioModeModifier() {
			@Override
			public void modify(int audioMode) {

				int current = OkBluetooth.getAudioMode();

				if(current != audioMode){
					OkBluetooth.setAudioMode(audioMode);
				}

			}
		};
		public void modify(int audioMode);
	}

	@Override
	public boolean init() throws BluetoothException {

		super.init();

		mAudioManager = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
		mRealAudioService = IAudioService.Stub.asInterface(ServiceManager.getService(Context.AUDIO_SERVICE));

		return true;
	}

	@Override
	public boolean destory() {
		super.destory();

		mAudioManager = null;
		mRealAudioService = null;

		return true;
	}

	public boolean connectAudio(AudioDevice audioDevice){

		if(audioDevice == null) throw new BluetoothRuntimeException("audioDevice null");

		Interceptor interceptor = OkBluetooth.getInterceptor();

		if(interceptor.beforeConnect(audioDevice)) {
			Log.i(TAG, "[AudioService] connectAudio "+audioDevice + " intercepted");
			return false;
		}

		if(AudioDevice.SBP == audioDevice){

		} else if(AudioDevice.SPEAKER == audioDevice){

			tryConnectSpeaker();

		} else if(AudioDevice.SCO == audioDevice){

			tryConnectSco();

		} else if(AudioDevice.WIREDHEADSET == audioDevice){

			tryConnectWiredHeadset();

		} else if(AudioDevice.A2DP == audioDevice){

			tryConnectSpeaker();

		}

		return true;
	}

	public boolean isBluetoothA2dpOn(){
		try {
			IAudioService audioService = mRealAudioService;
			boolean result = audioService.isBluetoothA2dpOn();
			return result;
		} catch (RemoteException e) {
			e.printStackTrace();
			Log.i(TAG, "[AudioService] isBluetoothA2dpOn on a2dp exception = " + e.getMessage());
		}

		return false;
	}

	public boolean isWiredHeadsetOn(){
		return mAudioManager.isWiredHeadsetOn();
	}

	public void setBluetoothA2dpOn(boolean on){
		try {
			IAudioService audioService = mRealAudioService;
			boolean result = audioService.isBluetoothA2dpOn();
			Log.i(TAG, "[AudioService] setBluetoothA2dpOn pre state = "+ result);
			audioService.setBluetoothA2dpOn(on);
		} catch (RemoteException e) {
			e.printStackTrace();
			Log.i(TAG, "[AudioService] setBluetoothA2dpOn on a2dp exception = " + e.getMessage());
		}
	}

	/**
	 * 尝试连接SCO
	 */
	void tryConnectSco() {
		boolean isPhoneCalling = OkBluetooth.isPhoneCalling();
		boolean isAudioConnected = OkBluetooth.HFP.isAudioConnected();
		boolean isBluetoothScoOn = isBluetoothScoOn();
		boolean isBluetoothA2dpOn = isBluetoothA2dpOn();
		boolean isSpeakerphoneOn = isSpeakerphoneOn();

		Log.i(TAG, "[AudioService] tryConnectSco isAudioConnected = " + isAudioConnected + " , isBluetoothScoOn = " + isBluetoothScoOn + " , isBluetoothA2dpOn = " + isBluetoothA2dpOn + " , isSpeakerphoneOn = " + isSpeakerphoneOn);
		if(!isAudioConnected) {
			if(!isPhoneCalling) {
				mAudioModeModifier.modify(AudioManager.MODE_IN_COMMUNICATION);
			}
			setSpeakerphoneOn(false);
			setBluetoothA2dpOn(false);
			setBluetoothScoOn(false);
			if(isPhoneCalling) {
				OkBluetooth.HFP.connectAudio();
			} else {
				OkBluetooth.startSco();
			}
		} else {

			if(isBluetoothA2dpOn){
				setBluetoothA2dpOn(false);
			}
			if(isSpeakerphoneOn){
				setSpeakerphoneOn(false);
			}
			if(!isBluetoothScoOn) {
				setBluetoothScoOn(true);
			}

		}

	}

	/**
	 * 尝试连接有线耳机
	 */
	void tryConnectWiredHeadset() {

		boolean isPhoneCalling = OkBluetooth.isPhoneCalling();

		boolean isAudioConnected = OkBluetooth.HFP.isAudioConnected();

		boolean isBluetoothScoOn = isBluetoothScoOn();
		boolean isBluetoothA2dpOn = isBluetoothA2dpOn();
		boolean isSpeakerphoneOn = isSpeakerphoneOn();
		if(!isPhoneCalling){
			mAudioModeModifier.modify(AudioManager.MODE_IN_COMMUNICATION);
		}
		Log.i(TAG, "[AudioService] tryConnectWiredHeadset isAudioConnected = " + isAudioConnected + " , isBluetoothScoOn = " + isBluetoothScoOn + " , isBluetoothA2dpOn = " + isBluetoothA2dpOn + " , isSpeakerphoneOn = " + isSpeakerphoneOn);

		if(isAudioConnected){
			outofBluetoothScoEnv();
			setSpeakerphoneOn(false);
		} else {
			if(OkBluetooth.isBluetoothA2dpOn()){
				setBluetoothA2dpOn(false);
			}
			if(OkBluetooth.isBluetoothScoOn()){
				setBluetoothScoOn(false);
			}
			if(OkBluetooth.isSpeakerphoneOn()){
				setSpeakerphoneOn(false);
			}
		}
	}

	void tryConnectSpeaker() {
		boolean isPhoneCalling = OkBluetooth.isPhoneCalling();
		boolean isAudioConnected = OkBluetooth.HFP.isAudioConnected();
		boolean isBluetoothScoOn = isBluetoothScoOn();
		boolean isBluetoothA2dpOn = isBluetoothA2dpOn();
		boolean isSpeakerphoneOn = isSpeakerphoneOn();
		if(!isPhoneCalling){
			mAudioModeModifier.modify(AudioManager.MODE_NORMAL);
		}
		Log.i(TAG, "[AudioService] tryConnectSpeaker isAudioConnected = " + isAudioConnected + " , isBluetoothScoOn = " + isBluetoothScoOn + " , isBluetoothA2dpOn = " + isBluetoothA2dpOn + " , isSpeakerphoneOn = " + isSpeakerphoneOn);
		if(isAudioConnected) {
			outofBluetoothScoEnv();
		}
		if(OkBluetooth.isBluetoothScoOn()){
			setBluetoothScoOn(false);
		}
		if(!isSpeakerphoneOn) {
			setSpeakerphoneOn(true);
		}

	}

	/**
	 * 退出SCO
	 * @return
	 */
	private boolean outofBluetoothScoEnv(){
		setBluetoothA2dpOn(false);
		boolean result = OkBluetooth.HFP.disconnectAudio();
		setBluetoothScoOn(false);
		return result;
	}

	public void setAudioMode(int mode) {
		mAudioManager.setMode(mode);
	}

	public int getAudioMode(){
		return mAudioManager.getMode();
	}

	public void setScoStreamVolumn(int volumnIndex,int flags){
		mAudioManager.setStreamVolume(AudioSystem.STREAM_BLUETOOTH_SCO, (volumnIndex), flags);
	}

	public void setStreamVolumn(int streamType,int volumnIndex,int flags){
		mAudioManager.setStreamVolume(streamType, (volumnIndex), flags);
	}

	public int getCurrentStreamVolumn(int streamType){
		return mAudioManager.getStreamVolume(streamType);
	}

	public int getMaxStreamVolumn(int streamType){
		return mAudioManager.getStreamMaxVolume(streamType);
	}

	public int getScoStreamVolumn(){
		return mAudioManager.getStreamVolume(AudioSystem.STREAM_BLUETOOTH_SCO);
	}

	public int getScoMaxStreamVolumn(){
		return mAudioManager.getStreamMaxVolume(AudioSystem.STREAM_BLUETOOTH_SCO);
	}

	public void setSpeakerphoneOn(boolean on){
		mAudioManager.setSpeakerphoneOn(on);
	}

	public void setBluetoothScoOn(boolean on){
		mAudioManager.setBluetoothScoOn(on);
	}

	public boolean isBluetoothScoOn(){
		return mAudioManager.isBluetoothScoOn();
	}

	public boolean isSpeakerphoneOn(){
		return mAudioManager.isSpeakerphoneOn();
	}

	public void requestAudioFocusForCall(int streamType){
		AudioManager audioManager = mAudioManager;

		try {
			Method method = audioManager.getClass().getDeclaredMethod("requestAudioFocusForCall", new Class[]{int.class,int.class});

			method.invoke(audioManager, streamType,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setAudioModeModifier(AudioModeModifier modifier) {
		this.mAudioModeModifier = (modifier != null ? modifier : AudioModeModifier.DEFAULT);
	}

}
