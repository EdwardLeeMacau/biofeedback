package com.zoetek.app.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //關閉掃描時間處理
    private Handler mHandler;
    //掃描間隔
    private static final long SCAN_PERIOD = 10000;
    //要求開啟藍牙
    private static final int REQUEST_ENABLE_BT = 1;
    //藍牙接口
    private BluetoothAdapter mBluetoothAdapter;
    //用來是否在掃描裝置
    private boolean mScanning;
    //藍牙裝置列表顯示
    private ListView listBLEDevice;
    //BLE列表接口
    private LeDeviceListAdapter mLeDeviceListAdapter;
    //用來顯示處理進度
    private ProgressDialog pd;
    //掃描狀態文字顯示
    private TextView tv_scan;
    //藍牙掃描彈出視窗
    private AlertDialog showList;
    //已選擇的裝置名稱
    private String ble_device_name = "";
    //已選擇的裝置MAC位址
    private String ble_device_address = "";
    //開始掃描按鈕
    private Button btn_connect;
    //藍牙掃描視窗
    private View scanView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //檢查是否支援BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "裝置不支援BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        //檢查是否支援藍牙
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "裝置不支援藍牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //取得BLE掃描視窗Layout
        scanView = getLayoutInflater().inflate(R.layout.list_device, null);
        //BLE列表接口初始化
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mHandler = new Handler();
        //BLE列表初始化
        listBLEDevice = (ListView) scanView.findViewById(R.id.listBLEDevice);
        //將BLE列表連結到BLE列表接口
        listBLEDevice.setAdapter(mLeDeviceListAdapter);
        //設定BLE列表點擊後要處理的工作
        listBLEDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //判斷使否還在掃描，如果還在掃描則取消掃描
                mHandler.removeCallbacksAndMessages(null);
                if(mScanning)
                    scanLeDevice(false);
                //取消列表顯示
                showList.cancel();
                //顯示處理狀態
                pd = ProgressDialog.show(MainActivity.this, "", "裝置連線中");
                //取得已選擇裝置的相關資訊
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);

                //儲存選擇的裝置名稱與MAC位址
                ble_device_name = device.getName();
                ble_device_address = device.getAddress();

                //顯示選擇的裝置
                Toast.makeText(MainActivity.this,device.getName() + " " + device.getAddress(),Toast.LENGTH_LONG).show();

                //關閉顯示處理狀態
                pd.dismiss();
            }
        });
        //取得掃描狀態TextView的Layout
        tv_scan = (TextView) scanView.findViewById(R.id.tv_scan);
        //設定點擊後要處理的工作
        tv_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = (TextView)v;
                if(tv.getText().toString().equals("重新掃描裝置")){
                    tv_scan.setText("中止掃描");
                    //清除BLE裝置列表
                    mLeDeviceListAdapter.clear();
                    //更新BLE列表顯示
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    //掃描BLE裝置
                    scanLeDevice(true);
                }
                else {
                    tv_scan.setText("重新掃描裝置");
                    //中止掃描BLE裝置
                    scanLeDevice(false);
                }
            }
        });
        //初始化BLE列表顯示Layout
        showList = new AlertDialog.Builder(this).setTitle("請選擇BLE裝置").setView(scanView).create();
        //設定關閉BLE掃描視窗後的工作
        showList.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                //判斷使否還在掃描，如果還在掃描則取消掃描
                mHandler.removeCallbacksAndMessages(null);
                if(mScanning)
                    scanLeDevice(false);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                    }
                },3000);
            }
        });

        //初始化連線按鈕
        btn_connect = (Button)findViewById(R.id.btn_connect);
        //設定點擊按鈕後的工作
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //清除BLE列表
                mLeDeviceListAdapter.clear();
                //開始BLE掃描
                scanLeDevice(true);
                //設定BLE掃描狀態
                tv_scan.setText("中止掃描");
                //顯示BLE掃描視窗
                showList.show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //詢問是否開啟藍牙功能
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(mLeDeviceListAdapter!=null) {
            scanLeDevice(false);
            mLeDeviceListAdapter.clear();
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //判斷是否選擇開啟藍牙，選擇否則關閉程式
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    //掃描BLE方法
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    tv_scan.setText("重新掃描裝置");
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            tv_scan.setText("重新掃描裝置");
        }
    }
    //BLE裝置的接口
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
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
                viewHolder.deviceName.setText("未知的裝置");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    //掃描到BLE裝置後的回傳方法
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mLeDeviceListAdapter.getCount()==0) {
                                mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                            else {
                                boolean find = false;
                                for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                                    if (mLeDeviceListAdapter.getDevice(i).equals(device)) {
                                        find = true;
                                        break;
                                    }
                                }
                                if(find == false){
                                    mLeDeviceListAdapter.addDevice(device);
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
