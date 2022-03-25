package com.zoetek.app.myapplication;

import java.util.UUID;

public class Profile
{
	//宣告BLE資料傳送與接收之UUID
	public static final UUID UUID_SERVICE	= UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID	UUID_DATA_READ	= UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID	UUID_DATA_WRITE	= UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID	UUID_DESCRIPTOR_UPDATE_NOTIFICATION	= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
}
