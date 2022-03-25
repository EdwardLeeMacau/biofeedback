package tw.edu.ntu.z2;

import android.app.IntentService;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class BLEService extends IntentService implements NotifyListener {
    public static BluetoothIO io;
    private Handler mHandler;
    boolean isQuit = false;

    public BLEService(){
        super("BLEService");
        mHandler = new Handler();
        io = new BluetoothIO();
    }

    @Override
    protected void onHandleIntent(Intent intent){
        if (intent != null){
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    public void connect(BluetoothDevice device){
        io.connect(this, new ActionCallback(){
            @Override
            public void onSuccess(Object data){
                BroadcastData("Receiver", "Connect successful");
                setDataListener();
            }
            @Override
            public void onFail(int errorCode, String msg){
                BroadcastData("Receiver", "Connect Fail");
            }
        }, device, mHandler);
    }

    public void setDataListener(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Thread.sleep(2000);
                    if (io.notifyListenersRead())
                        io.notifyListenersClean();
                    io.setNotifyListener(Profile.UUID_DATA_WRITE, BLEService.this);
                } catch (final Exception e){

                }
            }
        }).start();
    }

    public void disconnect(){
        if (io != null && io.getDevice() != null)
            io.disconnect();
    }
    public boolean getConnectState(){
        return io.isConnected;
    }

    public void sendData(String msg){
        byte[] data = new byte[2 + msg.length() + 1];
        byte head = (byte)0x00;
        byte type = (byte)0x80;
        data[0] = head;
        data[1] = type;
        int checksum = 0;
        for(int i = 0; i < msg.getBytes().length; i++){
            checksum = checksum + msg.getBytes()[i];
            data[i + 2] = msg.getBytes()[i];
        }
        checksum = checksum & 0xFF;
        checksum = (checksum + (short)(type) & 0xFF) & 0xFF;

        data[2 + msg.length() + 1 - 1] = (byte)checksum;
        io.writeCharacteristic(Profile.UUID_DATA_READ, data, null);
    }

    @Override
    public void onNotify(byte[] data){
        if(data.length > 0){
            int checksum = 0;
            for (int i = 1; i < data.length - 1; i++){
                checksum += data[i];
            }
            if (Integer.toHexString(checksum & 0xFF).equals(Integer.toHexString(data[data.length - 1] & 0xFF))){
                byte[] bledata = new byte[data.length - 3];
                byte head = data[0];
                Log.i("Header", String.valueOf(head & 0xFF));
                System.arraycopy(data, 2, bledata, 0, bledata.length);
                data = bledata;

                String text = null;
                try{
                    text = new String(data, "UTF-8");
                } catch (UnsupportedEncodingException e){
                    e.printStackTrace();
                }

                text = text.trim();
                BroadcastData("Receiver", text);
            }
        }
    }

    private void BroadcastData(String name, String msg){
        Intent intent = new Intent();
        intent.setAction(tw.edu.ntu.z2.MainActivity.MY_BROADCAST_TAG);
        intent.putExtra(name, msg);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder{
        public BLEService getService(){
            return BLEService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent){
        Log.d("Tag", "onUnbind");
        return true;
    }
    @Override
    public void onDestroy(){
        this.isQuit = true;
    }

}
