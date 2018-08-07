package com.devyok.bluetooth;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.devyok.okbluetooth.R;

import java.util.ArrayList;
/**
 * @author wei.deng
 */
public final class AudioDeviceSelector {

	public interface OnAudioDeviceSelectedListener {

		public static final OnAudioDeviceSelectedListener DEFAULT = new OnAudioDeviceSelectedListener(){
			@Override
			public void onSelected(AudioDevice audioDevice) {
				OkBluetooth.connectAudio(audioDevice);
			}
		};

		public void onSelected(AudioDevice audioDevice);
	}

	public static void showSelector(final Context context){
		showSelector(context, OnAudioDeviceSelectedListener.DEFAULT);
	}

	public static void showSelector(final Context context,OnAudioDeviceSelectedListener listener){

		if(context == null) throw new IllegalArgumentException("context null");

		final OnAudioDeviceSelectedListener selectedListener = (listener == null ? OnAudioDeviceSelectedListener.DEFAULT : listener);

		final Dialog dialog = new Dialog(context);
		dialog.setCanceledOnTouchOutside(true);

		View view = LayoutInflater.from(context).inflate(R.layout.okbt_audio_device_list, null);

		ListView listView = view.findViewById(R.id.okbt_audiodevice_list);

		final ArrayList<String> audioDeivceList = new ArrayList<>();

		//sco
		final String bluetooth = context.getString(R.string.okbt_audiodevice_bluetooth);
		//听筒
		final String earphone = context.getString(R.string.okbt_audiodevice_earphone);
		//扬声器
		final String speaker = context.getString(R.string.okbt_audiodevice_speaker);

		if(OkBluetooth.isBluetoothEnable() && OkBluetooth.HFP.hasConnectedDevice()){
			audioDeivceList.add(bluetooth);
		}

		audioDeivceList.add(earphone);
		audioDeivceList.add(speaker);

		listView.setAdapter(new BaseAdapter() {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				String item = audioDeivceList.get(position);

				View view = LayoutInflater.from(context).inflate(R.layout.okbt_audio_device_list_item, null);

				TextView textView = (TextView) view.findViewById(R.id.okbt_audiodevice_name);
				textView.setText(item);

				return view;
			}

			@Override
			public long getItemId(int position) {
				return 0;
			}

			@Override
			public Object getItem(int position) {
				return audioDeivceList.get(position);
			}

			@Override
			public int getCount() {
				return audioDeivceList.size();
			}
		});

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
									long arg3) {

				String selected = audioDeivceList.get(pos);

				if(bluetooth.equals(selected)) {

					selectedListener.onSelected(AudioDevice.SCO);

				} else if(earphone.equals(selected)) {

					selectedListener.onSelected(AudioDevice.WIREDHEADSET);

				} else if(speaker.equals(selected)) {

					selectedListener.onSelected(AudioDevice.SPEAKER);

				} else {

					selectedListener.onSelected(AudioDevice.SBP);

				}

				dialog.dismiss();

			}

		});

		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(view);
		dialog.show();

	}

}
