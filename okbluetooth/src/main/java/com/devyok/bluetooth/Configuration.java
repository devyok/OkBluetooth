package com.devyok.bluetooth;

import com.devyok.bluetooth.OkBluetooth.ConnectionMode;
import com.devyok.bluetooth.message.BluetoothMessageDispatcher;
import com.devyok.bluetooth.message.BluetoothMessageHandler;
import com.devyok.bluetooth.utils.BluetoothUtils;

/**
 * @author deng.wei
 */
public final class Configuration {
	
	public static final Configuration DEFAULT = new Builder()
																.setDebug(true)
																.setSupport(true)
																.setConnectionMode(ConnectionMode.BLUETOOTH_WIREDHEADSET_SPEAKER)
																.build();
	
	private boolean debug;
	private boolean isSupport;
	private boolean debugThread;
	private BluetoothMessageHandler<?> messageDispatcher;
	private boolean isSupportScoDaemon = true;
	private int companyid = BluetoothUtils.UNKNOW;
	private ConnectionMode connectionMode = ConnectionMode.BLUETOOTH_WIREDHEADSET_SPEAKER;
	private int forceTypes;
	private Configuration(){}
	
	public boolean isDebugThread() {
		return debugThread;
	}
	
	public int getForceTypes(){
		return forceTypes;
	}
	
	public ConnectionMode getConnectionMode(){
		return connectionMode;
	}
	
	public int getCompanyId(){
		return this.companyid;
	}
	
	public boolean isSupportScoDaemon(){
		return isSupportScoDaemon;
	}

	public boolean isSupport() {
		return isSupport;
	}
	
	public boolean isDebug() {
		return debug;
	}
	
	public <DataType> BluetoothMessageHandler<DataType> getDispatcher(){
		return (BluetoothMessageHandler<DataType>) this.messageDispatcher;
	}
	
	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("Configuration { ");
		buffer.append("debug").append("=").append(debug).append(",")
			  .append("isSupport").append("=").append(isSupport).append(",")
			  .append("debugThread").append("=").append(debugThread).append(",")
			  .append("messageDispatcher").append("=").append(messageDispatcher).append(",")
			  .append("isSupportScoDaemon").append("=").append(isSupportScoDaemon).append(",")
			  .append("companyid").append("=").append(companyid).append(",")
			  .append("connectionMode").append("=").append(connectionMode).append(",")
			  .append("forceTypes").append("=").append(forceTypes).append(",")
			  .append("hasForcePhoneIdle").append("=").append((forceTypes & OkBluetooth.FORCE_TYPE_PHONE_INCALL_TO_IDLE)!=0 ? "Y" : "N").append(",")
			  .append("hasForcePhoneIncall").append("=").append((forceTypes & OkBluetooth.FORCE_TYPE_PHONE_INCALL)!=0 ? "Y" : "N").append(",")
			  .append("hasForcePhoneRing").append("=").append((forceTypes & OkBluetooth.FORCE_TYPE_PHONE_RING)!=0 ? "Y" : "N");
		buffer.append(" }");
		
		return buffer.toString();
	}
	
	public static class Builder {
		
		private boolean debug;
		private boolean isSupportBMS;
		private boolean debugThread;
		private int companyid;
		private BluetoothMessageHandler<?> messageDispatcher;
		private ConnectionMode connectionMode;
		private int forceTypes;
		
		public Builder(){
		}
		
		public Builder setForceTypes(int forceTypes){
			this.forceTypes = forceTypes;
			return this;
		}
		
		public Builder setCompanyId(int companyid) {
			this.companyid = companyid;
			return this;
		}
		
		public Builder setConnectionMode(ConnectionMode mode) {
			this.connectionMode = mode;
			return this;
		}
		
		public Builder setDebug(boolean debug) {
			this.debug = debug;
			return this;
		}
		
		public Builder setSupport(boolean isSupport) {
			this.isSupportBMS = isSupport;
			return this;
		}
		
		public Builder setDebugThread(boolean debugThread) {
			this.debugThread = debugThread;
			return this;
		}
		
		public <DataType> Builder setMessageDispatcher(BluetoothMessageHandler<DataType> dispatch){
			messageDispatcher = dispatch;
			return this;
		}
		
		public Configuration build(){
			Configuration configuration = new Configuration();
			
			configuration.debug = this.debug;
			configuration.debugThread = this.debugThread;
			configuration.isSupport = this.isSupportBMS;
			configuration.messageDispatcher = this.messageDispatcher;
			configuration.companyid = this.companyid;
			configuration.connectionMode = this.connectionMode;
			configuration.forceTypes = this.forceTypes;
			
			if(configuration.messageDispatcher == null){
				configuration.messageDispatcher = BluetoothMessageDispatcher.getDispatcher();
			}
			
			return configuration;
		}
		
	}
	
}
