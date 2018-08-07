package com.devyok.bluetooth.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.debug.DebugUIConsoleActivity;

public class MainActivity extends Activity implements OnClickListener{
	static final String TAG = MainActivity.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.findViewById(R.id.start_debug_ui_console).setOnClickListener(this);
		
	}
	
	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(R.id.start_debug_ui_console == id){
			
			Intent impl = new Intent(OkBluetooth.getContext(),DebugUIConsoleActivity.class);

			impl.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			OkBluetooth.getContext().startActivity(impl);
			
		} 
	}
	
}
