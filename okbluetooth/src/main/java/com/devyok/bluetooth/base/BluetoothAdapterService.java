package com.devyok.bluetooth.base;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.os.Build;
import android.util.Log;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.OkBluetooth.Callback2;
import com.devyok.bluetooth.utils.BluetoothUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
/**
 * @author deng.wei
 */
public class BluetoothAdapterService extends BluetoothService{

	private static String TAG = BluetoothAdapterService.class.getSimpleName();

	private BluetoothAdapterStateListener mAdapterStateListener = BluetoothAdapterStateListener.EMPTY;
	private final BluetoothAdapterStateListenerImpl mAdapterStateListenerImpl = new BluetoothAdapterStateListenerImpl();

	public BluetoothAdapterService(){
	}

	public int getProfileConnectionState(int profile) {
		return BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(profile);
	}

	@Override
	public boolean init() throws BluetoothException {
		super.init();
		mAdapterStateListenerImpl.startListener();
		return true;
	}

	@Override
	public boolean destory() {
		super.destory();
		mAdapterStateListenerImpl.stopListener();
		mAdapterStateListener = null;
		return true;
	}

	public int getConnectionState() {

		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

		try {
			Method m = adapter.getClass().getDeclaredMethod("getConnectionState", null);

			try {
				Object result = m.invoke(adapter, null);

				if(result!=null){
					return Integer.valueOf(result.toString());
				}

			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}

		return BluetoothAdapter.STATE_DISCONNECTED;
	}

	public Set<BluetoothDevice> getBondedDevices(){
		return BluetoothAdapter.getDefaultAdapter().getBondedDevices();
	}

	public void setBluetoothAdapterStateListener(BluetoothAdapterStateListener lis){
		this.mAdapterStateListener = (lis!=null ? lis : BluetoothAdapterStateListener.EMPTY);
	}

	/**
	 * 打开蓝牙
	 */
	public void enable(){
		BluetoothAdapter.getDefaultAdapter().enable();
	}

	public void closeBluetoothProfile(int profile,BluetoothProfile bluetoothProfile) {
		if(bluetoothProfile==null) return ;
		BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, bluetoothProfile);
	}

	public void closeBluetoothProfile(BluetoothProfile bluetoothProfile) {
		if(bluetoothProfile==null) return ;
		if (bluetoothProfile instanceof BluetoothA2dp) {
			closeBluetoothProfile(BluetoothProfile.A2DP,bluetoothProfile);
		} else if (bluetoothProfile instanceof BluetoothHeadset) {
			closeBluetoothProfile(BluetoothProfile.HEADSET,bluetoothProfile);
		}
	}

	public boolean isEnable(){
		return BluetoothAdapter.getDefaultAdapter().isEnabled();
	}

	public boolean disable(){
		return BluetoothAdapter.getDefaultAdapter().disable();
	}

	public boolean getProfileService(final int profileParam,final ServiceListener serviceListener){

		boolean result = BluetoothAdapter.getDefaultAdapter().getProfileProxy(OkBluetooth.getContext(), new ServiceListener() {

			@Override
			public void onServiceDisconnected(int profile) {

				if(serviceListener!=null){
					serviceListener.onServiceDisconnected(profile);
				}

			}

			public void onServiceConnected(int profile, BluetoothProfile proxy) {

				if(serviceListener!=null){
					serviceListener.onServiceConnected(profile, proxy);
				}

			}
		}, profileParam);

		return result;
	}

	public void getConnectedBluetoothDevice(final int profileParam,
											final Callback2<BluetoothProfile,List<BluetoothDevice>> serviceConnectedCallback){

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

			boolean result = getProfileService(profileParam, new ServiceListener() {

				void handleCallback(BluetoothProfile profile,List<BluetoothDevice> list) {
					if(serviceConnectedCallback!=null){
						serviceConnectedCallback.callback(profile, list);
					}
				}

				@Override
				public void onServiceDisconnected(int profile) {

					Log.i(TAG, "[devybt connect] getConnectedBluetoothDevice BluetoothProfile("+BluetoothUtils.getProfileString(profile)+") onServiceDisconnected()");

					if(serviceConnectedCallback!=null){
						serviceConnectedCallback.callback(null, null);
					}

				}

				public void onServiceConnected(int profile, BluetoothProfile proxy) {
					try {
						switch (profile) {
							case BluetoothProfile.A2DP:
							case BluetoothProfile.HEADSET:

								Log.i(TAG, "[devybt connect] getConnectedBluetoothDevice BluetoothProfile("+BluetoothUtils.getProfileString(profile)+") onServiceConnected()");

								List<BluetoothDevice> connectedDevices = proxy.getConnectedDevices();

								Log.i(TAG, "[devybt connect] getConnectedBluetoothDevice BluetoothProfile("+BluetoothUtils.getProfileString(profile)+") connectedDevices(size = "+connectedDevices.size()+")");

								handleCallback(proxy,connectedDevices);

								break;
							case BluetoothProfile.HEALTH:
								Log.i(TAG, "[devybt connect] BluetoothProfile.HEALTH onServiceConnected()");
								handleCallback(null,null);
								break;
							default:
								Log.i(TAG, "[devybt connect] BluetoothProfile.default onServiceConnected()");
								handleCallback(null,null);
								break;
						}

					} catch(Exception e){
						handleCallback(null,null);
					}

				}
			});


			if(!result){ //get error , notify
				if(serviceConnectedCallback!=null){
					serviceConnectedCallback.callback(null, null);
				}
			}

			Log.i(TAG, "[devybt connect] BluetoothProfile.getProfileProxy() result = " + result);

		} else {
			if(serviceConnectedCallback!=null){
				serviceConnectedCallback.callback(null, null);
			}
		}

	}

	private class BluetoothAdapterStateListenerImpl extends BaseBluetoothStateChangedListener {

		public BluetoothAdapterStateListenerImpl() {
		}

		@Override
		public boolean onChanged(StateInformation information) {

			if(OkBluetooth.isDebugable()) {
				BluetoothUtils.dumpBluetoothSystemSwitchStateInfos(TAG, information.getIntent());
			}

			int currentState = information.getCurrentState();

			switch (currentState) {
				case BluetoothAdapter.STATE_TURNING_ON:
					mAdapterStateListener.onOpening(information);
					break;
				case BluetoothAdapter.STATE_ON:
					mAdapterStateListener.onOpened(information);
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
					mAdapterStateListener.onCloseing(information);
					break;
				case BluetoothAdapter.STATE_OFF:
					mAdapterStateListener.onClosed(information);
					break;
				default:
					break;
			}

			return true;
		}

		@Override
		public String[] actions() {
			return new String[]{BluetoothAdapter.ACTION_STATE_CHANGED};
		}

	}







}
