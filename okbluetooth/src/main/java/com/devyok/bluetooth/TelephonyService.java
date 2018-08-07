package com.devyok.bluetooth;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.devyok.bluetooth.ConnectionHelper.Event;
import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.base.BluetoothService;
/**
 *
 * @author wei.deng
 */
final class TelephonyService extends BluetoothService{

	private static final String TAG = OkBluetooth.TAG;

	private boolean isListenPhoneState = true;

	/**
	 * 负责监听系统电话状态，当系统电话挂断，则尝试提交恢复音频等连接的请求
	 */
	private volatile PhoneStateListenerImpl phoneStateListenerImpl;
	private TelephonyManager mTelephonyManager;

	public TelephonyService(){
		this(true);
	}

	public TelephonyService(boolean isListenPhoneState){
		this.isListenPhoneState = isListenPhoneState;
	}

	@Override
	public boolean init() throws BluetoothException {
		super.init();
		mTelephonyManager = (TelephonyManager) OkBluetooth.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		listenPhoneState();

		return true;
	}

	@Override
	public boolean destory() {
		super.destory();
		unlistenPhoneState();
		return true;
	}

	private void listenPhoneState() {
		if(isListenPhoneState && phoneStateListenerImpl == null){
			phoneStateListenerImpl = new PhoneStateListenerImpl();
			mTelephonyManager.listen(phoneStateListenerImpl,PhoneStateListener.LISTEN_CALL_STATE);
		}
	}

	private void unlistenPhoneState() {
		if(isListenPhoneState && phoneStateListenerImpl != null){
			mTelephonyManager.listen(phoneStateListenerImpl,PhoneStateListener.LISTEN_NONE);
			phoneStateListenerImpl = null;
		}
	}

	public boolean isPhoneCalling() {
		int callState = mTelephonyManager.getCallState();
		if(callState == TelephonyManager.CALL_STATE_OFFHOOK || callState == TelephonyManager.CALL_STATE_RINGING){
			return true;
		}
		return false;
	}

	class PhoneStateListenerImpl extends PhoneStateListener {

		volatile boolean called = false;

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {

			switch (state) {
				case TelephonyManager.CALL_STATE_IDLE:
					Log.i(TAG, "[TelephonyService] call idle");
					try {
						if(called && OkBluetooth.hasForcePhoneIdle()){
							OkBluetooth.tryRecoveryAudioConnection(Event.PHONECALL_INCALL_TO_IDLE);
						}
					} finally {
						called = false;
					}

					break;
				case TelephonyManager.CALL_STATE_RINGING:
				case TelephonyManager.CALL_STATE_OFFHOOK:
					Log.i(TAG, "[TelephonyService] system phone calling");
					called = true;
					break;
			}
		}
	}


}
