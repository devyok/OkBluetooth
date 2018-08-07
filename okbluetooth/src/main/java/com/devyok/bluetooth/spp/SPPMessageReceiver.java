package com.devyok.bluetooth.spp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.devyok.bluetooth.connection.BluetoothConnection.Protocol;
import com.devyok.bluetooth.message.BluetoothMessage;
import com.devyok.bluetooth.message.BluetoothMessageDispatcher;
import com.devyok.bluetooth.utils.BluetoothUtils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
/**
 * @author wei.deng
 */
public class SPPMessageReceiver extends Thread{

    private static final String TAG = SPPMessageReceiver.class.getSimpleName();
    
    private InputStream mInStream;
    private OutputStream mOutStream;
    private volatile boolean isStopped = false;
    private SPPBluetoothMessageParser<?> messageParser;
    private BluetoothDevice bluetoothDevice;

    public SPPMessageReceiver(BluetoothSocket socket,SPPBluetoothMessageParser<?> parser,BluetoothDevice bluetoothDevice) {
        Log.i(TAG, "create SPPMessageReader");

        this.messageParser = parser;
        this.bluetoothDevice = bluetoothDevice;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
        }

        mInStream = tmpIn;
        mOutStream = tmpOut;
    }
    
    public void run() {
        Log.i(TAG, "SPPMessageReceiver run");
        byte[] buffer = new byte[512];
        int bytes;
        
        while (!isStopped()) {
            try {
                bytes = mInStream.read(buffer);
                
                BluetoothMessage<?>[] messages = messageParser.parse(buffer, bytes,Protocol.SPP,this.bluetoothDevice);
                
                if(messages!=null){
                	
                	for (int i = 0; i < messages.length; i++) {
                		BluetoothMessage<?> message = messages[i];
                		message.setBluetoothDevice(this.bluetoothDevice);
                		BluetoothMessageDispatcher.dispatch(message);
					}
                	
                }
                
            } catch (IOException e) {
                Log.e(TAG, "disconnected", e);
                closeStream();
                break;
            }
        }
        Log.i(TAG, "SPPMessageReceiver run completed");
    }
    
    public void startReceiver() {
    	isStopped = false;
    	super.start();
    }
    
    public void stopReceiver(){
    	isStopped = true;
    	closeStream();
    }
    
    public boolean isStopped(){
    	return isStopped;
    }
    
	public void closeStream() {
		try {
			BluetoothUtils.close(mInStream);
			BluetoothUtils.close(mOutStream);
		} finally {
			mInStream = null;
			mOutStream = null;
		}
		
	}
	
}
