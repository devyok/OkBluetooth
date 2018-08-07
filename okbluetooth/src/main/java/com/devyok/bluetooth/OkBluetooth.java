package com.devyok.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import com.devyok.bluetooth.AudioService.AudioModeModifier;
import com.devyok.bluetooth.ConnectionHelper.Event;
import com.devyok.bluetooth.a2dp.A2dpProfileService;
import com.devyok.bluetooth.a2dp.BluetoothA2dpProfileService;
import com.devyok.bluetooth.base.BluetoothAdapterService;
import com.devyok.bluetooth.base.BluetoothAdapterStateListener;
import com.devyok.bluetooth.base.BluetoothAndroidThread;
import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.base.BluetoothProfileService;
import com.devyok.bluetooth.base.BluetoothProfileService.BluetoothProfileConnectionStateChangedListener;
import com.devyok.bluetooth.base.BluetoothProfileService.BluetoothProfileServiceStateListener;
import com.devyok.bluetooth.base.BluetoothProfileService.ProfileConnectionState;
import com.devyok.bluetooth.base.BluetoothRuntimeException;
import com.devyok.bluetooth.base.BluetoothService;
import com.devyok.bluetooth.base.Executor;
import com.devyok.bluetooth.connection.BluetoothConnection;
import com.devyok.bluetooth.connection.BluetoothConnection.Protocol;
import com.devyok.bluetooth.connection.BluetoothConnectionStateListener;
import com.devyok.bluetooth.connection.BluetoothDeviceConnectionService;
import com.devyok.bluetooth.hfp.BluetoothHeadsetProfileService;
import com.devyok.bluetooth.hfp.BluetoothHeadsetProfileService.BluetoothHeadsetAudioStateListener;
import com.devyok.bluetooth.hfp.HeadsetProfileService;
import com.devyok.bluetooth.message.BluetoothMessageDispatcher;
import com.devyok.bluetooth.message.BluetoothMessageReceiver;
import com.devyok.bluetooth.sco.BluetoothSCOService;
import com.devyok.bluetooth.spp.SPPBluetoothMessageParser;
import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * 提供所有使用蓝牙所支持的服务<p>
 * 1. {@link HeadsetProfileService}<br>
 * 2. {@link A2dpProfileService}<br>
 * 3. {@link BluetoothAdapterService}<br>
 * 4. {@link AudioService}<br>
 * 5. ....
 * @author deng.wei
 */
public final class OkBluetooth extends BluetoothService{

	static final String TAG = OkBluetooth.class.getSimpleName();
	
	/**
	 * 系统电话来电时强制尝试使用蓝牙
	 */
	public static final int FORCE_TYPE_PHONE_RING = 0x001;
	/**
	 * 系统电话通话中强制尝试使用蓝牙
	 */
	public static final int FORCE_TYPE_PHONE_INCALL = 0x002;
	/**
	 * 系统电话通话后挂断强制尝试使用蓝牙
	 */
	public static final int FORCE_TYPE_PHONE_INCALL_TO_IDLE = 0x004;
	
	private BluetoothSCOService mBluetoothScoService;
	private BluetoothDeviceConnectionService mBluetoothConnectService;
	private BluetoothAdapterService mBluetoothAdapterService;
	private BluetoothHeadsetProfileService mHeadsetProfileService;
	private BluetoothA2dpProfileService mA2dpProfileService;
	private Executor mBluetoothExecutor;
	private AudioService mAudioService;
	private TelephonyService mTelephonyService;
	private final HashMap<Integer,BluetoothProfileService> mProfileServiceMapping = new HashMap<Integer,BluetoothProfileService>();
	
	private Context mContext;
	private Configuration mConfiguration = Configuration.DEFAULT;
	private Interceptor mInterceptor = Interceptor.EMPTY;
	
	/**
	 * 监听HFP|SPP连接的状态
	 */
	private BluetoothProtocolConnectionStateListener mProtocolConnectionStateListener = BluetoothProtocolConnectionStateListener.EMTPY;
	private static SPPBluetoothMessageParser<?> sSppBluetoothMessageParser = SPPBluetoothMessageParser.DEFAULT; 
	
	static ExecutorService singleExecutor = new ThreadPoolExecutor(1, 1,
														            60L, TimeUnit.SECONDS,
														            new LinkedBlockingQueue<Runnable>(),
														            BluetoothUtils.createThreadFactory("bt.runtime.ok"));
	static class OkBluetoothInstanceHolder {
		static OkBluetooth sInstance = new OkBluetooth();
	}
	
	static OkBluetooth getInstance(){
		return OkBluetoothInstanceHolder.sInstance;
	}
	
	private OkBluetooth(){
	}
	
	public static void init(Context context){
		init(context,Configuration.DEFAULT);
	}
	
	public static boolean recycle(){
		return getInstance().destory();
	}
	
	public static void init(Context context,Configuration config){
		if(context == null){
			throw new IllegalArgumentException("init error , arg context is null");
		}
		
		if(Looper.myLooper() != Looper.getMainLooper()){
			throw new IllegalArgumentException("init must be in main thread");
		}
		
		getInstance().mContext = context.getApplicationContext();
		getInstance().mConfiguration = config == null ? Configuration.DEFAULT : config;
		
		Log.i(TAG, getConfiguration().toString());
		
		getInstance().initServices();
		bindConnectionHelper();
	}
	
	public void initServices(){
		mBluetoothExecutor = BluetoothAndroidThread.get();
		mBluetoothScoService = new BluetoothSCOService();
		mBluetoothConnectService = new BluetoothDeviceConnectionService();
		mBluetoothAdapterService = new BluetoothAdapterService();
		
		mHeadsetProfileService = new HeadsetProfileService();
		mA2dpProfileService = new A2dpProfileService();
		mAudioService = new AudioService();
		mTelephonyService = new TelephonyService();
		
		mProfileServiceMapping.put(HeadsetProfileService.PROFILE, mHeadsetProfileService);
		mProfileServiceMapping.put(A2dpProfileService.PROFILE, mA2dpProfileService);
		
		try {
			mBluetoothAdapterService.init();
			mBluetoothConnectService.init();
			mBluetoothScoService.init();
			mA2dpProfileService.init();
			mHeadsetProfileService.init();
			mAudioService.init();
			mTelephonyService.init();
		} catch (BluetoothException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public boolean destory() {
		check();
		
		mBluetoothAdapterService.destory();
		mBluetoothConnectService.destory();
		mBluetoothScoService.destory();
		mHeadsetProfileService.destory();
		mA2dpProfileService.destory();
		mAudioService.destory();
		mTelephonyService.destory();
		return true;
	}
	
	static void check(){
		if(getInstance().mContext == null){
			throw new BluetoothRuntimeException("please invoke BMS#init");
		}
	}
	
	public static void execute(Runnable task){
		getInstance().mBluetoothExecutor.execute(task);
	}
	
	public static boolean isReady(){
		return getInstance().mContext != null;
	}
	
	public static Context getContext(){
		return getInstance().mContext;
	}
	
	static boolean bindConnectionHelper(){
		ConnectionHelper connectionHelper = ConnectionHelper.getHelper();
		registerBluetoothProfileServiceStateListener(HeadsetProfileService.PROFILE,connectionHelper);
		registerProfileConnectionStateChangedListener(connectionHelper,new int[]{HeadsetProfileService.PROFILE,A2dpProfileService.PROFILE});
		registerBluetoothAdapterStateChangedListener(connectionHelper.getBluetoothAdapterStateListener());
		HFP.registerAudioStateChangedListener(connectionHelper);
		return true;
	}
	
	public static boolean isDebugable(){
		return getInstance().mConfiguration.isDebug();
	}
	
	public static boolean isDebugableThread(){
		return getInstance().mConfiguration.isDebugThread();
	}
	
	public static boolean isSupport(){
		return getInstance().mConfiguration.isSupport();
	}
	
	public static Configuration getConfiguration(){
		return getInstance().mConfiguration;
	}
	
	public static boolean isOnlyBluetoothMode(){
		return (getInstance().mConfiguration.getConnectionMode() == ConnectionMode.BLUETOOTH_ONLY);
	}
	
	public static boolean getProfileService(final int profileParam,final ServiceListener serviceListener){
		return getInstance().mBluetoothAdapterService.getProfileService(profileParam, serviceListener);
	}
	
	public static int getProfileConnectionState(int profile) {
		return getInstance().mBluetoothAdapterService.getProfileConnectionState(profile);
	}
	
	public static int getConnectionState() {
		return getInstance().mBluetoothAdapterService.getConnectionState();
	}
	
	public static boolean isDeviceConnected(){
		return (getConnectionState() == BluetoothAdapter.STATE_CONNECTED);
	}
	
	@Deprecated
	public static void registerBluetoothConnectionListener(BluetoothConnectionStateListener lis){
		getInstance().mBluetoothConnectService.setConnectionStateListener(lis);
	}
	
	public static void registerBluetoothProtocolConnectionStateListener(BluetoothProtocolConnectionStateListener listener){
		getInstance().mProtocolConnectionStateListener = (listener!=null ? listener : BluetoothProtocolConnectionStateListener.EMTPY);
	}
	
	public static BluetoothProtocolConnectionStateListener getBluetoothProtocolConnectionStateListener(){
		return getInstance().mProtocolConnectionStateListener;
	}
	
	public static <DataType> void registerBluetoothMessageParser(SPPBluetoothMessageParser<DataType> messageParser) {
		OkBluetooth.sSppBluetoothMessageParser = (messageParser == null ? SPPBluetoothMessageParser.DEFAULT : messageParser);
	}
	
	public static void registerBluetoothProfileServiceStateListener(int profile,BluetoothProfileServiceStateListener serviceStateListener) {
		getBluetoothProfileService(profile).registerBluetoothProfileServiceListener(serviceStateListener);
	}
	
	public static void unregisterBluetoothProfileServiceStateListener(int profile,BluetoothProfileServiceStateListener serviceStateListener){
		getBluetoothProfileService(profile).unregisterBluetoothProfileServiceListener(BluetoothProfileServiceStateListener.EMPTY);
	}
	
	public static void registerBluetoothAdapterStateChangedListener(BluetoothAdapterStateListener adapterStateListener) {
		getInstance().mBluetoothAdapterService.setBluetoothAdapterStateListener(adapterStateListener);
	}
	
	public static void unregisterBluetoothAdapterStateChangedListener(BluetoothAdapterStateListener adapterStateListener){
		getInstance().mBluetoothAdapterService.setBluetoothAdapterStateListener(BluetoothAdapterStateListener.EMPTY);
	}
	
	public static void registerProfileConnectionStateChangedListener(BluetoothProfileConnectionStateChangedListener lis,int... profiles) {
		
		for(int profile : profiles) {
			getBluetoothProfileService(profile).registerProfileConnectionStateChangedListener(lis);
		}
		
	}
	
	public static void unregisterProfileConnectionStateChangedListener(BluetoothProfileConnectionStateChangedListener lis,int... profiles){
		for(int profile : profiles) {
			getBluetoothProfileService(profile).unregisterProfileConnectionStateChangedListener(lis);
		}
	}
	
	public static <DataType> void registerBluetoothMessageReceiver(BluetoothMessageReceiver<DataType> receiver,Protocol...protocols) {
		BluetoothMessageDispatcher<DataType> dispatcherImpl = (BluetoothMessageDispatcher<DataType>) BluetoothMessageDispatcher.getDispatcher();
		
		for(Protocol protocol : protocols){
			dispatcherImpl.registerBluetoothMessageReceiver(protocol,receiver);
		}
		
	}
	
	public static <DataType> void unregisterMessageReceiver(BluetoothMessageReceiver<DataType> receiver,Protocol...protocols) {
		BluetoothMessageDispatcher<DataType> dispatcherImpl = (BluetoothMessageDispatcher<DataType>) BluetoothMessageDispatcher.getDispatcher();
		for(Protocol protocol : protocols){
			dispatcherImpl.unregisterBluetoothMessageReceiver(protocol,receiver);
		}
	}
	
	public static <DataType> SPPBluetoothMessageParser<DataType> getSppMessageParser(){
		return ((SPPBluetoothMessageParser<DataType>)OkBluetooth.sSppBluetoothMessageParser);
	}
	
	/**
	 * 获取所有已配对的设备
	 * @return
	 */
	public static Set<BluetoothDevice> getBondedDevices(){
		return getInstance().mBluetoothAdapterService.getBondedDevices();
	}
	
	public static boolean isSupportSPP(BluetoothDevice bluetoothDevice) {
		
		if(bluetoothDevice == null) return false;
		
		ParcelUuid[] parcelUuid = bluetoothDevice.getUuids();
		
		return BluetoothUuid.containsAnyUuid(parcelUuid, new ParcelUuid[]{new ParcelUuid(BluetoothConnection.DEFAULT_UUID)});
	}
	
	//媒体音频
	public static boolean isSupportA2DP(BluetoothDevice bluetoothDevice){
		
		if(bluetoothDevice == null) return false;
		
		ParcelUuid[] parcelUuid = bluetoothDevice.getUuids();
		
		return BluetoothUuid.containsAnyUuid(parcelUuid, A2dpProfileService.SINK_UUIDS);
	}
	
	//手机音频
	public static boolean isSupportHFP(BluetoothDevice bluetoothDevice){
		
		if(bluetoothDevice == null) return false;
		
		ParcelUuid[] parcelUuid = bluetoothDevice.getUuids();
		
		return BluetoothUuid.containsAnyUuid(parcelUuid, HeadsetProfileService.UUIDS);
	}
	
	//是否同时支持媒体与手机音频
	public static boolean isSupportHFPAndA2DP(BluetoothDevice bluetoothDevice){
		
		if(bluetoothDevice == null) return false;
		
		return isSupportA2DP(bluetoothDevice) && isSupportHFP(bluetoothDevice);
	}
	
	public static void closeBluetoothProfile(int profile,BluetoothProfile bluetoothProfile){
		getInstance().mBluetoothAdapterService.closeBluetoothProfile(profile,bluetoothProfile);
	}
	
	public static void closeBluetoothProfile(BluetoothProfile bluetoothProfile){
		getInstance().mBluetoothAdapterService.closeBluetoothProfile(bluetoothProfile);
	}
	
	public static void closeBluetoothProfiles(List<BluetoothProfile> profiles){
		if(profiles!=null){
			for (int i = 0; i < profiles.size(); i++) {
				closeBluetoothProfile(profiles.get(i));
			}
		}
	}

	public static ProfileConnectionState getConnectionState(int profile,final BluetoothDevice device) {
		
		BluetoothUtils.ifNullThrowException(device);
		
		return getBluetoothProfileService(profile).getConnectionState(device);
    }
	
	
	
	public static boolean connect(int profile,final BluetoothDevice device){
		return getBluetoothProfileService(profile).connect(device);
	}
	
	public static boolean disconnect(int profile,final BluetoothDevice device){
		return getBluetoothProfileService(profile).disconnect(device);
	}
	
	public static List<BluetoothDevice> getConnectedBluetoothDeviceList(int profile){
		return getBluetoothProfileService(profile).getConnectedBluetoothDeviceList();
	}
	
	public static List<BluetoothDevice> getConnectedBluetoothDeviceList(int profile,final String deviceName){
		
		BluetoothUtils.ifNullThrowException(deviceName);
		
		return getBluetoothProfileService(profile).getConnectedBluetoothDeviceList(deviceName);
	}
	
	public static void setAudioModeModifier(AudioModeModifier modifier){
		getAudioService().setAudioModeModifier(modifier);
	}
	
	public static void setInterceptor(Interceptor interceptorImpl){
		getInstance().mInterceptor = (interceptorImpl!=null ? interceptorImpl : Interceptor.EMPTY);
	}
	
	public static Interceptor getInterceptor(){
		return getInstance().mInterceptor;
	}
	
	
	public static class HFP {
		
		// HSP ------------------------------------------------------------------
		public static ProfileConnectionState getConnectionState(final BluetoothDevice device) {
			
			BluetoothUtils.ifNullThrowException(device);
			
			return OkBluetooth.getConnectionState(HeadsetProfileService.PROFILE,device);
	    }
		
		public static boolean isConnected(final BluetoothDevice device) {
			
			BluetoothUtils.ifNullThrowException(device);
			
			return isConnected(getConnectionState(device));
	    }
		
		public static int getPriority(final BluetoothDevice device) {
			
			BluetoothUtils.ifNullThrowException(device);
			
			return OkBluetooth.getPriority(HeadsetProfileService.PROFILE, device);
	    }
		
		/**
		 * Check if Bluetooth SCO audio is connected.
		 */
		public static boolean isAudioConnected(final BluetoothDevice device) {
			
			BluetoothUtils.ifNullThrowException(device);
			
			return getInstance().mHeadsetProfileService.isAudioConnected(device);
	    }
		
		public static boolean isAudioConnected(){
			List<BluetoothDevice> connectedDevices = getConnectedBluetoothDeviceList();
			
			for(BluetoothDevice device : connectedDevices){
				if(isAudioConnected(device)){
					return true;
				}
			}
			
			return false;
		}
		
		/**
		 * 强制系统HFP进行连接，基本等同于在系统设置中勾选手机音频复选框的效果
		 * 在调用连接之前，建议先{@link OkBluetooth#disconnect(BluetoothDevice)},然后在执行。
		 */
		public static boolean connect(final BluetoothDevice device){
			return OkBluetooth.connect(HeadsetProfileService.PROFILE,device);
		}
		/**
		 * 强制将系统HFP连接断开，基本等同于在系统设置中取消手机音频复选框的效果
		 */
		public static boolean disconnect(final BluetoothDevice device){
			return OkBluetooth.disconnect(HeadsetProfileService.PROFILE,device);
		}
		
		public static boolean connectAudio(){
			return getInstance().mHeadsetProfileService.connectAudio();
		}
		
		public static boolean disconnectAudio(){
			return getInstance().mHeadsetProfileService.disconnectAudio();
		}
		
		public static List<BluetoothDevice> getConnectedBluetoothDeviceList(){
			return OkBluetooth.getConnectedBluetoothDeviceList(HeadsetProfileService.PROFILE);
		}
		
		public static BluetoothDevice getAudioConnectedDevice(){
			List<BluetoothDevice> list = getConnectedBluetoothDeviceList();
			for(BluetoothDevice device : list){
				if(isAudioConnected(device)){
					return device;
				}
			}
			return null;
		}
		
		public static List<BluetoothDevice> getConnectedBluetoothDeviceList(final String deviceName){
			
			BluetoothUtils.ifNullThrowException(deviceName);
			
			return OkBluetooth.getConnectedBluetoothDeviceList(HeadsetProfileService.PROFILE,deviceName);
		}
		
		public static int getAudioState(final BluetoothDevice device){
			BluetoothUtils.ifNullThrowException(device);
			
			return getInstance().mHeadsetProfileService.getAudioState(device);
		}
		
		public static boolean isAudioOn(){
			return getInstance().mHeadsetProfileService.isAudioOn();
		}
		
		public static boolean isConnected(ProfileConnectionState state){
			switch (state) {
			case CONNECTED:
			case CONNECTED_NO_MEDIA:
				return true;
			case DISCONNECTED:
			case CONNECTED_NO_PHONE:
			case CONNECTED_NO_PHONE_AND_MEIDA:
				return false;
			}
			
			return false;
		}
		
		public static void registerAudioStateChangedListener(BluetoothHeadsetAudioStateListener lis) {
			getInstance().mHeadsetProfileService.registerAudioStateChangedListener(lis);
		}
		
		public void unregisterAudioStateChangedListener(BluetoothHeadsetAudioStateListener lis){
			getInstance().mHeadsetProfileService.unregisterAudioStateChangedListener(lis);
		}
		
		public static boolean hasConnectedDevice(){
			
			LinkedHashMap<BluetoothDevice, ProfileConnectionState> map = OkBluetooth.getAllProfileConnectionState();
			
			for(Iterator<Map.Entry<BluetoothDevice, ProfileConnectionState>> iter = map.entrySet().iterator();iter.hasNext();){
				
				Map.Entry<BluetoothDevice, ProfileConnectionState> item = iter.next();
				
				final BluetoothDevice connectedBluetoothDevice = item.getKey();
				ProfileConnectionState state = item.getValue();
				
				boolean isSupportHFP = OkBluetooth.isSupportHFP(connectedBluetoothDevice);
				
				if(isSupportHFP && (state == ProfileConnectionState.CONNECTED || state == ProfileConnectionState.CONNECTED_NO_MEDIA)){
					return true;
				}
			}
			
			return false;
		}
		
		public static BluetoothDevice getFristConnectedDevice(){
			
			LinkedHashMap<BluetoothDevice, ProfileConnectionState> map = OkBluetooth.getAllProfileConnectionState();
			
			for(Iterator<Map.Entry<BluetoothDevice, ProfileConnectionState>> iter = map.entrySet().iterator();iter.hasNext();){
				
				Map.Entry<BluetoothDevice, ProfileConnectionState> item = iter.next();
				
				final BluetoothDevice connectedBluetoothDevice = item.getKey();
				ProfileConnectionState state = item.getValue();
				
				boolean isSupportHFP = OkBluetooth.isSupportHFP(connectedBluetoothDevice);
				
				if(isSupportHFP && (state == ProfileConnectionState.CONNECTED || state == ProfileConnectionState.CONNECTED_NO_MEDIA)){
					return connectedBluetoothDevice;
				}
			}
			
			return null;
		}
		
	}
	
	public static class A2DP {
		/**
		 * 强制将系统A2DP连接断开，基本等同于在系统设置中取消媒体音频复选框的效果
		 */
		public static boolean disconnect(final BluetoothDevice device){
			return getBluetoothProfileService(A2dpProfileService.PROFILE).disconnect(device);
		}
		
		public static boolean connect(final BluetoothDevice device){
			return getBluetoothProfileService(A2dpProfileService.PROFILE).connect(device);
		}
		
		public static boolean isA2dpPlaying(BluetoothDevice device) {
			return getInstance().mA2dpProfileService.isA2dpPlaying(device);
		}
	}
	
	public static int getPriority(int profile,final BluetoothDevice device) {
		
		BluetoothUtils.ifNullThrowException(device);
		
		return getBluetoothProfileService(profile).getPriority(device);
	}
	
	
	public static boolean setPriority(int profile,final int priority,final BluetoothDevice device){
		
		BluetoothUtils.ifNullThrowException(device);
		
		return getBluetoothProfileService(profile).setPriority(priority, device);
	}
	
	public static void startSco(){
		
		check();
		
		getInstance().mBluetoothScoService.startSco();
	}
	
	public static void stopSco(){
		check();
		
		getInstance().mBluetoothScoService.stopSco();
	}
	
	/**
	 * 打开蓝牙
	 */
	public static void enableBluetooth(){
		getInstance().mBluetoothAdapterService.enable();
	}
	/**
	 * 系统蓝牙开关是否打开
	 * @return true:打开
	 */
	public static boolean isBluetoothEnable(){
		return getInstance().mBluetoothAdapterService.isEnable();
	}
	
	public static void disableBluetooth(){
		getInstance().mBluetoothAdapterService.disable();
	}

	public static void startDebugThread() {
		if(isDebugable() && isDebugableThread()) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					while(true){
						BluetoothUtils.dumpAudioState(TAG);
						BluetoothUtils.dumpBluetoothStates(TAG);
						
						try {
							Thread.sleep(1*1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}).start();
		}
	}
	
	public static List<BluetoothDevice> getConnectedBluetoothDeviceList(int... profiles){
		
		Set<BluetoothDevice> result = new HashSet<BluetoothDevice>();
		
		for(int profile : profiles){
			result.addAll(getConnectedBluetoothDeviceList(profile));
		}
		
		return new ArrayList<>(result);
	}
	
	
	public static void disconnectOther(BluetoothDevice connectedDevices){
		LinkedHashMap<BluetoothDevice, ProfileConnectionState> map = OkBluetooth.getAllProfileConnectionState();
		
		for(Iterator<Map.Entry<BluetoothDevice, ProfileConnectionState>> iter = map.entrySet().iterator();iter.hasNext();){
			
			Map.Entry<BluetoothDevice, ProfileConnectionState> item = iter.next();
			
			final BluetoothDevice connectedBluetoothDevice = item.getKey();
			ProfileConnectionState state = item.getValue();
			
			if(!connectedBluetoothDevice.equals(connectedDevices)) {
				
				switch (state) {
				case CONNECTED:
				case CONNECTED_NO_PHONE:
				case CONNECTED_NO_MEDIA:
				case CONNECTED_NO_PHONE_AND_MEIDA:
					if(OkBluetooth.isSupportHFP(connectedBluetoothDevice)){
						OkBluetooth.HFP.disconnect(connectedBluetoothDevice);
					}
					if(OkBluetooth.isSupportA2DP(connectedBluetoothDevice)){
						OkBluetooth.disconnect(A2dpProfileService.PROFILE, connectedBluetoothDevice);
					}
					break;
				default:
					break;
				}
			}
		}
	}
	
	public static LinkedHashMap<BluetoothDevice,ProfileConnectionState> getAllProfileConnectionState(){
		
		final int[] profiles = new int[]{HeadsetProfileService.PROFILE,A2dpProfileService.PROFILE};
		
		List<BluetoothDevice> bluetoothDevices = getConnectedBluetoothDeviceList(profiles);
		
		//获取支持headset与a2dp的所有已连接的设备
		//headset的设备可能只能获取一个，因为当前只能有一个处于正在的已连接状态，其余都是CONNECTED_NO_PHONE状态
		
		try {
			LinkedHashMap<BluetoothDevice,ProfileConnectionState> result = new LinkedHashMap<BluetoothDevice,ProfileConnectionState>();
			
			
			for (int j = 0; j < bluetoothDevices.size(); j++) {

				boolean profileConnected = false; 
				boolean a2dpNotConnected = false;
				boolean headsetNotConnected = false; 
				
				BluetoothDevice bluetoothDevice = bluetoothDevices.get(j);
				
				boolean isSupportHFP = OkBluetooth.isSupportHFP(bluetoothDevice);
				boolean isSupportA2DP = OkBluetooth.isSupportA2DP(bluetoothDevice);

				for (int i = 0; i < profiles.length; i++) {

					int profileInt = profiles[i];
					BluetoothProfileService profileService = getBluetoothProfileService(profileInt);

					Log.i(TAG, "bluetoothProfile = "+ profileService);

					boolean isA2dpProfile = false;
					boolean isHfpProfile = false;

					if (profileInt == A2dpProfileService.PROFILE) {
						isA2dpProfile = true;
					} else if (profileInt == HeadsetProfileService.PROFILE) {
						isHfpProfile = true;
					}

					if((isA2dpProfile && isSupportA2DP) || (isSupportHFP && isHfpProfile)) {
						ProfileConnectionState connectionStatus = profileService.getConnectionState(bluetoothDevice);

						Log.i(TAG,"connectionStatus = "+ connectionStatus + " , deviceName = "+ bluetoothDevice.getName());

						switch (connectionStatus) {
						case CONNECTING:
						case CONNECTED:
							profileConnected = true;
							break;

						case DISCONNECTED:
							if (profileInt == A2dpProfileService.PROFILE) {
								a2dpNotConnected = true;
							} else if (profileInt == HeadsetProfileService.PROFILE) {
								headsetNotConnected = true;
							}
							break;
						}
					}
					
					if (profileService instanceof HeadsetProfileService) {
						HeadsetProfileService bluetoothHeadset = (HeadsetProfileService) profileService;
						
						int audioState = bluetoothHeadset.getAudioState(bluetoothDevice);
						String scoStateString = BluetoothUtils.getScoStateStringFromHeadsetProfile(audioState);
						
						boolean isAudioConnected = bluetoothHeadset.isAudioConnected(bluetoothDevice);
						
						//android 4.1 不存在这个方法，需要适配
						boolean isAudioOn = bluetoothHeadset.isAudioOn();
						
						Log.i(TAG, "isAudioConnected = " + isAudioConnected + " , scoStateString = " + scoStateString + " , isAudioOn = " + isAudioOn);
					}

				}
				ProfileConnectionState currentState = ProfileConnectionState.DISCONNECTED;
				if (profileConnected) {
					if (a2dpNotConnected && headsetNotConnected) {
						Log.i(TAG, bluetoothDevice.getName() + " Connected (no phone or media)");
						currentState = ProfileConnectionState.CONNECTED_NO_PHONE_AND_MEIDA;
					} else if (a2dpNotConnected) {
						Log.i(TAG, bluetoothDevice.getName() + " Connected (no media)");
						currentState = ProfileConnectionState.CONNECTED_NO_MEDIA;
					} else if (headsetNotConnected) {
						Log.i(TAG, bluetoothDevice.getName() + " Connected (no phone)");
						currentState = ProfileConnectionState.CONNECTED_NO_PHONE;
					} else {
						Log.i(TAG, bluetoothDevice.getName() + " Connected");
						currentState = ProfileConnectionState.CONNECTED;
					}
				} 
				
				result.put(bluetoothDevice, currentState);

			}
			
			return result;
		} finally {
		}
			
	}
	
	static AudioService getAudioService() {
		return getInstance().mAudioService;
	}
	
	public static void setAudioMode(int mode) {
		getAudioService().setAudioMode(mode);
	}
	
	public static int getAudioMode(){
		return getAudioService().getAudioMode();
	}
	
	public static void setScoStreamVolumn(int volumnIndex,int flags){
		getAudioService().setScoStreamVolumn(volumnIndex,flags);
	}
	
	public static void setStreamVolumn(int streamType,int volumnIndex,int flags){
		getAudioService().setStreamVolumn(streamType, (volumnIndex), flags);
	}
	
	public static int getCurrentStreamVolumn(int streamType){
		return getAudioService().getCurrentStreamVolumn(streamType);
	}
	
	public static int getMaxStreamVolumn(int streamType){
		return getAudioService().getMaxStreamVolumn(streamType);
	}
	
	public static int getScoStreamVolumn(){
		return getAudioService().getScoStreamVolumn();
	}
	
	public static int getScoMaxStreamVolumn(){
		return getAudioService().getScoMaxStreamVolumn();
	}

	public static void setSpeakerphoneOn(boolean on){
		getAudioService().setSpeakerphoneOn(on);
	}
	
	public static void setBluetoothScoOn(boolean on){
		getAudioService().setBluetoothScoOn(on);
	}
	
	public static boolean isBluetoothScoOn(){
		return getAudioService().isBluetoothScoOn();
	}
	
	public static boolean isSpeakerphoneOn(){
		return getAudioService().isSpeakerphoneOn();
	}
	
	public static boolean isBluetoothA2dpOn(){
		return getAudioService().isBluetoothA2dpOn();
	}
	
	public static boolean isWiredHeadsetOn(){
		return getAudioService().isWiredHeadsetOn();
	}
	
	public static void setBluetoothA2dpOn(boolean on){
		getAudioService().setBluetoothA2dpOn(on);
	}
	
	static boolean connectAudioInternal(AudioDevice audioDevice){
		return getAudioService().connectAudio(audioDevice);
	}
	
	public static boolean connectAudio(AudioDevice audioDevice){
		ConnectionHelper.getHelper().connectAudio(audioDevice);
		return true;
	}
	
	public static boolean hasForcePhoneRing(){
		return (getConfiguration().getForceTypes() & FORCE_TYPE_PHONE_RING) != 0;
	}
	
	public static boolean hasForcePhoneIncall(){
		return (getConfiguration().getForceTypes() & FORCE_TYPE_PHONE_INCALL) != 0;
	}
	
	public static boolean hasForcePhoneIdle(){
		return (getConfiguration().getForceTypes() & FORCE_TYPE_PHONE_INCALL_TO_IDLE) != 0;
	}

	public static boolean isBluetoothAvailable() {
		return OkBluetooth.HFP.hasConnectedDevice();
	}

	public static void tryRecoveryAudioConnection(){
		ConnectionHelper.getHelper().tryRecoveryAudioConnection();
	}
	
	static void tryRecoveryAudioConnection(Event event){
		ConnectionHelper.getHelper().tryRecoveryAudioConnection(event);
	}
	
	public static void submitAudioConnectTask(){
		ConnectionHelper.getHelper().submitAudioConnectTask();
	}
	
	public static void requestAudioFocusForCall(int streamType){
		getAudioService().requestAudioFocusForCall(streamType);
	}
	
	public static boolean isPhoneCalling(){
		return getInstance().mTelephonyService.isPhoneCalling();
	}
	
	static BluetoothProfileService getBluetoothProfileService(int profile) {
		BluetoothProfileService service = getInstance().mProfileServiceMapping.get(profile);
		if(service == null)
			throw new BluetoothRuntimeException("not support profile("+BluetoothUtils.getProfileString(profile)+")");
		return service;
	}
	
	public enum ConnectionMode {
		/**
		 * 只要连接上蓝牙设备,将仅走蓝牙设备
		 */
		BLUETOOTH_ONLY,
		/**
		 * 在连接上蓝牙设备之后,可以走三种设备(蓝牙|扬声器|听筒)
		 */
		BLUETOOTH_WIREDHEADSET_SPEAKER
		
	}
	
	public interface Callback2<Key,Value> {
		public void callback(Key k , Value v);
	}
	
	/**
	 * 当HFP|SPP连接断开或者已连接时通知客户端,通过{@link Protocol}区分类型
	 * @author wei.deng
	 *
	 */
	public static abstract class BluetoothProtocolConnectionStateListener {
		
		public static final BluetoothProtocolConnectionStateListener EMTPY = new BluetoothProtocolConnectionStateListener() {
		};
		
		public void onDisconnected(Protocol protocol,BluetoothDevice device){}
		public void onConnected(Protocol protocol,BluetoothDevice device){}
	}
	
	/**
	 * 拦截器
	 * @author wei.deng
	 */
	public static abstract class Interceptor {
		
		public static final Interceptor EMPTY = new Interceptor() {
		};
		
		public boolean beforeRunConnectTask(){
			return false;
		}
		
		/**
		 * 系统电话中
		 * @return
		 */
		public boolean systemPhoneCalling(){
			return false;
		}
		
		public boolean beforeConnectBluetoothProfile(){
			return false;
		}
		
		/**
		 * 连接音频设备之前
		 */
		public boolean beforeConnect(AudioDevice audioDevice){
			return false;
		}

		
	}
	
}
