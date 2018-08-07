package com.devyok.bluetooth.hfp;

import java.io.Closeable;
import java.io.IOException;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.util.Log;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.connection.BluetoothConnection.Protocol;
import com.devyok.bluetooth.message.BluetoothMessage;
import com.devyok.bluetooth.message.BluetoothMessageDispatcher;
import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * @author wei.deng
 */
class HFPConnectionImpl extends BroadcastReceiver implements Closeable{

	static String TAG = HFPConnectionImpl.class.getSimpleName();
	
	private HFPConnectionImpl(){
	}
	
	public static HFPConnectionImpl connect(int companyid) throws BluetoothException {
		
		try {
			HFPConnectionImpl connectionImpl = new HFPConnectionImpl();
			
			IntentFilter intentFilter = new IntentFilter(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
			
			if(companyid<0){
				intentFilter.addCategory("android.bluetooth.headset.intent.category.companyid");
			} else {
				intentFilter.addCategory("android.bluetooth.headset.intent.category.companyid"+"." + companyid);
			}
			
			OkBluetooth.getContext().registerReceiver(connectionImpl, intentFilter);
			
			return connectionImpl;
		} catch(Exception e){
			throw new BluetoothException("connect fail", e);
		}
	}
	
	@Override
	public void close() throws IOException {
		try {
			OkBluetooth.getContext().unregisterReceiver(this);
		} catch(Exception e){
			throw new IOException("unregisterReceiver fail",e);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if(intent == null){
			return ;
		}
		
		String cmd = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD);
		Object[] cmdArgs = (Object[]) intent.getExtras().get(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS);
		int cmdType = intent.getIntExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, -1);
		Parcelable device = (Parcelable)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		String cmdTypeString = BluetoothUtils.getHeadsetEventTypeString(cmdType);
		
		String cmdArgsString = BluetoothUtils.toString(cmdArgs);
		
		BluetoothMessage<String> bluetoothMessage = BluetoothMessage.obtain(cmdArgsString,Protocol.HFP);
		
		if(device!=null){
			bluetoothMessage.setBluetoothDevice((BluetoothDevice) device);
		}
		
		BluetoothMessageDispatcher.dispatch(bluetoothMessage);
		
		Log.i(TAG, "[devybt sppconnection] cmd = " + cmd + ", args = " + cmdArgsString + " , cmdTypeString = " + cmdTypeString);
	}

}
