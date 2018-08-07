package com.devyok.bluetooth.base;

import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
/**
 * @author wei.deng
 */
public interface BluetoothProfileService extends BluetoothServiceLifecycle{
	
	public interface BluetoothProfileServiceStateListener {
		public static final BluetoothProfileServiceStateListener EMPTY = new BluetoothProfileServiceStateListener(){
			@Override
			public void onServiceReady(int profile,BluetoothProfileService service) {
			}
		};
		public void onServiceReady(int profile, BluetoothProfileService service);
	}
	
	public interface BluetoothProfileConnectionStateChangedListener {
		
		public static final BluetoothProfileConnectionStateChangedListener EMPTY = new BluetoothProfileConnectionStateChangedListener() {
			@Override
			public void onDisconnected(int profile, int newState, int preState,
					BluetoothDevice device) {
			}
			@Override
			public void onConnected(int profile, int newState, int preState,
					BluetoothDevice device) {
			}
		};
		
		public void onConnected(int profile, int newState, int preState, BluetoothDevice device);
		public void onDisconnected(int profile, int newState, int preState, BluetoothDevice device);
	}
	
	public void registerProfileConnectionStateChangedListener(BluetoothProfileConnectionStateChangedListener lis);
	public void unregisterProfileConnectionStateChangedListener(BluetoothProfileConnectionStateChangedListener lis);
	
	public void registerBluetoothProfileServiceListener(BluetoothProfileServiceStateListener lis); 
	public void unregisterBluetoothProfileServiceListener(BluetoothProfileServiceStateListener lis);
	
	/**
	 * DISCONNECTED��ordinal��{@link BluetoothProfile#STATE_DISCONNECTED}��ֵ�Ƕ�Ӧ��,����˳���ܱ�,��������������
	 */
	public enum ProfileConnectionState {
		DISCONNECTED,
		CONNECTING,
		CONNECTED,
		DISCONNECTING,
		CONNECTED_NO_PHONE,
		CONNECTED_NO_MEDIA,
		CONNECTED_NO_PHONE_AND_MEIDA;

		public static ProfileConnectionState toState(int state) {
			
			ProfileConnectionState[] states = ProfileConnectionState.values();
			
			for(ProfileConnectionState item : states){
				if(item.ordinal() == state){
					return item;
				}
			}
			
			return ProfileConnectionState.DISCONNECTED;
		}

		public static boolean isConnected(ProfileConnectionState state) {
			
			switch (state) {
			case CONNECTED:
			case CONNECTED_NO_PHONE:
			case CONNECTED_NO_MEDIA:
			case CONNECTED_NO_PHONE_AND_MEIDA:
				return true;
			}
			
			return false;
		}
	}
	
	
	public ProfileConnectionState getConnectionState(final BluetoothDevice device);
	
	public ProfileConnectionState getCurrentConnectionState();
	
	public List<BluetoothDevice> getConnectedBluetoothDeviceList();
	
	
	public List<BluetoothDevice> getConnectedBluetoothDeviceList(final String deviceName);
	/**
	 * ��android4.2�Ļ��������Ͽ��ɹ�֮�󣬻Ὣ���ȼ�����ΪON����ô�����������������Զ�����
	 * ����5.0�Ļ�������
	 * 
	 */
	public boolean disconnect(final BluetoothDevice device) ;
	/**
	 * android�汾���� 4.4֮��,����a2dpʧ�ܣ��׳�Ȩ���쳣��android.permission.WRITE_SECURE_SETTINGS(ϵͳ��)
	 * �Ͽ�û����
	 */
	public boolean connect(final BluetoothDevice device) ;
	
	public int getPriority(final BluetoothDevice device);
	/**
	 * android�汾���� 4.4֮��,setPriorityʧ�ܣ��׳�Ȩ���쳣��android.permission.WRITE_SECURE_SETTINGS(ϵͳ��)
	 * @param priority
	 * @param device
	 * @return
	 */
	public boolean setPriority(final int priority, final BluetoothDevice device);
	
	public static final BluetoothProfileService EMPTY = new BluetoothProfileService() {

		@Override
		public void registerProfileConnectionStateChangedListener(
				BluetoothProfileConnectionStateChangedListener lis) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unregisterProfileConnectionStateChangedListener(
				BluetoothProfileConnectionStateChangedListener lis) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public ProfileConnectionState getConnectionState(BluetoothDevice device) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ProfileConnectionState getCurrentConnectionState() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<BluetoothDevice> getConnectedBluetoothDeviceList() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<BluetoothDevice> getConnectedBluetoothDeviceList(
				String deviceName) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean disconnect(BluetoothDevice device) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean connect(BluetoothDevice device) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getPriority(BluetoothDevice device) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean setPriority(int priority, BluetoothDevice device) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void registerBluetoothProfileServiceListener(
				BluetoothProfileServiceStateListener lis) {
		}

		@Override
		public void unregisterBluetoothProfileServiceListener(
				BluetoothProfileServiceStateListener lis) {
		}

		@Override
		public boolean init() throws BluetoothException {
			return false;
		}

		@Override
		public boolean destory() {
			return false;
		}};
	
}
