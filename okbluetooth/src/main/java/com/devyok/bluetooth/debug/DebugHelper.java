package com.devyok.bluetooth.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.utils.BluetoothUtils;

/**
 * 仅运行在adb环境下
 *
 * 输出系统信息：adb shell am broadcast -a com.devyok.DEBUG_SYSTEM_BLUETOOTH_INFO
 * 弹出控制界面：adb shell am broadcast -a com.devyok.DEBUG_UI_CONSOLE
 * 输出LOG    ：adb logcat -v time debugBluetooth:I *:S
 * 系统社设置	   ：adb shell am start com.android.settings/com.android.settings.Settings
 * @author wei.deng
 */
public class DebugHelper extends BroadcastReceiver {

	public static final String ACTION_DEBUG_SYSTEM_INFO = "com.devyok.DEBUG_SYSTEM_BLUETOOTH_INFO";

	public static final String ACTION_DEBUG_ACTIVITY = "com.devyok.DEBUG_UI_CONSOLE";

	public static final String TAG = "debugBluetooth";

	@Override
	public void onReceive(Context context, Intent intent) {

		if(intent == null) return ;

		if(!OkBluetooth.isReady()) {
			OkBluetooth.init(context);
		}


		String action = intent.getAction();

		if(ACTION_DEBUG_SYSTEM_INFO.equals(action)) {

			BluetoothUtils.dumpBluetoothAllSystemInfos(TAG);

		} else if(ACTION_DEBUG_ACTIVITY.equals(action)){

			Intent impl = new Intent(OkBluetooth.getContext(),DebugUIConsoleActivity.class);
			impl.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			OkBluetooth.getContext().startActivity(impl);

		}
	}

}
