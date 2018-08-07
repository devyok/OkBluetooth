package com.devyok.bluetooth.spp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import com.devyok.bluetooth.connection.BluetoothConnectionException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;
/**
 * @author wei.deng
 */
public class SPPConnectionInsecurePolicy extends AbstractSPPConnection {

	public SPPConnectionInsecurePolicy(){
		this(null,null);
	}

	public SPPConnectionInsecurePolicy(UUID sppuuid,
									   BluetoothDevice connectedDevice) {
		super(sppuuid, connectedDevice);
	}

	static final String TAG = SPPConnectionInsecurePolicy.class.getSimpleName();

	@Override
	public void connect(UUID sppuuid,BluetoothDevice connectedDevice) throws BluetoothConnectionException{

		Log.i(TAG, "[devybt sppconnection] SPPConnectionDefaultPolicy start try connect , isCancel("+isCancel()+") , sppuuid = " + sppuuid);

		if (Build.VERSION.SDK_INT >= 10) { //不安全
			Class<?> cls = BluetoothDevice.class;
			try {
				Method m = cls.getMethod("createInsecureRfcommSocketToServiceRecord",new Class[] { UUID.class });
				transactCore = (BluetoothSocket) m.invoke(connectedDevice,new Object[] { sppuuid });
			} catch (Exception e) {
				Log.i(TAG, "[devybt sppconnection] createInsecureRfcommSocketToServiceRecord Exception");
				throw new BluetoothConnectionException("createInsecureRfcommSocketToServiceRecord exception",e);
			}
		} else {
			throw new BluetoothConnectionException("sdk not support insecure connection");
		}

		try {
			if(!isCancel() && transactCore != null){
				transactCore.connect();
			}
		} catch (IOException e) {
			closeTransactCore();
			Log.i(TAG, "[devybt sppconnection] try connect IOException");
			throw new BluetoothConnectionException("spp connect exception" , e);
		}
	}

}
