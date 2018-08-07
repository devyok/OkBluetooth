package com.devyok.bluetooth.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.devyok.bluetooth.OkBluetooth;
import com.devyok.bluetooth.connection.BluetoothConnection.Protocol;
import com.devyok.bluetooth.utils.BluetoothUtils;
/**
 * @author wei.deng
 */
public final class BluetoothMessageDispatcher<DataType> implements BluetoothMessageHandler<DataType>{

	final ConcurrentHashMap<Protocol,List<BluetoothMessageReceiver<DataType>>> receivers = new ConcurrentHashMap<Protocol,List<BluetoothMessageReceiver<DataType>>>();
	
	static ExecutorService dispatcherThreadPool = Executors.newSingleThreadExecutor(BluetoothUtils.createThreadFactory("bt.runtime.message-dispatcher"));
	
	static BluetoothMessageHandler<?> instance;
	
	public static <DataType> BluetoothMessageHandler<DataType> getDispatcher(){
		if(instance == null){
			instance = new BluetoothMessageDispatcher<DataType>();
		}
		return (BluetoothMessageHandler<DataType>) instance;
	}
	
	public void registerBluetoothMessageReceiver(Protocol protocol,BluetoothMessageReceiver<DataType> receiver) {
		
		List<BluetoothMessageReceiver<DataType>> list = receivers.get(protocol);
		
		if(list == null){
			list = Collections.synchronizedList(new ArrayList<BluetoothMessageReceiver<DataType>>());
		}

		if(!list.contains(receiver)){
			list.add(receiver);
		}
		
		receivers.put(protocol, list);
	}
	
	public void unregisterBluetoothMessageReceiver(Protocol protocol,BluetoothMessageReceiver<DataType> receiver) {
		List<BluetoothMessageReceiver<DataType>> list = receivers.get(protocol);
		list.remove(receiver);
	}

	public void clear(Protocol protocol) {
		receivers.remove(protocol);
	}
	
	public void clearAll() {
		receivers.clear();
	}
	
	class HandleTask implements Runnable {
		
		BluetoothMessage<DataType> message;
		
		public HandleTask(BluetoothMessage<DataType> message){
			this.message = message;
		}
		
		@Override
		public void run() {
			
			List<BluetoothMessageReceiver<DataType>> list = receivers.get(message.getProtocol());
			
			if(list!=null){
				int size = list.size();
				
				for(int i = 0;i < size;i++){
					BluetoothMessageReceiver<DataType> receive = list.get(i);
					if(receive!=null){
						receive.onReceive(message);
					}
				}
			}
			
		}
	}
	
	public void handle(BluetoothMessage<DataType> message) {
		dispatcherThreadPool.submit(new HandleTask(message));
	}
	
	public static <DataType> void dispatch(BluetoothMessage<DataType> message){
		BluetoothMessageHandler<DataType> dispatcher =  OkBluetooth.getConfiguration().getDispatcher();
		dispatcher.handle(message);
	}
	
}
