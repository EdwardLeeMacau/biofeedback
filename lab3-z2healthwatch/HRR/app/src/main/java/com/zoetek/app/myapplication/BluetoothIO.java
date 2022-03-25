package com.zoetek.app.myapplication;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

public class BluetoothIO extends BluetoothGattCallback
{
	private static final String TAG				= "BluetoothIO";
	BluetoothGatt gatt;
	ActionCallback					currentCallback;
	Context context;
	boolean isConnected;
	private Handler mHandler;
	BluetoothDevice device;
	private int error_count=0;
	
	HashMap<UUID, NotifyListener> notifyListeners	= new HashMap<UUID, NotifyListener>();
	
	public void connect(final Context context, final ActionCallback callback, BluetoothDevice device,Handler mHandler)
	{
		this.context = context;
		this.mHandler = mHandler;
		BluetoothIO.this.currentCallback = callback;
		this.device = device;
		this.device.connectGatt(context, false, BluetoothIO.this);
	}

	public void disconnect()
	{
		if(gatt != null) {
			//gatt.disconnect();
			gatt.close();
			mHandler.removeCallbacksAndMessages(null);
			isConnected=false;
		}
	}

	public BluetoothDevice getDevice()
	{
		if(null == gatt)
		{
			Log.e(TAG, "connect to device first");
			return null;
		}
		return gatt.getDevice();
	}
	
	public void writeAndRead(final UUID uuid, byte[] valueToWrite, final ActionCallback callback)
	{
		ActionCallback readCallback = new ActionCallback() {
			
			@Override
			public void onSuccess(Object characteristic)
			{
				BluetoothIO.this.readCharacteristic(uuid, callback);
			}
			
			@Override
			public void onFail(int errorCode, String msg)
			{
				callback.onFail(errorCode, msg);
			}
		};
		this.writeCharacteristic(uuid, valueToWrite, readCallback);
	}
	
	public void writeCharacteristic(UUID uuid, byte[] value, ActionCallback callback)
	{
		try
		{
			if(null == gatt)
			{
				Log.e(TAG, "connect to device first");
				throw new Exception("connect to device first");
			}
			this.currentCallback = callback;
			BluetoothGattCharacteristic chara = gatt.getService(Profile.UUID_SERVICE).getCharacteristic(uuid);
			if (null == chara)
			{
				this.onFail(-1, "BluetoothGattCharacteristic " + uuid + " is not exsit");
				return;
			}
			chara.setValue(value);
			if (false == this.gatt.writeCharacteristic(chara))
			{
				this.onFail(-1, "gatt.writeCharacteristic() return false");
			}
		} catch (Throwable tr)
		{
			Log.e(TAG, "writeCharacteristic", tr);
			this.onFail(-1, tr.getMessage());
		}
	}
	
	public void readCharacteristic(UUID uuid, ActionCallback callback)
	{
		try
		{
			if(null == gatt)
			{
				Log.e(TAG, "connect to device first");
				throw new Exception("connect to device first");
			}
			this.currentCallback = callback;
			BluetoothGattCharacteristic chara = gatt.getService(Profile.UUID_SERVICE).getCharacteristic(uuid);
			if (null == chara)
			{
				this.onFail(-1, "BluetoothGattCharacteristic " + uuid + " is not exsit");
				return;
			}
			if (false == this.gatt.readCharacteristic(chara))
			{
				this.onFail(-1, "gatt.readCharacteristic() return false");
			}
		} catch (Throwable tr)
		{
			Log.e(TAG, "readCharacteristic", tr);
			this.onFail(-1, tr.getMessage());
		}
	}
	
	public void readRssi(ActionCallback callback)
	{
		try
		{
			if(null == gatt)
			{
				Log.e(TAG, "connect to device first");
				throw new Exception("connect to device first");
			}
			this.currentCallback = callback;
			this.gatt.readRemoteRssi();
		} catch (Throwable tr)
		{
			Log.e(TAG, "readRssi", tr);
			this.onFail(-1, tr.getMessage());
		}
		
	}

	public void notifyListenersClean(){
		if (this.notifyListeners.containsKey(Profile.UUID_DATA_WRITE))
		{
			this.notifyListeners.clear();
		}
	}

	public boolean notifyListenersRead(){
		if (this.notifyListeners.containsKey(Profile.UUID_DATA_WRITE))
		{
			return true;
		}
		return false;
	}

	public void setNotifyListener(UUID characteristicId, NotifyListener listener)
	{
		if(null == gatt)
		{
			Log.e(TAG, "connect to device first");
			return;
		}
		if(this.notifyListeners.containsKey(characteristicId))
			return;
		
		BluetoothGattCharacteristic chara = gatt.getService(Profile.UUID_SERVICE).getCharacteristic(characteristicId);
		if (chara == null)
			return;
		
		this.gatt.setCharacteristicNotification(chara, true);
		BluetoothGattDescriptor descriptor = chara.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		this.gatt.writeDescriptor(descriptor);
		this.notifyListeners.put(characteristicId, listener);
	}
	
	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
	{
		super.onConnectionStateChange(gatt, status, newState);
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				isConnected = true;
				error_count=0;
				mHandler.removeCallbacksAndMessages(null);
				gatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				isConnected = false;
				if(status==0 ||status==8) {
					if (checkIsSamsung()) {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						gatt.close();
						gatt.connect();
					} else {
						connectGatt();
						mHandler.removeCallbacksAndMessages(null);
						mHandler.postDelayed(runnableReconnect, 30 * 1000);
					}
				}
				else {
					error_count++;
					if(error_count<=3) {
						this.device.connectGatt(context, false, BluetoothIO.this);
					}
					else
					{
						error_count=0;
					}
				}
				Log.d(TAG, "onConnectionStateChange received: " + status);
			}
	}
	
	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		super.onCharacteristicRead(gatt, characteristic, status);
		if (BluetoothGatt.GATT_SUCCESS == status)
		{
			this.onSuccess(characteristic);
		} else
		{
			this.onFail(status, "onCharacteristicRead fail");
		}
	}
	
	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		super.onCharacteristicWrite(gatt, characteristic, status);
		if (BluetoothGatt.GATT_SUCCESS == status)
		{
			this.onSuccess(characteristic);
		} else
		{
			this.onFail(status, "onCharacteristicWrite fail");
		}
	}
	
	@Override
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
	{
		super.onReadRemoteRssi(gatt, rssi, status);
		if (BluetoothGatt.GATT_SUCCESS == status)
		{
			this.onSuccess(rssi);
		} else
		{
			this.onFail(status, "onCharacteristicRead fail");
		}
	}
	
	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status)
	{
		super.onServicesDiscovered(gatt, status);
		if (status == BluetoothGatt.GATT_SUCCESS)
		{
			this.gatt = gatt;
			this.onSuccess(null);
		} else
		{
			this.onFail(status, "onServicesDiscovered fail");
		}
	}

	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		super.onCharacteristicChanged(gatt, characteristic);
		if (this.notifyListeners.containsKey(characteristic.getUuid()))
		{
			this.notifyListeners.get(characteristic.getUuid()).onNotify(characteristic.getValue());
		}
	}
	
	private void onSuccess(Object data)
	{
		if (this.currentCallback != null)
		{
			ActionCallback callback = this.currentCallback;
			this.currentCallback = null;
			callback.onSuccess(data);
		}
	}
	
	private void onFail(int errorCode, String msg)
	{
		if (this.currentCallback != null)
		{
			ActionCallback callback = this.currentCallback;
			this.currentCallback = null;
			callback.onFail(errorCode, msg);
		}
	}
	private boolean checkIsSamsung() {
		String brand = android.os.Build.BRAND;
		Log.e("", " brand:" + brand);
		if (brand.toLowerCase().equals("samsung")) {
			return true;
		}
		return false;
	}
	public void connectGatt() {
		if (gatt != null) {/*
			if (gatt.getDevice() != null) {
				gatt.close();
				gatt.disconnect();
				gatt = null;
			}
			this.device.connectGatt(context, false, this);
			gatt = this.device.connectGatt(context, false, this);*/
			gatt.connect();

		} else {
			Log.e("ble", "the bluetoothDevice is null, please reset the bluetoothDevice");
		}
	}
	Runnable runnableReconnect = new Runnable() {


		@Override
		public void run() {
			if (!isConnected) {
				mHandler.postDelayed(this, 30 * 1000);
				connectGatt();
			}
		}
	};
}
