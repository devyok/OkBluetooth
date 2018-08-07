package com.devyok.bluetooth.demo;

import android.app.Application;

public class DemoApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		OkBluetoothAdapter.onAppReady(this);

	}

}
