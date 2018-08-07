package com.devyok.bluetooth.connection;

import android.bluetooth.BluetoothDevice;

import com.devyok.bluetooth.base.BluetoothException;

import java.util.UUID;


/**
 * 重连策略
 * 默认实现：{@link DefaultRetryPolicy}
 * @author wei.deng
 */
public interface RetryPolicy {
    public void reset();
    public int getCurrentRetryCount();
    public void retry(UUID sppuuid,BluetoothDevice connectedDevice,Exception e) throws BluetoothConnectionException,BluetoothException;
}
