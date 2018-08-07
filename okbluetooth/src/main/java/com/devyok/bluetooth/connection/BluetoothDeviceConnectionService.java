package com.devyok.bluetooth.connection;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.base.BaseBluetoothStateChangedListener;
import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.base.BluetoothService;
import com.devyok.bluetooth.base.StateInformation;
import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * @author deng.wei
 */
public class BluetoothDeviceConnectionService extends BluetoothService{

	private static String TAG = BluetoothDeviceConnectionService.class.getSimpleName();
	
	private BluetoothConnectionStateListener mConnectionStateListener = BluetoothConnectionStateListener.EMTPY;
	private final BluetoothDeviceConnectionStateListenerImpl stateListenerImpl = new BluetoothDeviceConnectionStateListenerImpl();
	
	public BluetoothDeviceConnectionService(){
	}
	
	public void setConnectionStateListener(BluetoothConnectionStateListener lis){
		this.mConnectionStateListener = lis;
	}
	
	@Override
	public boolean init() throws BluetoothException {
		// TODO Auto-generated method stub
		super.init();
		stateListenerImpl.startListener();
		return true;
		
	}
	
	@Override
	public boolean destory() {
		super.destory();
		stateListenerImpl.stopListener();
		mConnectionStateListener = null;
		return true;
	}
	
	class BluetoothDeviceConnectionStateListenerImpl extends BaseBluetoothStateChangedListener {

		public BluetoothDeviceConnectionStateListenerImpl(){
		}
		
		@Override
		public boolean onChanged(StateInformation stateInformation){
			
			if(OkBluetooth.isDebugable()){
				
				int connectionState = OkBluetooth.getConnectionState();
				String getConnectionStateString = BluetoothUtils.getConnectionStateString(connectionState);
				
				Log.i(TAG, "[devybt connect] getConnectionStateString = " + getConnectionStateString);
				
				BluetoothUtils.dumpBluetoothConnectionInfos(TAG, stateInformation.getIntent());
			}
			
			int currentState = stateInformation.getCurrentState();
			
			switch (currentState) {
			case BluetoothAdapter.STATE_CONNECTING:
				mConnectionStateListener.onConnecting(stateInformation);
				break;
			case BluetoothAdapter.STATE_CONNECTED:
				mConnectionStateListener.onConnected(stateInformation);
				break;
			case BluetoothAdapter.STATE_DISCONNECTING:
				mConnectionStateListener.onDisconnecting(stateInformation);
				break;
			case BluetoothAdapter.STATE_DISCONNECTED:
				mConnectionStateListener.onDisconnected(stateInformation);
				break;

			default:
				break;
			}
			
			return true;
		}
		
		@Override
		public String[] actions(){
			return new String[]{BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED};
		}
		
	}

	
}
