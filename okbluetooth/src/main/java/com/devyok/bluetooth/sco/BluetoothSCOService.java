package com.devyok.bluetooth.sco;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.base.BaseBluetoothStateChangedListener;
import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.base.BluetoothService;
import com.devyok.bluetooth.base.StateInformation;
import com.devyok.bluetooth.connection.BluetoothConnectionStateListener;
import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * @author deng.wei
 */
public class BluetoothSCOService extends BluetoothService{

	private static final String TAG = BluetoothSCOService.class.getSimpleName();
	
	private BluetoothConnectionStateListener mScoStateListener = BluetoothConnectionStateListener.EMTPY;
	private final ScoStateListenerImpl stateListenerImpl = new ScoStateListenerImpl();
	
	public BluetoothSCOService(){
	}
	
	@Override
	public boolean init() throws BluetoothException {
		super.init();
		stateListenerImpl.startListener();
		return true;
	}

	@Override
	public boolean destory() {
		super.destory();
		stateListenerImpl.stopListener();
		mScoStateListener = null;
		return true;
	}
	
	public void setConnectionStateListener(BluetoothConnectionStateListener lis){
		this.mScoStateListener = (lis!=null ? lis : BluetoothConnectionStateListener.EMTPY);
	}
	
	public boolean isConnectedSco(){
		AudioManager am = getAudioManager();
		
		boolean isBluetoothScoOn = am.isBluetoothScoOn();
		boolean isSpeakphoneOn = am.isSpeakerphoneOn();
		int audioMode = am.getMode();
		
		if(OkBluetooth.isBluetoothEnable() && isBluetoothScoOn && !isSpeakphoneOn && AudioManager.MODE_IN_COMMUNICATION == audioMode) {
			return true;
		}
		
		return false;
	}
	
	public void startSco(){
		AudioManager am = getAudioManager();
		boolean isBluetoothScoOn = am.isBluetoothScoOn();
		
		Log.i(TAG, "[devybt sco] startTryConnectSco startSco enter , isBluetoothScoOn = " + isBluetoothScoOn);
		if(!am.isBluetoothScoOn()) {
			am.setBluetoothScoOn(true); 
			am.startBluetoothSco();
		}
		Log.i(TAG, "[devybt sco] startTryConnectSco startSco exit");
	}
	
	public void stopSco(){
		AudioManager am = getAudioManager();
		am.stopBluetoothSco();
		am.setBluetoothScoOn(false);
	}
	
	AudioManager getAudioManager(){
		return (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
	}
	
	
	private class ScoStateListenerImpl extends BaseBluetoothStateChangedListener {

		public ScoStateListenerImpl(){
		}

		@Override
		public boolean onChanged(StateInformation information) {
			
			if(OkBluetooth.isDebugable()){
				BluetoothUtils.dumpBluetoothScoStateInfos(TAG, information.getIntent());
			}
			
			int currentState = information.getCurrentState();
			
			switch (currentState) {
			case AudioManager.SCO_AUDIO_STATE_CONNECTING:
				mScoStateListener.onConnecting(information);
				break;
			case AudioManager.SCO_AUDIO_STATE_CONNECTED:
				mScoStateListener.onConnected(information);
				break;
			case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
				mScoStateListener.onDisconnected(information);
				break;
			case AudioManager.SCO_AUDIO_STATE_ERROR:
				mScoStateListener.onError(information);
				break;
			default:
				break;
			}
			
			return true;
		}


		@Override
		public String[] actions() {
			return new String[]{AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED};
		}
		
		
	}

	
}
