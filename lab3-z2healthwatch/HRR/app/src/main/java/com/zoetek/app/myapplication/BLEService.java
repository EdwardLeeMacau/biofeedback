package com.zoetek.app.myapplication;

import android.app.IntentService;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class BLEService extends IntentService implements NotifyListener{


    public static BluetoothIO io;
    private Handler mHandler;

    boolean isQuit = false;


    public BLEService() {
        super("BLEService");
        mHandler = new Handler();
        io = new BluetoothIO();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public void connect(BluetoothDevice device){

        io.connect(this, new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                BroadcastData("Receiver","連線成功");
                setDataListener();
            }

            @Override
            public void onFail(int errorCode, String msg) {
                BroadcastData("Receiver","連線失敗");
            }
        }, device, mHandler);
    }

    public void setDataListener(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    if(io.notifyListenersRead())
                        io.notifyListenersClean();
                    io.setNotifyListener(Profile.UUID_DATA_WRITE, BLEService.this);
                }catch (final Exception e){
                    //BroadcastData("message", e.toString());
                }
            }
        }).start();
    }

    public void disconnect(){
        if(io!=null && io.getDevice() !=null)
            io.disconnect();
    }
    public boolean getConnectState(){
        return io.isConnected;
    }

    public void sendData(String msg){
        //依照Zoetek Protocol格式傳送資料
        //宣告20Byte資料變數
        byte[] data = new byte[2+msg.length()+1];
        //宣告標頭
        byte head = (byte)0x00;
        //宣告類型
        byte type = (byte)0x80;
        //加入標頭與類型
        data[0] = head;
        data[1] = type;
        //宣告檢查碼
        int checksum = 0;
        //計算檢查碼
        for(int i=0;i<msg.getBytes().length;i++){
            checksum = checksum + msg.getBytes()[i];
            data[i+2] = msg.getBytes()[i];
        }
        checksum = checksum & 0xFF;
        checksum = (checksum + (short)(type) & 0xFF) & 0xFF;
        //加入檢查碼
        data[2+msg.length()+1-1] = (byte)checksum;
        //發送資料
        io.writeCharacteristic(Profile.UUID_DATA_READ,data,null);
    }

    @Override
    public void onNotify(byte[] data) {
        //判斷是否有資料
        if(data.length>0) {
            //宣告檢查碼
            int checksum = 0;
            //計算檢查碼
            for (int i = 1; i < data.length - 1; i++) {
                checksum += data[i];
            }
            //判斷檢查碼是否正確
            if (Integer.toHexString(checksum & 0xFF).equals(Integer.toHexString(data[data.length - 1] & 0xFF))) {
                //宣告資料變數
                byte[] bledata = new byte[data.length - 3];
                //取出header部分
                byte head = data[0];
                Log.i("Header", String.valueOf(head & 0xFF));
                //取出資料部分位元組
                System.arraycopy(data, 2, bledata, 0, bledata.length);
                data = bledata;

                String text = null;
                try {
                    //將資料部分位元組轉為文字
                    text = new String(data, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                //去除空白字元
                text = text.trim();
                //發送資料給MainActivity
                BroadcastData("Receiver",text);
            }
        }
    }
    //發送廣播資料回MainActivity
    private void BroadcastData(String name,String msg){
        Intent intent = new Intent();
        intent.setAction(com.zoetek.app.myapplication.MainActivity.MY_BROADCAST_TAG);
        intent.putExtra(name, msg);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BLEService getService() {
            return  BLEService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.d("Tag" , "onUnbind");
        return true ;
    }

    @Override
    public void onDestroy(){
        this.isQuit = true ;
    }
}
