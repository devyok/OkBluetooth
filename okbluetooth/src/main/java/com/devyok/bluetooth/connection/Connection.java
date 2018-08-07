package com.devyok.bluetooth.connection;

import android.bluetooth.BluetoothDevice;

import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.connection.BluetoothConnection.State;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

/**
 * 蓝牙连接建立的策略
 * @author wei.deng
 */
public interface Connection<TransactCore extends Closeable> {

	public long getTimeout();
	public void connect() throws BluetoothConnectionException , BluetoothConnectionTimeoutException , BluetoothException;
	public void connect(UUID sppuuid,BluetoothDevice connectedDevice) throws BluetoothConnectionException , BluetoothConnectionTimeoutException , BluetoothException;
	public void disconnect();
	public boolean isConnected();
	public void cancel();
	public void reset();
	public TransactCore getCore();
	public UUID getUuid();
	public BluetoothDevice getBluetoothDevice();
	public State getState();

	public static Connection<?> EMPTY = new Connection() {

		@Override
		public long getTimeout() {
			return -1;
		}

		@Override
		public void connect() throws BluetoothConnectionException,
				BluetoothConnectionTimeoutException, BluetoothException {
			throw new BluetoothConnectionException("empty");
		}

		@Override
		public void connect(UUID sppuuid, BluetoothDevice connectedDevice)
				throws BluetoothConnectionException,
				BluetoothConnectionTimeoutException, BluetoothException {
			throw new BluetoothConnectionException("empty");
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean isConnected() {
			return false;
		}

		@Override
		public void cancel() {
		}

		@Override
		public void reset() {
		}

		@Override
		public Closeable getCore() {
			return new Closeable() {

				@Override
				public void close() throws IOException {
				}
			};
		}

		@Override
		public UUID getUuid() {
			return UUID.randomUUID();
		}

		@Override
		public BluetoothDevice getBluetoothDevice() {
			return null;
		}

		@Override
		public State getState() {
			return State.UNKNOW;
		}
	};

}
