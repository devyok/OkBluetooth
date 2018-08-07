package com.devyok.bluetooth;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.devyok.bluetooth.OkBluetooth.Interceptor;
import com.devyok.bluetooth.base.BluetoothAdapterStateListener;
import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.base.BluetoothProfileService;
import com.devyok.bluetooth.base.BluetoothProfileService.BluetoothProfileConnectionStateChangedListener;
import com.devyok.bluetooth.base.BluetoothProfileService.BluetoothProfileServiceStateListener;
import com.devyok.bluetooth.base.BluetoothProfileService.ProfileConnectionState;
import com.devyok.bluetooth.base.StateInformation;
import com.devyok.bluetooth.base.TaskQueue;
import com.devyok.bluetooth.connection.BluetoothConnection;
import com.devyok.bluetooth.connection.BluetoothConnection.BluetoothConnectionListener;
import com.devyok.bluetooth.connection.BluetoothConnection.Protocol;
import com.devyok.bluetooth.connection.BluetoothConnectionException;
import com.devyok.bluetooth.connection.BluetoothConnectionTimeoutException;
import com.devyok.bluetooth.connection.Connection;
import com.devyok.bluetooth.connection.DefaultRetryPolicy;
import com.devyok.bluetooth.hfp.BluetoothHeadsetProfileService;
import com.devyok.bluetooth.hfp.BluetoothHeadsetProfileService.BluetoothHeadsetAudioStateListener;
import com.devyok.bluetooth.hfp.HFPConnection;
import com.devyok.bluetooth.hfp.HeadsetProfileService;
import com.devyok.bluetooth.spp.SPPConnectionSecurePolicy;
import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * 负责维护蓝牙设备间相关连接(SCO/HFP/SPP) <br>
 * 执行者：{@link ConnectionProcessor}   <br>
 * 提交者：{@link ConnectionHelper#buildBluetoothConnection()} <br>
 * @author wei.deng
 */
class ConnectionHelper implements BluetoothProfileConnectionStateChangedListener , 
												  BluetoothProfileServiceStateListener , 
												  BluetoothHeadsetAudioStateListener {

	private static final ConnectionHelper sDefault = new ConnectionHelper();
	
	private static final String TAG = ConnectionHelper.class.getSimpleName();
	
	static ExecutorService recoveryExecutor = Executors.newCachedThreadPool(BluetoothUtils.createThreadFactory("bt.runtime.helper-recovery"));
	
	
	/**
	 * 所有需要尝试建立SCO|HFP|SPP连接的任务都需要提交到此队列中
	 */
	final TaskQueue connectionTaskQueue;
	/**
	 * 负责观察SCO|HFP|SPP连接的正确性
	 */
	final TaskQueue watchDogQueue;
	
	final TaskQueue connectionBuilderQueue;
	
	/**
	 * 与蓝牙设备间的 HFP 连接
	 */
	Connection hfpConnection;
	/**
	 * 与蓝牙设备间的 SPP 连接
	 * 注意：部分设备在建立连接时，如果之前已建立，则需要先断开 {@link Connection#disconnect()}
	 */
	Connection sppConnection;
	
	static BluetoothDevice sLastConnectBluetoothDevice = null;
	
	public static ConnectionHelper getHelper(){
		return sDefault;
	}
	/**
	 * 所有需要尝试建立SCO|HFP|SPP连接的任务都需要延迟2S进入队列
	 * 与系统SCO|HFP超时时间相同
	 */
	static final long TRY_BUILD_CONNECTION_DELAY_TIME = 2*1000;
	/**
	 * 与系统超时时间相同,sco超时时间(sco一直停留在connecting状态)
	 */
	static final long SCO_AUDIO_CONNECT_TIMEOUT = 3*1000;
	
	/**
	 * 建立HFP连接超时时间
	 */
	static final long HFP_CONNECTION_TIMEOUT = 2*1000;
	/**
	 * 建立SPP连接超时时间
	 */
	static final long SPP_CONNECTION_TIMEOUT = 3*1000;
	/**
	 * 建立SCO连接异常,连接一直处于{@link BluetoothProfile#STATE_CONNECTING}
	 */
	private Runnable scoConnectionExceptionTask = BluetoothUtils.EMPTY_TASK;
	
	static AudioDevice gUserTouchConnectAudioDevice = AudioDevice.SBP;
	
	final BluetoothAdapterStateListenerImpl bluetoothAdapterStateListenerImpl = new BluetoothAdapterStateListenerImpl();
	
	private ConnectionHelper(){
		connectionTaskQueue = new TaskQueue("bt.runtime.helper-taskqueue");
		watchDogQueue = new TaskQueue("bt.runtime.helper-watch-taskqueue"); 
		connectionBuilderQueue = new TaskQueue("bt.runtime.helper-connbuilder-taskqueue");
	}

	/**
	 * 当应用第一次启动并连接蓝牙服务成功后会触发
	 * 当系统蓝牙从关闭->打开后触发
	 */
	@Override
	public void onServiceReady(int profile, BluetoothProfileService service) {
		
		Log.i(TAG, "onServiceReady enter , profile("+BluetoothUtils.getProfileString(profile)+")");
		
		if(profile == HeadsetProfileService.PROFILE) {
			tryConnectBluetooth();
		}
		
	}
	
	public BluetoothAdapterStateListener getBluetoothAdapterStateListener(){
		return bluetoothAdapterStateListenerImpl;
	}

	void tryConnectBluetooth(){
		
		LinkedHashMap<BluetoothDevice, ProfileConnectionState> map = OkBluetooth.getAllProfileConnectionState();
		
		Log.i(TAG, "tryConnect device mapping size = " + map.size());
		
		for(Iterator<Map.Entry<BluetoothDevice, ProfileConnectionState>> iter = map.entrySet().iterator();iter.hasNext();){
			
			Map.Entry<BluetoothDevice, ProfileConnectionState> item = iter.next();
			
			final BluetoothDevice connectedBluetoothDevice = item.getKey();
			ProfileConnectionState state = item.getValue();
			
			Log.i(TAG, "tryConnect device device = " + connectedBluetoothDevice.getName() + " , state = " + state);
			
			boolean isSupportHFP = OkBluetooth.isSupportHFP(connectedBluetoothDevice);
			
			if(isSupportHFP && (state == ProfileConnectionState.CONNECTED || state == ProfileConnectionState.CONNECTED_NO_MEDIA)){
				onConnected(HeadsetProfileService.PROFILE, state.ordinal(), state.ordinal(), connectedBluetoothDevice);
				break;
			}
		}
	}

	/**
	 * 系统HFP已建立连接
	 */
	@Override
	public void onConnected(int profile, int newState, int preState,BluetoothDevice bluetoothDevice) {
		Protocol protocol = Protocol.getProtocol(profile);
		buildConnection(HeadsetProfileService.PROFILE == profile ? Event.HFP_CONNECTED : Event.A2DP_CONNECTED, protocol,bluetoothDevice);
		connectedNotifier(protocol,bluetoothDevice);
	}

	/**
	 * 系统HFP连接断开
	 */
	@Override
	public void onDisconnected(int profile, int newState, int preState,
			BluetoothDevice bluetoothDevice) {
		Protocol protocol = Protocol.getProtocol(profile);
		buildConnection(HeadsetProfileService.PROFILE == profile ? Event.HFP_DISCONNECTED : Event.A2DP_DISCONNECTED, protocol, bluetoothDevice);
		disconnectedNotifier(protocol, bluetoothDevice);
	}
	
	private void buildConnection(Event event,Protocol protocol){
		buildConnection(event, protocol,null,TRY_BUILD_CONNECTION_DELAY_TIME,AudioDevice.SBP);
	}
	
	private void buildConnection(Event event,Protocol protocol, BluetoothDevice bluetoothDevice){
		buildConnection(event, protocol,bluetoothDevice,TRY_BUILD_CONNECTION_DELAY_TIME,AudioDevice.SBP);
	}
	/**
	 * 尝试构建SCO|HFP|SPP等连接,但AudioDeivce为UNKNOW,根据当前终端设备所连接的音频设备的优先级来决定
	 * 
	 * @param event
	 * @param protocol
	 * @param bluetoothDevice
	 * @param delay 延迟多长时间开始连接
	 * @param audioDevice
	 */
	private void buildConnection(Event event,Protocol protocol,BluetoothDevice bluetoothDevice,long delay,AudioDevice audioDevice){
		connectionBuilderQueue.submitTask(new ConnectionProcessSubmitter(event, bluetoothDevice, audioDevice, protocol, delay));
	}

	public void connectAudio(AudioDevice audioDevice){
		connectionTaskQueue.removeAllTasks();
		
		if(audioDevice == AudioDevice.SBP) {
			buildConnection(Event.USER_INTERFACE_QUIT,Protocol.HFP,null,0,audioDevice);
		} else {
			buildConnection(Event.USER_INTERFACE,Protocol.HFP,null,0,audioDevice);
		}
		
		
	}
	
	@Override
	public void onAudioConnected(BluetoothDevice bluetoothDevice,
			BluetoothHeadsetProfileService service) {
		Log.i(TAG, "hfp audio onConnected("+bluetoothDevice.getName()+")");
		connectionTaskQueue.removeTasks(scoConnectionExceptionTask);
	}

	@Override
	public void onAudioDisconnected(BluetoothDevice bluetoothDevice,
			BluetoothHeadsetProfileService service) {
		Log.i(TAG, "hfp audio onDisconnected("+bluetoothDevice.getName()+")");
		connectionTaskQueue.removeTasks(scoConnectionExceptionTask);
		
		if(OkBluetooth.isPhoneCalling()){
			buildConnection(Event.PHONECALL_SCO_DISCONNECTED, Protocol.HFP);
		} else {
			buildConnection(Event.SCO_DISCONNECTED, Protocol.HFP);
		}
	}
	
	@Override
	public void onAudioConnecting(BluetoothDevice bluetoothDevice,
			BluetoothHeadsetProfileService service) {
		
		Log.i(TAG, "hfp audio onConnecting("+bluetoothDevice.getName()+")");
		connectionTaskQueue.removeTasks(scoConnectionExceptionTask);
		connectionTaskQueue.submitTask(scoConnectionExceptionTask = new ScoConnectionExceptionTask(bluetoothDevice), SCO_AUDIO_CONNECT_TIMEOUT);
		
	}
	
	public void tryRecoveryAudioConnection(){
		Log.i(TAG, "tryRecoveryAudioConnection enter");
		tryRecoveryAudioConnection(Event.WATCH_DOG);
	}
	
	public void tryRecoveryAudioConnection(Event event){
		watchDogQueue.removeAllTasks();
		watchDogQueue.submitTask(new WatchDogTask(event), TRY_BUILD_CONNECTION_DELAY_TIME);
	}
	
	public void submitAudioConnectTask(){
		submitTryRecoveryConnectionTask(Event.HEADSET_OR_OTHERDEVICE_RECOVERY);
	}
	
	private void submitTryRecoveryConnectionTask(Event event){
		BluetoothDevice device = OkBluetooth.HFP.getFristConnectedDevice();
		buildConnection(event, Protocol.HFP, device);
	}
	
	private void connectedNotifier(Protocol protocol,BluetoothDevice device){
		OkBluetooth.getBluetoothProtocolConnectionStateListener().onConnected(protocol,device);
	}
	
	private void disconnectedNotifier(Protocol protocol,BluetoothDevice device){
		OkBluetooth.getBluetoothProtocolConnectionStateListener().onDisconnected(protocol,device);
	}
	
	/**
	 * 负责建立SCO/HFP/SPP,同时尝试恢复与当前设备环境相符的音频状态<br>
	 * 
	 * <p>为什么不以构造方法传递的蓝牙设备对象为准来构建HFP与SPP连接？</p>
	 * 由于ConnectionProcessor接收来自A2dp入口，所以<br>
	 * 当A设备的HFP连接建立成功之后延迟提交任务到队列，而此时B设备的A2dp进入，会将HFP连接任务从队列中移除<br>
	 * 如果以构造方法的设备为准，那么将会根据B设备来建立HFP与SPP，如果B不支持SPP或HFP,则将无法建立。
	 * 但是此时支持HFP与SPP的A设备已经连接.<br>
	 * 
	 * <p>以上情况是多蓝牙设备间多次切换导致</p>
	 * 
	 * 所以任何一个协议连接建立完成之后，都实时去获取当前终端连接情况，保证HFP与SPP连接的正确创建。<br>
	 * 
	 * @author wei.deng
	 */
	class ConnectionProcessor implements Runnable {
		
		private AudioDevice audioDevice = AudioDevice.SBP;
		
		public ConnectionProcessor(BluetoothDevice bluetoothDevice,AudioDevice audioDevice){
			this.audioDevice = (audioDevice!=null ? audioDevice : AudioDevice.SBP);
		}
		
		public void run(){
			try {
				
				Interceptor interceptor = OkBluetooth.getInterceptor();
				
				if(interceptor.beforeRunConnectTask()){
					Log.i(TAG, "ConnectTask intercepted");
					return ;
				}
				
				Log.i(TAG, "start connect " + audioDevice);
				
				if(AudioDevice.SBP == audioDevice){
					
					//根据终端当前已连接的音频设备优先级来决定走哪个音频设备
					tryConnect(interceptor);
					
				} else if(AudioDevice.SPEAKER == audioDevice){
					
					tryConnectSpeaker();
					
				} else if(AudioDevice.SCO == audioDevice){
					
					if(OkBluetooth.isBluetoothEnable() && OkBluetooth.HFP.hasConnectedDevice()){
						tryConnectBluetooth(interceptor);
					}
					
				} else if(AudioDevice.WIREDHEADSET == audioDevice){
					
					tryConnectWiredHeadset();

				} else if(AudioDevice.A2DP == audioDevice){
					
					tryConnectA2dp();
				}
				
			} finally {
				
			}
		}
		
		public AudioDevice getAudioDevice(){
			return audioDevice;
		}
		
		private void tryConnect(Interceptor interceptor){
			if(OkBluetooth.isPhoneCalling()){ //系统电话是否连接中
				
				if(interceptor.systemPhoneCalling()){
					Log.i(TAG, "SystemEvent intercepted");
					return ;
				}
				
				disconnectAllProfiles();
				
				if(OkBluetooth.hasForcePhoneRing() && OkBluetooth.hasForcePhoneIncall() && OkBluetooth.isBluetoothEnable() && OkBluetooth.HFP.hasConnectedDevice()) {
					tryConnectSco();
				}
			} else if(OkBluetooth.isBluetoothEnable() && OkBluetooth.HFP.hasConnectedDevice()){ //是否存在可用的蓝牙设备
				
				tryConnectBluetooth(interceptor);
				
			} else if(OkBluetooth.isWiredHeadsetOn()){ //有线耳机是否插入
				
				tryConnectWiredHeadset();
				
			}  else {  //正常设备环境
				
				tryConnectSpeaker();
			}
		}
		
		private void tryConnectBluetooth(Interceptor interceptor){
			
			BluetoothDevice bluetoothDevice = OkBluetooth.HFP.getFristConnectedDevice();
			
			if(interceptor.beforeConnectBluetoothProfile()){
				Log.i(TAG, "ConnectBluetoothProfile event intercepted");
				sLastConnectBluetoothDevice = bluetoothDevice;
				return ;
			} else {
				if(bluetoothDevice!=null){
					
					Log.i(TAG, "current device("+bluetoothDevice.getName()+") , last connect deivce("+(sLastConnectBluetoothDevice!=null?sLastConnectBluetoothDevice.getName():"none")+")");
					
					if(sLastConnectBluetoothDevice!=null && !(sLastConnectBluetoothDevice.equals(bluetoothDevice))) {
						disconnectAllProfiles();
					}
					
					tryConnectHfp(bluetoothDevice);
					
					//建立SPP连接可能需要花费一些时间,失败后会进行重连或重启系统HFP连接后建立SPP连接
					tryConnectSpp(bluetoothDevice);
					
					sLastConnectBluetoothDevice = bluetoothDevice;
				}
			}
			tryConnectSco();
		}
		
		
		private void tryConnectSco() {
			dumpAudioEnv(AudioDevice.SCO);
			OkBluetooth.connectAudioInternal(AudioDevice.SCO);
		}

		private void tryConnectWiredHeadset() {
			dumpAudioEnv(AudioDevice.WIREDHEADSET);
			OkBluetooth.connectAudioInternal(AudioDevice.WIREDHEADSET);
			disconnectAllProfiles();
		}
		
		private void tryConnectSpeaker() {
			dumpAudioEnv(AudioDevice.SPEAKER);
			OkBluetooth.connectAudioInternal(AudioDevice.SPEAKER);
			disconnectAllProfiles();
			BluetoothUtils.dumpProfileConnectionMap(TAG,OkBluetooth.getAllProfileConnectionState());
		}
		
		private void tryConnectA2dp() {
			dumpAudioEnv(AudioDevice.A2DP);
			OkBluetooth.connectAudioInternal(AudioDevice.A2DP);
		}
		
		private void dumpAudioEnv(AudioDevice audioDevice){
			boolean isAudioConnected = OkBluetooth.HFP.isAudioConnected();
			boolean isBluetoothScoOn = OkBluetooth.isBluetoothScoOn();
			boolean isBluetoothA2dpOn = OkBluetooth.isBluetoothA2dpOn();
			boolean isSpeakerphoneOn = OkBluetooth.isSpeakerphoneOn();
			Log.i(TAG, "tryBuildAudioConnection("+audioDevice.getName()+") isAudioConnected = " + isAudioConnected + " , isBluetoothScoOn = " + isBluetoothScoOn + " , isBluetoothA2dpOn = " + isBluetoothA2dpOn + " , isSpeakerphoneOn = " + isSpeakerphoneOn);
		}
		
		private boolean isHFPConnected(){
			return (hfpConnection!=null && hfpConnection.isConnected());
		}
		
		private boolean isSPPConnected(){
			return (sppConnection!=null && sppConnection.isConnected());
		}
		
		private boolean tryConnectHfp(BluetoothDevice bluetoothDevice) {
			String deviceName = bluetoothDevice != null ? bluetoothDevice.getName() : "none";
			
			boolean isHFPConnected = isHFPConnected();
			Log.i(TAG, "start build hfp connection("+isHFPConnected+") , deviceName = " + deviceName);
			if(!isHFPConnected) {
				connectHFP(bluetoothDevice);
				return true;
			}
			return false;
		}

		private boolean tryConnectSpp(BluetoothDevice bluetoothDevice) {
			
			String deviceName = bluetoothDevice != null ? bluetoothDevice.getName() : "none";
			
			boolean isSupportSPP = OkBluetooth.isSupportSPP(bluetoothDevice);
			boolean isSPPConnected = isSPPConnected();
			Log.i(TAG, "start build spp("+isSupportSPP+") connection("+isSPPConnected+"), deviceName = " + deviceName);
			if(isSupportSPP && !isSPPConnected){
				Log.i(TAG, "start build spp connection");
				connectSPP(bluetoothDevice,true);
				return true;
			}
			return false;
		}
		
		private void connectHFP(final BluetoothDevice bluetoothDevice){
			try{	
				Log.i(TAG, "connectHFP enter");
				new BluetoothConnection.Builder()
								.setConnectionUUID(HeadsetProfileService.UUIDS[0].getUuid())
								.setConnectedDevice(bluetoothDevice)
								.setConnectionTimeout(HFP_CONNECTION_TIMEOUT)
								.setCompanyid(OkBluetooth.getConfiguration().getCompanyId())
								.addConnectionPolicy(new HFPConnection())
								.setConnectionListener(new BluetoothConnectionListener() {
						
									@Override
									public void onConnected(Connection connection) {
										Log.i(TAG, "HFP connected, start receive message");
										hfpConnection = connection;
										BluetoothUtils.dumpBluetoothConnection(TAG,connection);
									}
						
									@Override
									public void onDisconnected(Connection connection) {
										Log.i(TAG, "HFP onDisconnected");
									}
								})
								.build()
								.connect();
			} catch (BluetoothConnectionException e) {
				e.printStackTrace();
				Log.i(TAG, "connectHFP exception("+(e!=null ? e.getMessage() : "none")+")");
			} catch (BluetoothConnectionTimeoutException e) {
				e.printStackTrace();
				Log.i(TAG, "connectHFP exception("+(e!=null ? e.getMessage() : "none")+")");
			} catch (BluetoothException e) {
				e.printStackTrace();
				Log.i(TAG, "connectHFP exception("+(e!=null ? e.getMessage() : "none")+")");
			}
			
		}
		
		private boolean connectSPP(final BluetoothDevice bluetoothDevice,final boolean isSupportRebuildHfpConnection) {
			try {
				
				Log.i(TAG, "connectSPP enter");
				
				new BluetoothConnection.Builder()
						.setConnectionUUID(BluetoothConnection.DEFAULT_UUID)
						.setConnectedDevice(bluetoothDevice)
						.setConnectionTimeout(SPP_CONNECTION_TIMEOUT)
						.setConnectionRetryPolicy(new DefaultRetryPolicy(1))
						.addConnectionPolicy(new SPPConnectionSecurePolicy())
						.setConnectionListener(new BluetoothConnectionListener() {

							@Override
							public void onConnected(Connection connection) {
								Log.i(TAG, "SPP connected, start receive message");
								sppConnection = connection;
								connectedNotifier(Protocol.SPP,bluetoothDevice);
							}

							@Override
							public void onDisconnected(Connection connection) {
								Log.i(TAG, "SPP Disconnected");
								disconnectedNotifier(Protocol.SPP,bluetoothDevice);
							}
						})
						.build()
						.connect();
				
				return true;
			} catch (BluetoothConnectionException e) {
				e.printStackTrace();
				Log.i(TAG, "connectSPP exception("+(e!=null ? e.getMessage() : "none")+")");
				if(isSupportRebuildHfpConnection){
					rebuildHfpConnection(bluetoothDevice);
				}
			} catch (BluetoothConnectionTimeoutException e) {
				e.printStackTrace();
				Log.i(TAG, "connectSPP exception("+(e!=null ? e.getMessage() : "none")+")");
				if(isSupportRebuildHfpConnection){
					rebuildHfpConnection(bluetoothDevice);
				}
			} catch (BluetoothException e) {
				e.printStackTrace();
				Log.i(TAG, "connectSPP exception("+(e!=null ? e.getMessage() : "none")+")");
			}
			
			return false;
		}
		/**
		 * 尝试断开系统HFP连接进行恢复<br>
		 * 什么情况下出现这种情况?<br>
		 * 当SPP连接维护的socket出现错误，在建立一次新的连接时没有正确断开上次连接，可能会导致当前连接建立失败,是一种补救方法。<br>
		 * 同时尝试恢复SPP需要同步在当前任务队列完成，使后续任务得到正确的状态<br>
		 * @param device
		 */
		private void rebuildHfpConnection(final BluetoothDevice device){
			
			if(!OkBluetooth.isBluetoothEnable()) return ;
			
			boolean isConnected = OkBluetooth.HFP.isConnected(device);
			
			Log.i(TAG, "rebuildHfpConnection device isConnected = " + isConnected + " , deivce name = " + device.getName());
			
			if(isConnected){
				
				boolean disconnectResult = OkBluetooth.HFP.disconnect(device);
				int priority = OkBluetooth.HFP.getPriority(device);
				Log.i(TAG, "rebuildHfpConnection disconnect device result = " + disconnectResult + " , priority = " + BluetoothUtils.getDevicePriority(priority));
				
				int count = 0;
				int maxConnectCount = 3;
				
				while(true){
					try {
						boolean connectResult = OkBluetooth.HFP.connect(device);
						Log.i(TAG, "rebuildHfpConnection while item connectResult = " + connectResult + " , priority = " + BluetoothUtils.getDevicePriority(priority));
						try {
							Thread.sleep((1*1000 + (count * 200)));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						isConnected = OkBluetooth.HFP.isConnected(device);
						if(OkBluetooth.isBluetoothEnable() && (isConnected || (count >= maxConnectCount))){
							Log.i(TAG, "rebuildHfpConnection break loop("+count+")");
							break;
						}
						
						++count;
					} catch(Throwable e){
						break;
					}
					
				}
				
				BluetoothDevice bluetoothDevice = OkBluetooth.HFP.getFristConnectedDevice();
				if(bluetoothDevice!=null && OkBluetooth.isSupportSPP(bluetoothDevice)) {
					if(!connectSPP(device,false)){
						
					}
				}
				
			} else {
				
				BluetoothDevice bluetoothDevice = OkBluetooth.HFP.getFristConnectedDevice();
				if(bluetoothDevice!=null && OkBluetooth.isSupportSPP(bluetoothDevice)) {
					connectSPP(bluetoothDevice,true);
				}
				
			}
			
		}
		
		private void disconnectAllProfiles(){
			Log.i(TAG, "disconnectAllProfiles enter");
			disconnectHFP();
			disconnectSPP();
		}
		
		private void disconnectHFP(){
			if(hfpConnection!=null){
				Log.i(TAG, "disconnect hfp connection, stop receive message");
				hfpConnection.disconnect();
				hfpConnection = null;
			}
		}
		
		private void disconnectSPP(){
			if(sppConnection!=null){
				Log.i(TAG, "disconnect spp connection, stop receive message");
				sppConnection.disconnect();
				sppConnection = null;
			}
		}
		
	}
	
	class ConnectionProcessSubmitter implements Runnable {

		private Event event = Event.UNKNOW;
		private BluetoothDevice bluetoothDevice;
		private AudioDevice audioDevice = AudioDevice.SBP;
		private Protocol protocol = Protocol.HFP;
		private long delay = HFP_CONNECTION_TIMEOUT;
		
		public ConnectionProcessSubmitter(Event event, BluetoothDevice bluetoothDevice,
				AudioDevice audioDevice, Protocol protocol, long delay) {
			this.event = (event == null ? Event.UNKNOW : event);
			this.bluetoothDevice = bluetoothDevice;
			this.audioDevice = (audioDevice == null ? AudioDevice.SBP : audioDevice);
			this.protocol = (protocol == null ? Protocol.HFP : protocol);
			this.delay = delay;
		}

		@Override
		public void run() {
			
			BluetoothUtils.dumpBluetoothDevice(TAG, bluetoothDevice);
			Log.i(TAG, "buildBluetoothConnection event = "+ event +", delay time = " + delay + " , param userTouch " + audioDevice + " , pre userTouch " + gUserTouchConnectAudioDevice + " , protocol = " + protocol);
			
			//为什么还需要处理A2dp的profile连接？
			//如果当前设备处于正常模式下即：speaker开启，而当A2dp连接成功后,A2dp开启,那么会走蓝牙设备,所以当A2dp连接成功之后,也需要检查音频连接并关闭A2dp
			
			if(BluetoothConnection.Protocol.HFP == protocol || BluetoothConnection.Protocol.A2DP == protocol) {
				
				switch (event) {
				case A2DP_CONNECTED:
				case A2DP_DISCONNECTED:
				case HFP_CONNECTED:
				case HFP_DISCONNECTED:
				case HEADSET_OR_OTHERDEVICE_RECOVERY:
				case PHONECALL_INCALL_TO_IDLE:
				case SCO_DISCONNECTED:
				case PHONECALL_SCO_DISCONNECTED:
				case WATCH_DOG:
					audioDevice = gUserTouchConnectAudioDevice;
					break;
				case USER_INTERFACE:
					gUserTouchConnectAudioDevice = audioDevice;
					break;
				case UNKNOW:
				case USER_INTERFACE_QUIT:
					gUserTouchConnectAudioDevice = AudioDevice.SBP;
					break;
				}
				
				Log.i(TAG, "submitTask(ConnectionProcessor), event " + event + " , submit audioDevice = " + audioDevice);
				connectionTaskQueue.removeAllTasks();
				connectionTaskQueue.submitTask(new ConnectionProcessor(bluetoothDevice,audioDevice),delay);
				
			}
			
		}
		
	}
	
	
	/**
	 * 以下定义为触发构建音频连接({@link ConnectionHelper#buildBluetoothConnection()})的事件
	 * @author wei.deng
	 */
	public enum Event {
		
		UNKNOW,
		
		/**
		 * 当HFP连接成功之后，通过此事件的定义开始触发构建音频
		 */
		HFP_CONNECTED,
		HFP_DISCONNECTED,
		A2DP_CONNECTED,
		A2DP_DISCONNECTED,
		/**
		 * 当系统电话状态从INCALL -> IDLE时
		 */
		PHONECALL_INCALL_TO_IDLE,
		/**
		 * 检测音频状态
		 */
		WATCH_DOG,
		/**
		 * 用户通过UI主动触发切换音频设备
		 */
		USER_INTERFACE,
		USER_INTERFACE_QUIT,
		HEADSET_OR_OTHERDEVICE_RECOVERY,
		/**
		 * sco 断开
		 */
		SCO_DISCONNECTED,
		PHONECALL_SCO_DISCONNECTED
	}

	abstract class AbstractExceptionTask implements Runnable{
		
		protected BluetoothDevice mDevice;
		protected volatile boolean isStop = false;
		protected Thread recoveryThread;
		static final  int DEFAULT_TASK_TIMEOUT = 10; //seconds
		
		public AbstractExceptionTask(BluetoothDevice bluetoothDevice) {
			this.mDevice = bluetoothDevice;
		}
		
		public long timeout(){
			return 0L;
		}
		
		public abstract Runnable task();
		
		@Override
		public void run() {
			
			long timeout = timeout();
			Runnable taskImpl = task();
			if(timeout == 0){
				taskImpl.run();
				return ;
			}
			
			try {
				Future<?> future = recoveryExecutor.submit(taskImpl);
				future.get(timeout, TimeUnit.SECONDS);
				mDevice = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
				Log.i(TAG, "start recovery Timeout");
				isStop = true;
				if(recoveryThread!=null){
					recoveryThread.interrupt();
					recoveryThread = null;
				}
			}
			
		}
		
	}
	
	/**
	 * sco 一直处于{@link BluetoothHeadset#STATE_AUDIO_CONNECTING}状态下，则认为发生异常
	 * @author wei.deng
	 */
	class ScoConnectionExceptionTask extends AbstractExceptionTask {
		
		public ScoConnectionExceptionTask(BluetoothDevice bluetoothDevice) {
			super(bluetoothDevice);
		}

		@Override
		public long timeout() {
			return 10;
		}

		@Override
		public Runnable task() {
			return new Runnable() {
				
				@Override
				public void run() {
					
					if(mDevice!=null){
						
						int audioState = OkBluetooth.HFP.getAudioState(mDevice);
						ProfileConnectionState connectionState = OkBluetooth.HFP.getConnectionState(mDevice);
						int priority = OkBluetooth.getPriority(HeadsetProfileService.PROFILE,mDevice);
						
						Log.i(TAG, "AudioExceptionTask run , bt isenable = "+ OkBluetooth.isBluetoothEnable() + ", audioState = " + BluetoothUtils.getScoStateStringFromHeadsetProfile(audioState) + " , hfp connection state = " + connectionState + " ,priority = " + BluetoothUtils.getDevicePriority(priority));
						
						if(OkBluetooth.isBluetoothEnable() && audioState == BluetoothHeadset.STATE_AUDIO_CONNECTING && ProfileConnectionState.isConnected(connectionState)) {
							
							Log.i(TAG, "start recovery#2");
							
							recoveryThread = Thread.currentThread();
							
							connectionTaskQueue.removeAllTasks();
							
							//在当前任务线程(ConnectionThread)阻塞运行(推迟所有任务)，保证当前线程所在线程队列的其他任务可以正确获取蓝牙状态
							
							while(!isStop && OkBluetooth.isBluetoothEnable()){
								Log.i(TAG, "start recovery#2 disableBluetooth");
								OkBluetooth.disableBluetooth();
								
								try {
									Thread.sleep(1500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
							}
							
							while(!isStop && !OkBluetooth.isBluetoothEnable()){
								Log.i(TAG, "start recovery#2 enableBluetooth");
								OkBluetooth.enableBluetooth();
								
								try {
									Thread.sleep(1500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							
							
							if(priority == BluetoothHeadset.PRIORITY_AUTO_CONNECT) {
								
								ProfileConnectionState state = OkBluetooth.HFP.getConnectionState(mDevice);
								
								Log.i(TAG, "start recovery#2 check device("+mDevice.getName()+") new state("+state+")");
								
								while(!isStop && !ProfileConnectionState.isConnected(state)) {
									state = OkBluetooth.HFP.getConnectionState(mDevice);
									
									Log.i(TAG, "start recovery#2 check device("+mDevice.getName()+") new state = " + state);
									
									try {
										Thread.sleep(500);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
								
								Log.i(TAG, "start recovery check device("+mDevice.getName()+") final state = " + state);
							}
							
							
							if(isStop && !OkBluetooth.isBluetoothEnable()) {
								OkBluetooth.enableBluetooth();
							}
							
							Log.i(TAG, "start recovery#2 completed");
							
						}
						
					}
					
				}
			};
		}
		
	}
	

	class WatchDogTask implements Runnable {
		
		private Event event;
		
		public WatchDogTask(Event event){
			this.event = event;
		}
		
		@Override
		public void run() {
			submitTryRecoveryConnectionTask(event);
		}
	}

	class BluetoothAdapterStateListenerImpl extends BluetoothAdapterStateListener {

		@Override
		public void onClosed(StateInformation stateInformation) {
			super.onClosed(stateInformation);
			sLastConnectBluetoothDevice = null;
		}
		
	}

}
