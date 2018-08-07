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
 * ����ά�������豸���������(SCO/HFP/SPP) <br>
 * ִ���ߣ�{@link ConnectionProcessor}   <br>
 * �ύ�ߣ�{@link ConnectionHelper#buildBluetoothConnection()} <br>
 * @author wei.deng
 */
class ConnectionHelper implements BluetoothProfileConnectionStateChangedListener , 
												  BluetoothProfileServiceStateListener , 
												  BluetoothHeadsetAudioStateListener {

	private static final ConnectionHelper sDefault = new ConnectionHelper();
	
	private static final String TAG = ConnectionHelper.class.getSimpleName();
	
	static ExecutorService recoveryExecutor = Executors.newCachedThreadPool(BluetoothUtils.createThreadFactory("bt.runtime.helper-recovery"));
	
	
	/**
	 * ������Ҫ���Խ���SCO|HFP|SPP���ӵ�������Ҫ�ύ���˶�����
	 */
	final TaskQueue connectionTaskQueue;
	/**
	 * ����۲�SCO|HFP|SPP���ӵ���ȷ��
	 */
	final TaskQueue watchDogQueue;
	
	final TaskQueue connectionBuilderQueue;
	
	/**
	 * �������豸��� HFP ����
	 */
	Connection hfpConnection;
	/**
	 * �������豸��� SPP ����
	 * ע�⣺�����豸�ڽ�������ʱ�����֮ǰ�ѽ���������Ҫ�ȶϿ� {@link Connection#disconnect()}
	 */
	Connection sppConnection;
	
	static BluetoothDevice sLastConnectBluetoothDevice = null;
	
	public static ConnectionHelper getHelper(){
		return sDefault;
	}
	/**
	 * ������Ҫ���Խ���SCO|HFP|SPP���ӵ�������Ҫ�ӳ�2S�������
	 * ��ϵͳSCO|HFP��ʱʱ����ͬ
	 */
	static final long TRY_BUILD_CONNECTION_DELAY_TIME = 2*1000;
	/**
	 * ��ϵͳ��ʱʱ����ͬ,sco��ʱʱ��(scoһֱͣ����connecting״̬)
	 */
	static final long SCO_AUDIO_CONNECT_TIMEOUT = 3*1000;
	
	/**
	 * ����HFP���ӳ�ʱʱ��
	 */
	static final long HFP_CONNECTION_TIMEOUT = 2*1000;
	/**
	 * ����SPP���ӳ�ʱʱ��
	 */
	static final long SPP_CONNECTION_TIMEOUT = 3*1000;
	/**
	 * ����SCO�����쳣,����һֱ����{@link BluetoothProfile#STATE_CONNECTING}
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
	 * ��Ӧ�õ�һ��������������������ɹ���ᴥ��
	 * ��ϵͳ�����ӹر�->�򿪺󴥷�
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
	 * ϵͳHFP�ѽ�������
	 */
	@Override
	public void onConnected(int profile, int newState, int preState,BluetoothDevice bluetoothDevice) {
		Protocol protocol = Protocol.getProtocol(profile);
		buildConnection(HeadsetProfileService.PROFILE == profile ? Event.HFP_CONNECTED : Event.A2DP_CONNECTED, protocol,bluetoothDevice);
		connectedNotifier(protocol,bluetoothDevice);
	}

	/**
	 * ϵͳHFP���ӶϿ�
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
	 * ���Թ���SCO|HFP|SPP������,��AudioDeivceΪUNKNOW,���ݵ�ǰ�ն��豸�����ӵ���Ƶ�豸�����ȼ�������
	 * 
	 * @param event
	 * @param protocol
	 * @param bluetoothDevice
	 * @param delay �ӳٶ೤ʱ�俪ʼ����
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
	 * ������SCO/HFP/SPP,ͬʱ���Իָ��뵱ǰ�豸�����������Ƶ״̬<br>
	 * 
	 * <p>Ϊʲô���Թ��췽�����ݵ������豸����Ϊ׼������HFP��SPP���ӣ�</p>
	 * ����ConnectionProcessor��������A2dp��ڣ�����<br>
	 * ��A�豸��HFP���ӽ����ɹ�֮���ӳ��ύ���񵽶��У�����ʱB�豸��A2dp���룬�ὫHFP��������Ӷ������Ƴ�<br>
	 * ����Թ��췽�����豸Ϊ׼����ô�������B�豸������HFP��SPP�����B��֧��SPP��HFP,���޷�������
	 * ���Ǵ�ʱ֧��HFP��SPP��A�豸�Ѿ�����.<br>
	 * 
	 * <p>��������Ƕ������豸�����л�����</p>
	 * 
	 * �����κ�һ��Э�����ӽ������֮�󣬶�ʵʱȥ��ȡ��ǰ�ն������������֤HFP��SPP���ӵ���ȷ������<br>
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
					
					//�����ն˵�ǰ�����ӵ���Ƶ�豸���ȼ����������ĸ���Ƶ�豸
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
			if(OkBluetooth.isPhoneCalling()){ //ϵͳ�绰�Ƿ�������
				
				if(interceptor.systemPhoneCalling()){
					Log.i(TAG, "SystemEvent intercepted");
					return ;
				}
				
				disconnectAllProfiles();
				
				if(OkBluetooth.hasForcePhoneRing() && OkBluetooth.hasForcePhoneIncall() && OkBluetooth.isBluetoothEnable() && OkBluetooth.HFP.hasConnectedDevice()) {
					tryConnectSco();
				}
			} else if(OkBluetooth.isBluetoothEnable() && OkBluetooth.HFP.hasConnectedDevice()){ //�Ƿ���ڿ��õ������豸
				
				tryConnectBluetooth(interceptor);
				
			} else if(OkBluetooth.isWiredHeadsetOn()){ //���߶����Ƿ����
				
				tryConnectWiredHeadset();
				
			}  else {  //�����豸����
				
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
					
					//����SPP���ӿ�����Ҫ����һЩʱ��,ʧ�ܺ���������������ϵͳHFP���Ӻ���SPP����
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
		 * ���ԶϿ�ϵͳHFP���ӽ��лָ�<br>
		 * ʲô����³����������?<br>
		 * ��SPP����ά����socket���ִ����ڽ���һ���µ�����ʱû����ȷ�Ͽ��ϴ����ӣ����ܻᵼ�µ�ǰ���ӽ���ʧ��,��һ�ֲ��ȷ�����<br>
		 * ͬʱ���Իָ�SPP��Ҫͬ���ڵ�ǰ���������ɣ�ʹ��������õ���ȷ��״̬<br>
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
			
			//Ϊʲô����Ҫ����A2dp��profile���ӣ�
			//�����ǰ�豸��������ģʽ�¼���speaker����������A2dp���ӳɹ���,A2dp����,��ô���������豸,���Ե�A2dp���ӳɹ�֮��,Ҳ��Ҫ�����Ƶ���Ӳ��ر�A2dp
			
			if(Protocol.HFP == protocol || Protocol.A2DP == protocol) {
				
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
	 * ���¶���Ϊ����������Ƶ����({@link ConnectionHelper#buildBluetoothConnection()})���¼�
	 * @author wei.deng
	 */
	public enum Event {
		
		UNKNOW,
		
		/**
		 * ��HFP���ӳɹ�֮��ͨ�����¼��Ķ��忪ʼ����������Ƶ
		 */
		HFP_CONNECTED,
		HFP_DISCONNECTED,
		A2DP_CONNECTED,
		A2DP_DISCONNECTED,
		/**
		 * ��ϵͳ�绰״̬��INCALL -> IDLEʱ
		 */
		PHONECALL_INCALL_TO_IDLE,
		/**
		 * �����Ƶ״̬
		 */
		WATCH_DOG,
		/**
		 * �û�ͨ��UI���������л���Ƶ�豸
		 */
		USER_INTERFACE,
		USER_INTERFACE_QUIT,
		HEADSET_OR_OTHERDEVICE_RECOVERY,
		/**
		 * sco �Ͽ�
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
	 * sco һֱ����{@link BluetoothHeadset#STATE_AUDIO_CONNECTING}״̬�£�����Ϊ�����쳣
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
							
							//�ڵ�ǰ�����߳�(ConnectionThread)��������(�Ƴ���������)����֤��ǰ�߳������̶߳��е��������������ȷ��ȡ����״̬
							
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
