package com.devyok.bluetooth.connection;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.devyok.bluetooth.base.BluetoothException;
import com.devyok.bluetooth.connection.BluetoothConnection.BluetoothConnectionListener;
import com.devyok.bluetooth.connection.BluetoothConnection.State;
import com.devyok.bluetooth.utils.BluetoothUtils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 * 负责与蓝牙设备建立连接
 * @author wei.deng
 */
@SuppressWarnings("rawtypes")
final class BluetoothConnectionImpl extends AbstractBluetoothConnection<Closeable> {

	private static final String TAG = BluetoothConnectionImpl.class.getSimpleName();

	final RetryPolicy retryPolicy;
	final BluetoothConnectionListener connectionListener;
	final List<Connection> sppConnectPolicyList;
	Connection currentConnectPolicy = null;
	static final ExecutorService btConnectionExecutorService = Executors.newCachedThreadPool(BluetoothUtils.createThreadFactory("bt.runtime.connection"));

	private BluetoothConnectionImpl(UUID sppuuid,BluetoothDevice connectedDevice){
		this(sppuuid,connectedDevice,null,null,null);
	}

	private BluetoothConnectionImpl(UUID sppid,BluetoothDevice connectedDevice,Connection<? extends Closeable>[] connectPolicies,RetryPolicy retryPolicy,BluetoothConnectionListener connectionListener){
		super(sppid,connectedDevice);
		sppConnectPolicyList = new ArrayList<Connection>();
		if(connectPolicies!=null){
			for(int i = 0;i < connectPolicies.length;i++){
				sppConnectPolicyList.add(connectPolicies[i]);
			}
		}
		this.retryPolicy = retryPolicy;
		this.connectionListener = connectionListener;
	}

	static BluetoothConnectionImpl open(BluetoothDevice connectedDevice) throws BluetoothException {
		return open(BluetoothConnection.DEFAULT_UUID,connectedDevice,new DefaultRetryPolicy(),null,null);
	}

	static BluetoothConnectionImpl open(UUID sppuuid,BluetoothDevice connectedDevice) throws BluetoothException {
		return open(sppuuid,connectedDevice,new DefaultRetryPolicy(),null,null);
	}

	static BluetoothConnectionImpl open(UUID sppuuid,BluetoothDevice connectedDevice,RetryPolicy policy) throws BluetoothException {
		return open(sppuuid,connectedDevice,policy,null,null);
	}

	static BluetoothConnectionImpl open(UUID sppuuid,BluetoothDevice connectedDevice,RetryPolicy policy,BluetoothConnectionListener lis) throws BluetoothException {
		return open(sppuuid,connectedDevice,policy,lis,null);
	}

	/**
	 * 开启一个SPP连接
	 * @param sppuuid 连接蓝牙设备时需要的ID
	 * @param connectedDevice 已连接的蓝牙设备
	 * @param policy 当SPP连接失败后，尝试重连的策略
	 * @return
	 * @throws BluetoothException
	 */
	static BluetoothConnectionImpl open(UUID sppuuid,BluetoothDevice connectedDevice,RetryPolicy policy,BluetoothConnectionListener sppConnectListener,Connection[] sppConnectPolicyArray) throws BluetoothException {

		if(sppuuid == null){
			throw new BluetoothException("sppuuid == null");
		}

		Log.i(TAG, "[devybt btconnection] open sppuuid = " + sppuuid);

		if(connectedDevice == null){
			throw new BluetoothException("connectedDevice == null");
		}

		BluetoothConnectionImpl sppConnection = new BluetoothConnectionImpl(sppuuid,connectedDevice,sppConnectPolicyArray,policy,sppConnectListener);
		return sppConnection;
	}


	/**
	 * 连接蓝牙设备
	 * @throws BluetoothConnectionException
	 * @throws BluetoothException
	 */
	public void connect() throws BluetoothConnectionException , BluetoothConnectionTimeoutException , BluetoothException{

		try {
			Connection connection = currentConnectPolicy;
			if(connection!=null){
				connection.connect();
			} else {
				connection = tryConnect();
			}

			if(connection!=null && connection.isConnected()){
				this.currentConnectPolicy = connection;
				try {
					resetRetryPolicy();
				} finally {
					connectedNotifier();
				}
			}
		} catch (BluetoothConnectionException e) {
			if(!retry(e)){
				throw e;
			}
		} catch(TimeoutException e){
			if(!retry(e)){
				throw new BluetoothConnectionTimeoutException("spp connect timeout("+getTotalTimeout()+")" , e);
			}
		}
		catch(Exception e){
			throw new BluetoothException(e);
		}
	}

	boolean retry(Exception e) throws BluetoothConnectionException, BluetoothException{
		if(retryPolicy!=null){

			retryPolicy.retry(sppuuid, connectedDevice, e);
			return true;
		}
		return false;
	}

	void resetRetryPolicy(){
		if(retryPolicy!=null){
			retryPolicy.reset();
		}
	}

	void connectedNotifier(){
		if(connectionListener!=null){
			connectionListener.onConnected(this);
		}
	}

	void disconnetedNotifier() {
		if(connectionListener!=null){
			connectionListener.onDisconnected(this);
		}
	}

	@Override
	public long getTimeout() {
		return getTotalTimeout();
	}

	@Override
	public void cancel() {
		super.cancel();
		if(currentConnectPolicy!=null){
			currentConnectPolicy.cancel();
		}
	}

	@Override
	public void connect(UUID sppuuid, BluetoothDevice connectedDevice) throws BluetoothConnectionException, BluetoothConnectionTimeoutException, BluetoothException {
		this.sppuuid = sppuuid;
		this.connectedDevice = connectedDevice;
		connect();
	}

	/**
	 * 将连接成功的调整到最前面,避免在重连时可以快速连接
	 * @param bestPolicyIndex
	 */
	void adjustBestConnectPolicy(int bestPolicyIndex){
		Log.i(TAG, "[devybt btconnection] adjustBestConnectPolicy bestPolicyIndex = " + bestPolicyIndex);
		if(bestPolicyIndex == 0) return ;
		Connection bestPolicy = sppConnectPolicyList.remove(bestPolicyIndex);
		sppConnectPolicyList.add(0, bestPolicy);
	}

	/**
	 * 尝试获取所有的连接策略，进行循环连接.
	 * @return
	 * @throws BluetoothConnectionException
	 * @throws BluetoothConnectionTimeoutException
	 * @throws TimeoutException
	 */
	Connection tryConnect() throws BluetoothConnectionException, TimeoutException{

		long totalTimeout = getTotalTimeout();

		Log.i(TAG, "[devybt btconnection] start try connect policy , totalTimeout = " + totalTimeout);
		FutureTask<CallResult<Connection>> futureTask =
				new FutureTask<CallResult<Connection>>(new Callable<CallResult<Connection>>() {

					@Override
					public CallResult<Connection> call() {

						int size = sppConnectPolicyList.size();
						Log.i(TAG, "[devybt btconnection] try connect policy size = " + size);

						BluetoothConnectionException excp = null;
						for (int i = 0; i < size; i++) {
							final Connection policy = sppConnectPolicyList.get(i);
							Log.i(TAG, "[devybt btconnection] try connect policy = " + policy);
							policy.reset();
							currentConnectPolicy = policy;
							try {
								policy.connect(sppuuid, connectedDevice);
								if(policy.isConnected()) {
									adjustBestConnectPolicy(i);
									return CallResult.obtain(policy,CallResult.SUCCESS);
								}
							} catch (BluetoothConnectionException e) {
								excp = e;
							} catch (BluetoothConnectionTimeoutException e) {
								e.printStackTrace();
							} catch (BluetoothException e) {
								e.printStackTrace();
							}
						}

						return CallResult.obtain(null, CallResult.EXCEPTION,excp);
					}
				});

		btConnectionExecutorService.execute(futureTask);

		CallResult<Connection> callResult = null;
		try {
			callResult = futureTask.get(totalTimeout,TimeUnit.MILLISECONDS);

			if(callResult.isSuccess()){

				Connection connect = callResult.result;

				Log.i(TAG, "[devybt btconnection] try connect futureTask result = " + connect);

				if(connect!=null && connect.isConnected()) {
					return connect;
				}
			} else {
				if(callResult.exception!=null && callResult.exception instanceof BluetoothConnectionException){
					throw (BluetoothConnectionException)callResult.exception;
				} else {
					throw new BluetoothConnectionException(callResult.exception);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			//当出现超时之后,有一种情况是socket阻塞在connect上长时间不返回,所以需要调用policy#cancel关闭连接
			//关闭之后connect会抛出IOException，这样就打断阻塞了
			if(currentConnectPolicy!=null){
				currentConnectPolicy.cancel();
			}
			throw e;
		} finally {
			currentConnectPolicy = null;
		}

		return null;
	}

	private long getTotalTimeout() {

		int size = sppConnectPolicyList.size();
		long total = 0L;
		for (int i = 0; i < size; i++) {
			final Connection policy = sppConnectPolicyList.get(i);
			total += policy.getTimeout();
		}

		return total == 0L ? 6*1000 : total;
	}

	@Override
	public boolean isConnected() {
		return currentConnectPolicy!=null ? currentConnectPolicy.isConnected() : false;
	}

	@Override
	public State getState() {
		return currentConnectPolicy!=null ? currentConnectPolicy.getState() : State.INIT;
	}

	@Override
	public void disconnect() {
		try {
			if(currentConnectPolicy!=null){
				currentConnectPolicy.disconnect();

				disconnetedNotifier();

			}
		} finally {
			super.disconnect();
		}
	}

	static class CallResult<Result> {

		public static final int SUCCESS = 0x00;
		public static final int EXCEPTION = 0x01;

		Result result;
		int status;
		Throwable exception;

		public static <Result> CallResult<Result> obtain(Result result,int status){
			return obtain(result,status,null);
		}

		public static <Result> CallResult<Result> obtain(Result result,int status,Throwable exception){
			CallResult<Result> callResult = new CallResult<Result>();
			callResult.result = result;
			callResult.status = status;
			callResult.exception = exception;
			return callResult;
		}

		public boolean isSuccess(){
			return (status == SUCCESS);
		}

	}

}
