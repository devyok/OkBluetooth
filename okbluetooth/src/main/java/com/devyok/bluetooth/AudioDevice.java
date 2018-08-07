package com.devyok.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.devyok.bluetooth.connection.Connection;

/**
 * @author wei.deng
 */
public final class AudioDevice {

	private String name;
	private WorkInformation workInfomation = WorkInformation.sPool;

	public static final AudioDevice SBP = new AudioDevice("select_by_priority");

	public static final AudioDevice A2DP = new AudioDevice("a2dp");

	public static final AudioDevice SCO = new AudioDevice("sco");

	public static final AudioDevice WIREDHEADSET = new AudioDevice("wiredheadset");

	public static final AudioDevice SPEAKER = new AudioDevice("speaker");

	public boolean isSBP() {
		return (this == AudioDevice.SBP);
	}

	private AudioDevice(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	public WorkInformation getWorkInfomation() {
		return workInfomation;
	}

	public AudioDevice setWorkInformation(WorkInformation workInfomation){
		this.workInfomation = workInfomation;
		return this;
	}

	@Override
	public boolean equals(Object obj) {

		if(obj == null) return false;

		AudioDevice audioDevice = (AudioDevice) obj;

		return (this.name.equals(audioDevice.getName()));
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public String toString() {
		return "AudioDevice {name="+getName()+"}";
	}

	@Deprecated
	public static class WorkInformation {

		private static final WorkInformation sPool = new WorkInformation();

		/**
		 * HFP的连接
		 */
		public Connection hfpConnection = Connection.EMPTY;
		/**
		 * SPP的连接
		 */
		public Connection sppConnection = Connection.EMPTY;
		/**
		 * 当前已连接的设备,有可能为空
		 */
		public BluetoothDevice currentConnectedDevice;
		/**
		 * 最近一次与此设备进行了HFP|SPP的连接
		 */
		public BluetoothDevice lastConnectedDevice;
		private WorkInformation(){}

		public static WorkInformation pool(Connection hfpConnection,Connection sppConnection,BluetoothDevice currentConnectedDevice,BluetoothDevice lastConnectionDevice){
			WorkInformation interceptorArgs = sPool;

			interceptorArgs.hfpConnection = (hfpConnection!=null ? hfpConnection : Connection.EMPTY);
			interceptorArgs.sppConnection = (sppConnection!=null ? sppConnection : Connection.EMPTY);
			interceptorArgs.currentConnectedDevice = currentConnectedDevice;
			interceptorArgs.lastConnectedDevice = lastConnectionDevice;

			return interceptorArgs;
		}

		@Deprecated
		static WorkInformation newArgs(Connection hfpConnection,Connection sppConnection,BluetoothDevice currentConnectedDevice,BluetoothDevice lastConnectionDevice) {
			WorkInformation interceptorArgs = new WorkInformation();

			interceptorArgs.hfpConnection = (hfpConnection!=null ? hfpConnection : Connection.EMPTY);
			interceptorArgs.sppConnection = (sppConnection!=null ? sppConnection : Connection.EMPTY);
			interceptorArgs.currentConnectedDevice = currentConnectedDevice;
			interceptorArgs.lastConnectedDevice = lastConnectionDevice;

			return interceptorArgs;
		}

	}

}
