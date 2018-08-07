package com.devyok.bluetooth.debug;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.devyok.bluetooth.AudioDevice;
import com.devyok.bluetooth.AudioDeviceSelector;
import com.devyok.bluetooth.OkBluetooth;
import com.devyok.okbluetooth.R;
import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.base.BluetoothProfileService.ProfileConnectionState;
import com.devyok.bluetooth.connection.BluetoothConnection;
import com.devyok.bluetooth.connection.BluetoothConnection.BluetoothConnectionListener;
import com.devyok.bluetooth.connection.BluetoothConnectionException;
import com.devyok.bluetooth.connection.BluetoothConnectionTimeoutException;
import com.devyok.bluetooth.connection.Connection;
import com.devyok.bluetooth.connection.DefaultRetryPolicy;
import com.devyok.bluetooth.hfp.HFPConnection;
import com.devyok.bluetooth.hfp.HeadsetProfileService;
import com.devyok.bluetooth.spp.SPPConnectionSecurePolicy;
import com.devyok.bluetooth.utils.BluetoothUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * @author wei.deng
 */
public class DebugUIConsoleActivity extends Activity implements OnClickListener{

	static final String TAG = DebugUIConsoleActivity.class.getSimpleName();
	
	static Handler handler = new Handler(new Handler.Callback() {
		
		@Override
		public boolean handleMessage(Message msg) {
			String message = msg.obj.toString();
			
			Toast.makeText(OkBluetooth.getContext(), message, 1).show();
			return true;
		}
	});
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.okbt_activity_debug_ui_console);
		
		if(!OkBluetooth.isReady()){
			OkBluetooth.init(getApplicationContext());
		}
		
		
		this.findViewById(R.id.okbt_socket_disconnect).setOnClickListener(this);
		this.findViewById(R.id.okbt_socket_connect).setOnClickListener(this);
		this.findViewById(R.id.okbt_output_bt_infos).setOnClickListener(this);
		this.findViewById(R.id.okbt_music_play).setOnClickListener(this);
		this.findViewById(R.id.okbt_music_pause).setOnClickListener(this);
		this.findViewById(R.id.okbt_start_sco).setOnClickListener(this);
		this.findViewById(R.id.okbt_stop_sco).setOnClickListener(this);
		this.findViewById(R.id.okbt_check_and_recovery_bt_connection).setOnClickListener(this);
		this.findViewById(R.id.okbt_request_audio_focus).setOnClickListener(this);
		
		
		this.findViewById(R.id.okbt_on_sco).setOnClickListener(this);
		this.findViewById(R.id.okbt_off_sco).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_on_a2dp).setOnClickListener(this);
		this.findViewById(R.id.okbt_off_a2dp).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_hfp_connect).setOnClickListener(this);
		this.findViewById(R.id.okbt_hfp_disconnect).setOnClickListener(this);
		this.findViewById(R.id.okbt_set_audio_in_call).setOnClickListener(this);
		this.findViewById(R.id.okbt_set_audio_in_commu).setOnClickListener(this);
		this.findViewById(R.id.okbt_set_audio_normal).setOnClickListener(this);
		this.findViewById(R.id.okbt_open_speaker).setOnClickListener(this);
		this.findViewById(R.id.okbt_close_speaker).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_connect_system_hfp).setOnClickListener(this);
		this.findViewById(R.id.okbt_disconnect_system_hfp).setOnClickListener(this);
		this.findViewById(R.id.okbt_restart_system_hfp).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_connect_system_hfp_audio).setOnClickListener(this);
		this.findViewById(R.id.okbt_disconnect_system_hfp_audio).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_connect_system_a2dp).setOnClickListener(this);
		this.findViewById(R.id.okbt_disconnect_system_a2dp).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_connect_system_a2dp_by_androidapi).setOnClickListener(this);
		this.findViewById(R.id.okbt_disconnect_system_a2dp_by_androidapi).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_set_pri_auto).setOnClickListener(this);
		this.findViewById(R.id.okbt_set_pri_on).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_set_stream_music).setOnClickListener(this);
		this.findViewById(R.id.okbt_set_stream_voice_call).setOnClickListener(this);
		this.findViewById(R.id.okbt_set_dtmf).setOnClickListener(this);
		this.findViewById(R.id.okbt_set_system).setOnClickListener(this);
		this.findViewById(R.id.okbt_set_sco).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_start_audiomanager_sco).setOnClickListener(this);
		this.findViewById(R.id.okbt_stop_audiomanager_sco).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_inc_bt_volumn).setOnClickListener(this);
		this.findViewById(R.id.okbt_des_bt_volumn).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_inc_system_volumn).setOnClickListener(this);
		this.findViewById(R.id.okbt_des_system_volumn).setOnClickListener(this);
		
		
		this.findViewById(R.id.okbt_quit_user_interface_event).setOnClickListener(this);
		this.findViewById(R.id.okbt_audio_device_list_dlg).setOnClickListener(this);
		
		this.findViewById(R.id.okbt_start_watch_dog).setOnClickListener(this);
		
		
	}
	
	static BluetoothDevice currentDisconnectSystemHFPBluetoothDevice;
	
	private static int MEIDA_PLAYER_STREAM = AudioManager.STREAM_VOICE_CALL;
	
	@Override
	public void onClick(View v) {
		int id = v.getId();
		
		
		if(R.id.okbt_start_watch_dog == id){
			
			startWatchDog();
			
		}
		
		else if(R.id.okbt_audio_device_list_dlg == id){
			AudioDeviceSelector.showSelector(this);
		}
		
		else if(R.id.okbt_quit_user_interface_event == id){
			
			OkBluetooth.connectAudio(AudioDevice.SBP);
			
		} else if(R.id.okbt_request_audio_focus == id){
			
			OkBluetooth.requestAudioFocusForCall(AudioManager.STREAM_VOICE_CALL);
			
			showToast("requestAudioFocusForCall ok");
			
		} else if(R.id.okbt_socket_connect == id){
			
			List<BluetoothDevice> list = OkBluetooth.HFP.getConnectedBluetoothDeviceList();
			
			if(list!=null && list.size() == 0){
				showToast("not found connected bt devices");
				return ;
			}
			
			final BluetoothDevice bluetoothDevice = list.get(0);
			
			connectspp(bluetoothDevice,true);
			
		} else if(R.id.okbt_socket_disconnect == id){
			disconnectspp();
		}
		else if(R.id.okbt_check_and_recovery_bt_connection == id){
			
//			BluetoothConnectionHelper.getHelper().tryRecoveryAudioConnection();
			
		} else if(R.id.okbt_inc_bt_volumn == id){
			
			AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
			
			int btScoVolumn = am.getStreamVolume(AudioSystem.STREAM_BLUETOOTH_SCO);
			showToast(""+btScoVolumn);
			
			am.setStreamVolume(AudioSystem.STREAM_BLUETOOTH_SCO, (++btScoVolumn), AudioManager.FLAG_PLAY_SOUND);
			
			
		} else if(R.id.okbt_des_bt_volumn == id){
			
			AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
			
			int btScoVolumn = am.getStreamVolume(AudioSystem.STREAM_BLUETOOTH_SCO);
			showToast(""+btScoVolumn);
			am.setStreamVolume(AudioSystem.STREAM_BLUETOOTH_SCO, (--btScoVolumn), AudioManager.FLAG_PLAY_SOUND);
		}
		
		
		else if(R.id.okbt_inc_system_volumn == id){
			
			AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
			
			int systemScoVolumn = am.getStreamVolume(AudioSystem.STREAM_SYSTEM);
			showToast(""+systemScoVolumn);
			
			am.setStreamVolume(AudioSystem.STREAM_SYSTEM, (++systemScoVolumn), AudioManager.FLAG_PLAY_SOUND);
			
			
		} else if(R.id.okbt_des_system_volumn == id){
			
			AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
			
			int systemScoVolumn = am.getStreamVolume(AudioSystem.STREAM_SYSTEM);
			showToast(""+systemScoVolumn);
			
			am.setStreamVolume(AudioSystem.STREAM_SYSTEM, (--systemScoVolumn), AudioManager.FLAG_PLAY_SOUND);
		}
		
		
		else if(R.id.okbt_start_audiomanager_sco == id){
			
			OkBluetooth.startSco();
			
		} else if(R.id.okbt_stop_audiomanager_sco == id){
			
			OkBluetooth.stopSco();
			
		}
		
		else if(R.id.okbt_on_sco == id){
			AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
			am.setBluetoothScoOn(true);
		} else if(R.id.okbt_off_sco == id){
			AudioManager am = (AudioManager) OkBluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
			am.setBluetoothScoOn(false);
		} else if(R.id.okbt_on_a2dp == id){
			
			IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
			IAudioService sService = IAudioService.Stub.asInterface(b);
			
			try {
				boolean result = sService.isBluetoothA2dpOn();
				Log.i(TAG, "isBluetoothA2dpOn = " + result);
				sService.setBluetoothA2dpOn(true);
			} catch (RemoteException e) {
				e.printStackTrace();
				Log.i(TAG, "on a2dp exception = " + e.getMessage());
			}
			
		} else if(R.id.okbt_off_a2dp == id){
			IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
			IAudioService sService = IAudioService.Stub.asInterface(b);
			
			try {
				boolean result = sService.isBluetoothA2dpOn();
				Log.i(TAG, "isBluetoothA2dpOn = " + result);
				sService.setBluetoothA2dpOn(false);
			} catch (RemoteException e) {
				e.printStackTrace();
				Log.i(TAG, "off a2dp exception = " + e.getMessage());
			}
		}
		
		else if(R.id.okbt_set_stream_music == id) {
			
			MEIDA_PLAYER_STREAM = AudioManager.STREAM_MUSIC;
			
		} else if(R.id.okbt_set_stream_voice_call == id) {
			
			MEIDA_PLAYER_STREAM = AudioManager.STREAM_VOICE_CALL;
		}else if(R.id.okbt_set_dtmf == id) {
			
			MEIDA_PLAYER_STREAM = AudioManager.STREAM_DTMF;
		}else if(R.id.okbt_set_system == id) {
			
			MEIDA_PLAYER_STREAM = AudioManager.STREAM_SYSTEM;
		}else if(R.id.okbt_set_sco == id) {
			
			MEIDA_PLAYER_STREAM = AudioSystem.STREAM_BLUETOOTH_SCO;
		}
		
		else if(R.id.okbt_set_pri_auto == id){
			if(currentDisconnectSystemHFPBluetoothDevice!=null){
				boolean result = OkBluetooth.setPriority(BluetoothProfile.A2DP, BluetoothProfile.PRIORITY_AUTO_CONNECT, currentDisconnectSystemHFPBluetoothDevice);
				Log.i(TAG, "set a2dp priority_auto result = " + result);
			}
		} else if(R.id.okbt_set_pri_on == id){
			if(currentDisconnectSystemHFPBluetoothDevice!=null){
				boolean result = OkBluetooth.setPriority(BluetoothProfile.A2DP, BluetoothProfile.PRIORITY_ON, currentDisconnectSystemHFPBluetoothDevice);
				Log.i(TAG, "set a2dp priority_on result = " + result);
			}
		}
		else if(R.id.okbt_connect_system_a2dp_by_androidapi == id){
			
			BluetoothUtils.setBluetoothA2dpOn(true);
			
		} else if(R.id.okbt_disconnect_system_a2dp_by_androidapi == id){
			
			BluetoothUtils.setBluetoothA2dpOn(false);
			
		}
		else if(R.id.okbt_connect_system_a2dp == id){
			
			connectSystemA2dp();
			
		} else if(R.id.okbt_disconnect_system_a2dp == id){
			
			disconnectSystemA2dp();
		} else if(R.id.okbt_connect_system_hfp_audio == id){
			
			connectSystemHFPAudio();//sco
			
		} else if(R.id.okbt_disconnect_system_hfp_audio == id){
			
			disconnectSystemHFPAudio(); //sco
			
		} else if(R.id.okbt_restart_system_hfp == id){
			
			
		} else if(R.id.okbt_connect_system_hfp == id){
			
			connectSystemHFP();
			
		} else if(R.id.okbt_disconnect_system_hfp == id){
			
			disconnectSystemHFP();
			
		} else if(R.id.okbt_output_bt_infos == id) {
			
			BluetoothUtils.dumpBluetoothAllSystemInfos(DebugHelper.TAG);
			
		} else if(R.id.okbt_start_sco == id){
			
			boolean result = OkBluetooth.HFP.connectAudio();
			Log.i(TAG, "start sco result = " + result);
			
		} else if(R.id.okbt_stop_sco == id) {
				
			boolean result = OkBluetooth.HFP.disconnectAudio();
			
			Log.i(TAG, "stop sco result = " + result);
			
		} else if(R.id.okbt_music_play == id){
			
			play();
			
		} else if(R.id.okbt_music_pause == id){
			
			stop();
			
		} else if(R.id.okbt_hfp_connect == id){
			
			connecthfp();
			
		} else if(R.id.okbt_hfp_disconnect == id){
			
			disconnectBlutoothConnection();
			
		} else if(R.id.okbt_set_audio_in_call == id){
			
			BluetoothUtils.setAudioMode(AudioManager.MODE_IN_CALL);
			showToast("set MODE_IN_CALL success");
			
		} else if(R.id.okbt_set_audio_in_commu == id){
			
			BluetoothUtils.setAudioMode(AudioManager.MODE_IN_COMMUNICATION);
			showToast("set MODE_IN_COMMUNICATION success");
			
		} else if(R.id.okbt_set_audio_normal == id){
			
			BluetoothUtils.setAudioMode(AudioManager.MODE_NORMAL);
			showToast("set MODE_NORMAL success");
			
		} else if(R.id.okbt_open_speaker == id){
			
			BluetoothUtils.openSpeaker();
			
		}else if(R.id.okbt_close_speaker == id){
			
			BluetoothUtils.closeSpeaker();
			
		}
	}
	
	private void startWatchDog() {
		
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				
				OkBluetooth.tryRecoveryAudioConnection();
				
				handler.postDelayed(this, 2*1000);
			}
		}, 2*1000);
		
	}

	private void connectSystemA2dp() {
		if(currentDisconnectSystemHFPBluetoothDevice!=null){
			
			boolean isSupportHFP = OkBluetooth.isSupportHFP(currentDisconnectSystemHFPBluetoothDevice);
			boolean isSupportA2dp = OkBluetooth.isSupportA2DP(currentDisconnectSystemHFPBluetoothDevice);
			
			Log.i(TAG, "current disconnect device name = " + currentDisconnectSystemHFPBluetoothDevice.getName() + " , state = " + currentDisconnectSystemHFPBluetoothDevice.getBondState() + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);
			
			if(isSupportA2dp){
				boolean result = OkBluetooth.A2DP.connect(currentDisconnectSystemHFPBluetoothDevice);
				Log.i(TAG, "connect system a2dp result = " + result);
			}
			
		}
	}
	
	private void connectSystemHFP() {
		if(currentDisconnectSystemHFPBluetoothDevice!=null){
			
			boolean isSupportHFP = OkBluetooth.isSupportHFP(currentDisconnectSystemHFPBluetoothDevice);
			boolean isSupportA2dp = OkBluetooth.isSupportA2DP(currentDisconnectSystemHFPBluetoothDevice);
			
			Log.i(TAG, "current disconnect device name = " + currentDisconnectSystemHFPBluetoothDevice.getName() + " , state = " + currentDisconnectSystemHFPBluetoothDevice.getBondState() + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);
			
			boolean result = OkBluetooth.HFP.connect(currentDisconnectSystemHFPBluetoothDevice);
			
			Log.i(TAG, "connect system hfp result = " + result);
			
			return;
		}
	}

	private void disconnectSystemHFPAudio() {
		LinkedHashMap<BluetoothDevice, ProfileConnectionState> map = OkBluetooth.getAllProfileConnectionState();
				
		for(Iterator<Map.Entry<BluetoothDevice, ProfileConnectionState>> iter = map.entrySet().iterator();iter.hasNext();){
			
			Map.Entry<BluetoothDevice, ProfileConnectionState> item = iter.next();
			
			final BluetoothDevice bluetoothDevice = item.getKey();
			ProfileConnectionState state = item.getValue();
			
			boolean isSupportHFP = OkBluetooth.isSupportHFP(bluetoothDevice);
			boolean isSupportA2dp = OkBluetooth.isSupportA2DP(bluetoothDevice);
			
			Log.i(TAG, "device name = " + bluetoothDevice.getName() + " , state = " + state + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);
			
			if(isSupportHFP && state == ProfileConnectionState.CONNECTED) {
				currentDisconnectSystemHFPBluetoothDevice = bluetoothDevice;
				boolean result = OkBluetooth.HFP.disconnectAudio();
				Log.i(TAG, "disconnect system hfp audio result = " + result);
				
			}
			
		}
		
	}

	private void connectSystemHFPAudio() {
		if(currentDisconnectSystemHFPBluetoothDevice!=null){
			
			boolean isSupportHFP = OkBluetooth.isSupportHFP(currentDisconnectSystemHFPBluetoothDevice);
			boolean isSupportA2dp = OkBluetooth.isSupportA2DP(currentDisconnectSystemHFPBluetoothDevice);
			
			Log.i(TAG, "current disconnect device name = " + currentDisconnectSystemHFPBluetoothDevice.getName() + " , state = " + currentDisconnectSystemHFPBluetoothDevice.getBondState() + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);
			
			boolean result = OkBluetooth.HFP.connectAudio();
			
			Log.i(TAG, "connect system hfp audio result = " + result);
		}
	}

	private void disconnectSystemHFP() {
		LinkedHashMap<BluetoothDevice, ProfileConnectionState> map = OkBluetooth.getAllProfileConnectionState();
				
		for(Iterator<Map.Entry<BluetoothDevice, ProfileConnectionState>> iter = map.entrySet().iterator();iter.hasNext();){
			
			Map.Entry<BluetoothDevice, ProfileConnectionState> item = iter.next();
			
			final BluetoothDevice bluetoothDevice = item.getKey();
			ProfileConnectionState state = item.getValue();
			
			boolean isSupportHFP = OkBluetooth.isSupportHFP(bluetoothDevice);
			boolean isSupportA2dp = OkBluetooth.isSupportA2DP(bluetoothDevice);
			
			Log.i(TAG, "device name = " + bluetoothDevice.getName() + " , state = " + state + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);
			
			
			if(isSupportHFP) {
				
				currentDisconnectSystemHFPBluetoothDevice = bluetoothDevice;
				boolean result = OkBluetooth.HFP.disconnect(bluetoothDevice);
				Log.i(TAG, "disconnect system hfp result = " + result);
				
			}
			
		}
		
	}


	private static void disconnectSystemA2dp() {
		LinkedHashMap<BluetoothDevice, ProfileConnectionState> map = OkBluetooth.getAllProfileConnectionState();
				
		for(Iterator<Map.Entry<BluetoothDevice, ProfileConnectionState>> iter = map.entrySet().iterator();iter.hasNext();){
			
			Map.Entry<BluetoothDevice, ProfileConnectionState> item = iter.next();
			
			final BluetoothDevice bluetoothDevice = item.getKey();
			ProfileConnectionState state = item.getValue();
			
			boolean isSupportHFP = OkBluetooth.isSupportHFP(bluetoothDevice);
			boolean isSupportA2dp = OkBluetooth.isSupportA2DP(bluetoothDevice);
			
			Log.i(TAG, "device name = " + bluetoothDevice.getName() + " , state = " + state + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);
			
			
			if(isSupportA2dp && ProfileConnectionState.isConnected(state)) {
				
				currentDisconnectSystemHFPBluetoothDevice = bluetoothDevice;
				
				boolean result = OkBluetooth.A2DP.disconnect(bluetoothDevice);
				Log.i(TAG, "disconnect system a2dp result = " + result);
			}
			
		}
				
	}

	private static void showToast(String message){
		handler.sendMessage(Message.obtain(handler, 0, message));
	}
	
	static MediaPlayer mediaPlayer = null;
	
	public static void play(){
		if(mediaPlayer == null){
			try {
				mediaPlayer = new MediaPlayer();
		    	AssetFileDescriptor fd = OkBluetooth.getContext().getAssets().openFd("test.mp3");
		    	mediaPlayer.setDataSource(fd.getFileDescriptor());
		    	Log.i(TAG, "setStreamType for mediaPlayer = " + BluetoothUtils.getAudioStreamTypeString(MEIDA_PLAYER_STREAM));
		    	mediaPlayer.setAudioStreamType(MEIDA_PLAYER_STREAM);
		        mediaPlayer.prepare();
		        mediaPlayer.setLooping(true);
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
		
		mediaPlayer.start();
	}
	
	public static void stop(){
		if(mediaPlayer!=null){
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}
	
	private static void disconnectspp(){
		if(bluetoothConnection != null){
			bluetoothConnection.disconnect();
			bluetoothConnection = null;
		}
	}
	
	private static void connectspp(final BluetoothDevice bluetoothDevice,final boolean isSupportRestartSystemHfp){
		
//		disconnectspp();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
					try {
						new BluetoothConnection.Builder()
								.setConnectionUUID(BluetoothConnection.DEFAULT_UUID)
//								.setConnectionUUID(UUID.randomUUID())
								.setConnectedDevice(bluetoothDevice)
								.setConnectionTimeout(3*1000)
//								.setConnectionRetryPolicy(new DefaultRetryPolicy())
//								.addConnectionPolicy(new SPPConnectionInsecurePolicy())
								.addConnectionPolicy(new SPPConnectionSecurePolicy())
//								.addConnectionPolicy(new SppConnectionLoopChannelPolicy())
								.setConnectionRetryPolicy(new DefaultRetryPolicy(1))
								.setConnectionListener(new BluetoothConnectionListener() {

									@Override
									public void onConnected(Connection connection) {
										Log.i(TAG, "SPP Connected");
										bluetoothConnection = connection;
										showToast("SPP Connected");
									}

									@Override
									public void onDisconnected(Connection connection) {
										Log.i(TAG, "SPP Disconnected");
										showToast("SPP Disconnected");
									}
									
								})
								.build()
								.connect();
						
						
						Log.i(TAG, "start SPP connect , isSupportRestartSystemHfp = " + isSupportRestartSystemHfp);
						
					} catch (BluetoothConnectionException e) {
						e.printStackTrace();
						Log.i(TAG, "SPP BluetoothConnectionException = " + e.getMessage());
						showToast("SPP BluetoothConnectionException = " + e.getMessage());
						if(isSupportRestartSystemHfp) {
							syncRebuildHfpConnection(bluetoothDevice);
						}
						
					} catch (BluetoothConnectionTimeoutException e) {
						e.printStackTrace();
						Log.i(TAG, "SPP BluetoothConnectionException = " + e.getMessage());
						showToast("SPP  BluetoothConnectionTimeoutException");
					} catch (BluetoothException e) {
						e.printStackTrace();
						Log.i(TAG, "SPP BluetoothConnectionException = " + e.getMessage());
						showToast("SPP BluetoothException");
					}
			}
		}).start();
		
	}
	
	private static void syncRebuildHfpConnection(final BluetoothDevice device){
		
		if(!OkBluetooth.isBluetoothEnable()) return ;
		
		boolean isConnected = OkBluetooth.HFP.isConnected(device);
		
		Log.i(TAG, "syncReconnectSystemHFP isConnected = " + isConnected + " , deivce name = " + device.getName());
		
		if(isConnected){
			
			boolean disconnectResult = OkBluetooth.HFP.disconnect(device);
			int priority = OkBluetooth.HFP.getPriority(device);
			Log.i(TAG, "disconnect = " + disconnectResult + " , priority = " + BluetoothUtils.getDevicePriority(priority));
			
			int count = 0;
			int maxConnectCount = 3;
			
			while(true){
				try {
					boolean connectResult = OkBluetooth.HFP.connect(device);
					Log.i(TAG, "while item connectResult = " + connectResult + " , priority = " + BluetoothUtils.getDevicePriority(priority));
					try {
						Thread.sleep((1*1000 + (count * 200)));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					isConnected = OkBluetooth.HFP.isConnected(device);
					if(OkBluetooth.isBluetoothEnable() && (isConnected || (count >= maxConnectCount))){
						break;
					}
					
					++count;
				} catch(Throwable e){
					break;
				}
				
			}
			
			BluetoothDevice bluetoothDevice = OkBluetooth.HFP.getFristConnectedDevice();
			if(bluetoothDevice!=null && OkBluetooth.isSupportSPP(bluetoothDevice)) {
				connectspp(device,false);
			}
			
			
		} else {
			
			BluetoothDevice bluetoothDevice = OkBluetooth.HFP.getFristConnectedDevice();
			if(bluetoothDevice!=null && OkBluetooth.isSupportSPP(bluetoothDevice)) {
				connectspp(bluetoothDevice,true);
			}
			
		}
		
	}
	
	private static void disconnectBlutoothConnection(){
		if(bluetoothConnection!=null){
			bluetoothConnection.disconnect();
			BluetoothUtils.dumpBluetoothConnection(TAG, bluetoothConnection);
			bluetoothConnection = null;
		}
	}
	
	private static Connection bluetoothConnection;
	
	private static void connecthfp(){
		
		List<BluetoothDevice> list = OkBluetooth.HFP.getConnectedBluetoothDeviceList();
		
		if(list!=null && list.size() == 0){
			showToast("not found connected bt devices");
			return ;
		}
		
		final BluetoothDevice bluetoothDevice = list.get(0);
		new Thread(new Runnable() {
			
			@Override
			public void run() {
					try {
						new BluetoothConnection.Builder()
								.setConnectionUUID(HeadsetProfileService.UUIDS[0].getUuid())
								.setConnectedDevice(bluetoothDevice)
								.addConnectionPolicy(new HFPConnection(85))
								.setConnectionListener(new BluetoothConnectionListener() {

									@Override
									public void onConnected(Connection connection) {
										Log.i(TAG, "HFP onConnected");
										showToast("HFP Connected");
										bluetoothConnection = connection;
										
										BluetoothUtils.dumpBluetoothConnection(TAG,connection);
										
									}

									@Override
									public void onDisconnected(Connection connection) {
										Log.i(TAG, "HFP onDisconnected");
										showToast("HFP Disconnected");
									}
									
								})
								.build()
								.connect();
					} catch (BluetoothConnectionException e) {
						e.printStackTrace();
					} catch (BluetoothConnectionTimeoutException e) {
						e.printStackTrace();
					} catch (BluetoothException e) {
						e.printStackTrace();
					}
			}
		}).start();
	}
	
}
