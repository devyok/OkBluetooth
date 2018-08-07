package com.devyok.bluetooth.connection;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import com.devyok.bluetooth.a2dp.A2dpProfileService;
import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.hfp.HFPConnection;
import com.devyok.bluetooth.hfp.HeadsetProfileService;
import com.devyok.bluetooth.utils.BluetoothUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
/**
 * @author wei.deng
 */
public abstract class BluetoothConnection{

	public static final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	/**
	 * 连接状态
	 * @author wei.deng
	 */
	public enum State {
		UNKNOW,
		INIT,
		CONNECTED,
		LISTENING,
		CLOSED,
	}

	/**
	 * 连接监听器
	 * @author wei.deng
	 */
	public static abstract class BluetoothConnectionListener {
		public void onConnected(Connection connection) {
		}

		public void onDisconnected(Connection connection) {
		}
	}
	/**
	 * 支持的协议
	 * @author wei.deng
	 *
	 */
	public static class Protocol {
		private String name;

		public static final Protocol A2DP = new Protocol("a2dp");

		public static final Protocol HFP = new Protocol("hfp");

		public static final Protocol SPP = new Protocol("spp");

		public static final Protocol[] ALL = new Protocol[]{A2DP,HFP,SPP};

		public Protocol(String name){
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static Protocol getProtocol(ParcelUuid uuid){
			throw new RuntimeException("stub");
		}

		public static Protocol getProtocol(int profile){
			if(profile == HeadsetProfileService.PROFILE) {
				return Protocol.HFP;
			} else if(profile == A2dpProfileService.PROFILE) {
				return Protocol.A2DP;
			}
			return Protocol.SPP;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			Protocol p1 = (Protocol) obj;
			return p1.getName().equals(this.name);
		}

	}

	/**
	 * sppconnection builder
	 * @author wei.deng
	 */
	public static class Builder {
		private BluetoothDevice connectedDevice;
		private UUID serviceUUID;
		private RetryPolicy connectionRetryPolicy;
		private BluetoothConnectionListener connectListener;
		private long connectionTimeout;
		private int companyid = BluetoothUtils.UNKNOW;
		final List<Connection> connectPolicyList = new ArrayList<Connection>();

		public Builder(){
		}

		public Builder setConnectedDevice(BluetoothDevice connectedDevice) {
			this.connectedDevice = connectedDevice;
			return this;
		}

		public Builder setCompanyid(int companyid) {
			this.companyid = companyid;
			return this;
		}

		public Builder setConnectionUUID(UUID sppUUID) {
			this.serviceUUID = sppUUID;
			return this;
		}

		public Builder setConnectionRetryPolicy(RetryPolicy connectionRetryPolicy) {
			this.connectionRetryPolicy = connectionRetryPolicy;
			return this;
		}

		public Builder addDefaultConnectionRetryPolicy(){
			this.connectionRetryPolicy = new DefaultRetryPolicy();
			return this;
		}

		public Builder setConnectionListener(BluetoothConnectionListener lis) {
			this.connectListener = lis;
			return this;
		}

		public Builder addConnectionPolicy(Connection connectPolicy) {
			if(!this.connectPolicyList.contains(connectPolicy)) {
				this.connectPolicyList.add(connectPolicy);
			}
			return this;
		}

		public Builder setConnectionTimeout(long timeout){
			this.connectionTimeout = timeout;
			return this;
		}

		public Connection build() throws BluetoothException{
			if(connectPolicyList.size()>0){
				for (Connection connection : connectPolicyList) {
					if(connection instanceof AbstractBluetoothConnection<?>) {
						AbstractBluetoothConnection<?> abstractBluetoothConnection = (AbstractBluetoothConnection<?>) connection;
						abstractBluetoothConnection.connectedDevice = this.connectedDevice;
						abstractBluetoothConnection.sppuuid = this.serviceUUID;
						abstractBluetoothConnection.setTimeout(connectionTimeout);

						if(abstractBluetoothConnection instanceof HFPConnection){
							HFPConnection hfpConnection = (HFPConnection) abstractBluetoothConnection;
							hfpConnection.setCompanyid(this.companyid);
						}

					}
				}
			}
			return BluetoothConnectionImpl.open(this.serviceUUID, this.connectedDevice, this.connectionRetryPolicy, this.connectListener,this.connectPolicyList.toArray(new Connection[]{}));
		}

	}

}
