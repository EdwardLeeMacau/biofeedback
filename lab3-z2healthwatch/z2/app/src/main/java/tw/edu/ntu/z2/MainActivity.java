package tw.edu.ntu.z2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private ListView listBLEDevice;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ProgressDialog pd;
    private TextView tv_scan;
    private TextView measureResult;
    private AlertDialog showList;
    private String ble_device_name = "";
    private String ble_device_address = "";
    private Button btn_connect;
    private Button btn_send, btn_send2, btn_send3;
    private View scanView;

    private BLEService bleService;
    public static final String MY_BROADCAST_TAG = "tw.edu.ntu";
    private MyReceiver receiver;
    private IntentFilter filter;

    // State Variable. Mark whether the experiment is running.
    private int experiment = 0;

    // Exp 5
    private int E_HR, E_Stress, E_NN50;
    private double E_SDNN, E_LF_HF, E_RMSSD, E_VLF, E_LF, E_HF, MSG;

    // Exp 6
    // (REUSE) private int E_HR
    private int SBP, DBP;
    private double PTT, ET, SLP;

    // Exp 10
    private double HRR = 0.0, VO2 = 0.0;
    private int HR = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //檢查是否支援BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "Device doesn't support BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        //檢查是否支援藍牙
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        measureResult = (TextView)findViewById(R.id.measureResult);
        scanView = getLayoutInflater().inflate(R.layout.list_device, null);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mHandler = new Handler();
        listBLEDevice = (ListView) scanView.findViewById(R.id.listBLEDevice);
        listBLEDevice.setAdapter(mLeDeviceListAdapter);
        listBLEDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mHandler.removeCallbacksAndMessages(null);
                if(mScanning)
                    scanLeDevice(false);
                showList.cancel();
                pd = ProgressDialog.show(MainActivity.this, "", "Device connecting");
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);

                ble_device_name = device.getName();
                ble_device_address = device.getAddress();

                Toast.makeText(MainActivity.this, device.getName() + " " + device.getAddress(), Toast.LENGTH_LONG).show();
                pd.dismiss();
                bleService.connect(device);
            }
        });

        tv_scan = (TextView) scanView.findViewById(R.id.tv_scan);
        tv_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView) v;
                if (tv.getText().toString().equals("Re-scanning Object")) {
                    tv_scan.setText("Stop scanning");
                    mLeDeviceListAdapter.clear();
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    scanLeDevice(true);
                } else {
                    tv_scan.setText("Re-scanning Object");
                    scanLeDevice(false);
                }
            }
        });

        showList = new AlertDialog.Builder(this).setTitle("Please choose BLE Device").setView(scanView).create();
        showList.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mHandler.removeCallbacksAndMessages(null);
                if (mScanning)
                    scanLeDevice(false);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                    }
                },3000);
            }
        });

        btn_connect = (Button)findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                tv_scan.setText("Stop Scanning");
                showList.show();
            }
        });

        btn_send = (Button)findViewById(R.id.btn_send);
        btn_send.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (btn_send.getText().toString().equals("Send")){
                    bleService.sendData("menu.hrv,");
                    // bleService.sendData("ppg_on");
                    btn_send.setText("Measure");
                }
                else{
                    if (btn_send.getText().toString().equals("Stop Measuring")){
                        bleService.sendData("menu.hrv,");
                        btn_send.setText("Measure");
                        experiment = 0;
                    }
                    else{
                        bleService.sendData("icon.hrv,");
                        // bleService.sendData("ppg_on");
                        btn_send.setText("Stop Measuring");
                        experiment = 5;
                    }
                }
            }
        });

        btn_send2 = (Button)findViewById(R.id.btn_send2);
        btn_send2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if (btn_send2.getText().toString().equals("Send")){
                    bleService.sendData("menu.bp,");
                    btn_send2.setText("Measure");
                } else{
                    if(btn_send2.getText().toString().equals("Stop Measuring")){
                        bleService.sendData("menu.bp,");
                        btn_send2.setText("Measure");
                        experiment = 0;
                    } else {
                        bleService.sendData("icon.bp,");
                        btn_send2.setText("Stop Measuring");
                        experiment = 6;
                    }
                }
            }
        });

        btn_send3 = (Button)findViewById(R.id.btn_send3);
        btn_send3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (btn_send3.getText().toString().equals("Send")){
                    bleService.sendData("menu.hrr,");
                    btn_send3.setText("Measure");
                } else {
                    if (btn_send3.getText().toString().equals("Stop Measuring")){
                        bleService.sendData("menu.hrr,");
                        btn_send3.setText("Measure");
                        experiment = 0;
                    } else {
                        bleService.sendData("icon.hrr,");
                        btn_send3.setText("Stop Measuring");
                        experiment = 10;
                    }
                }
            }
        });

        doBindService();
        receiver = new MyReceiver();
        filter = new IntentFilter();
        filter.addAction(MY_BROADCAST_TAG);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()){
            if (!mBluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(mLeDeviceListAdapter!=null){
            scanLeDevice(false);
            mLeDeviceListAdapter.clear();
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        bleService.disconnect();
        unbindService(connc);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED){
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable){
        if(enable){
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    tv_scan.setText("Re-scanning Device");
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            tv_scan.setText("Re-scanning Device");
        }
    }

    private class LeDeviceListAdapter extends BaseAdapter{
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public  LeDeviceListAdapter(){
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device){
            if(!mLeDevices.contains(device)){
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position){
            return mLeDevices.get(position);
        }

        public void clear(){
            mLeDevices.clear();
        }

        @Override
        public int getCount(){
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i){
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i){
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup){
            ViewHolder viewHolder;
            if (view == null){
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown device");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mLeDeviceListAdapter.getCount() == 0){
                        mLeDeviceListAdapter.addDevice(device);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                    else{
                        boolean find = false;
                        for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++){
                            if (mLeDeviceListAdapter.getDevice(i).equals(device)){
                                find = true;
                                break;
                            }
                        }
                        if (find == false){
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });
        }
    };

    private void doBindService(){
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, connc, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection connc = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name){
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            bleService = ((BLEService.LocalBinder)service).getService();
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    class MyReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context arg0, Intent arg1){
            if(arg1.getStringExtra("Receiver") != null){
                // Log the package
                Log.d("Arg1", arg1.getStringExtra("Receiver"));
                switch (experiment){
                    case 5:
                        if (arg1.getStringExtra("Receiver").equals("ERR=3020,")){
                        } else if (arg1.getStringExtra("Receiver").equals("menu.hrv")){
                            switch (experiment){
                                case 5:
                                    btn_send.setText("Measure");
                                    // experiment = 0;
                                    break;
                                case 6:
                                    btn_send2.setText("Measure");
                                    // experiment = 0;
                                    break;
                                case 10:
                                    btn_send3.setText("Measure");
                                    // experiment = 0;
                                    break;
                            }
                        } else {
                            switch (arg1.getStringExtra("Receiver").split("=")[0]){
                                case "E_HR":
                                    //以"="和","文字切割，將ECG HR資料部分取出並轉為Integer
                                    E_HR = Integer.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                    break;
                                case "E_SD":
                                    //以"="和","文字切割，將ECG SDNN資料部分取出並轉為Double
                                    E_SDNN = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                    break;
                                case "E_VLF":
                                    E_VLF = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                    break;
                                case "E_LF":
                                    E_LF = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                    break;
                                case "E_HF":
                                    E_HF = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                    break;
                                case "MSG":
                                    MSG = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                    break;
                                case "E_Stress":
                                    //以"="和","文字切割，將ECG Stress資料部分取出並轉為Integer
                                    E_Stress = Integer.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                    break;
                                case "E_RMSSD":
                                    E_RMSSD = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                    break;
                                case "E_NN50":
                                    E_NN50 = Integer.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                    break;
                                default:
                                    break;
                            }
                            if (E_HF != 0) {
                                E_LF_HF = E_LF / E_HF;
                            }
                            String outputString =   "E_HR = " + String.valueOf(E_HR) + "\n" +
                                                    "E_SDNN = " + String.valueOf(E_SDNN) + "\n" +
                                                    "E_RMSSD = " + String.valueOf(E_RMSSD) + "\n" +
                                                    "E_NN50 = " + String.valueOf(E_NN50) + "\n" +
                                                    "E_VLF = " + String.valueOf(E_VLF) + "\n" +
                                                    "E_LF = " + String.valueOf(E_LF) + "\n" +
                                                    "E_HF = " + String.valueOf(E_HF) + "\n" +
                                                    "E_LF/HF = " + String.valueOf(E_LF_HF);

                            measureResult.setText(outputString);
                        }
                        break;
                    case 6:
                        switch (arg1.getStringExtra("Receiver").split("=")[0]){
                            case "E_HR":
                                //以"="和","文字切割，將ECG HR資料部分取出並轉為Integer
                                E_HR = Integer.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                            case "PTT":
                                //以"="和","文字切割，將PTT資料部分取出並轉為Double
                                PTT = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                            case "ET":
                                //以"="和","文字切割，將ET資料部分取出並轉為Double
                                ET = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                            case "SLP":
                                //以"="和","文字切割，將SLP資料部分取出並轉為Double
                                SLP = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                            case "SYS":
                                SBP = Integer.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                            case "DIA":
                                DBP = Integer.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                        }
                        String outputString2 =  "E_HR = " + String.valueOf(E_HR) + "\n"+
                                                "PTT = " + String.valueOf(PTT) + "\n"+
                                                "ET = " + String.valueOf(ET) + "\n"+
                                                "SLP = " + String.valueOf(SLP) + "\n" +
                                                "SBP/SYS = " + String.valueOf(SBP) + "\n" +
                                                "DBP/DIA = " + String.valueOf(DBP);

                        measureResult.setText(outputString2);
                        break;
                    case 10:
                        switch (arg1.getStringExtra("Receiver").split("=")[0]){
                            case "E_HR":
                                HR = Integer.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                            case "P_HR":
                                HR = Integer.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                            case "VO2":
                                VO2 = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                            case "HRR":
                                HRR = Double.valueOf(arg1.getStringExtra("Receiver").split("=|,")[1]);
                                break;
                        }
                        String outputString =   "HR  = " + String.valueOf(HR)  + "\n" +
                                                "HRR = " + String.valueOf(HRR) + "\n" +
                                                "VO2 = " + String.valueOf(VO2);
                        measureResult.setText(outputString);
                        break;
                    default:
                        break;
                }
            }
        }
    }

}
