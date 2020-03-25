package com.lan.cang.backupp2p.ui;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lan.cang.backupp2p.R;
import com.lan.cang.backupp2p.adapter.DeviceAdapter;
import com.lan.cang.backupp2p.broadcast.DirectBroadcastReceiver;
import com.lan.cang.backupp2p.callback.DirectActionListener;
import com.lan.cang.backupp2p.common.LoadingDialog;
import com.lan.cang.backupp2p.model.FileTransfer;
import com.lan.cang.backupp2p.task.WifiClientTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SendFileActivity extends BaseActivity {

    private static final String TAG = "SendFileActivity";

    private static final int CODE_CHOOSE_FILE = 100;

    private WifiP2pManager wifiP2pManager;

    private WifiP2pManager.Channel channel;

    private WifiP2pInfo wifiP2pInfo;

    private boolean wifiP2pEnabled = false;

    private DirectActionListener directActionListener = new DirectActionListener() {

        @Override
        public void wifiP2pEnabled(boolean enabled) {
            wifiP2pEnabled = enabled;
        }

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            dismissLoadingDialog();
            wifiP2pDeviceList.clear();
            deviceAdapter.notifyDataSetChanged();
            btn_disconnect.setEnabled(true);
            btn_chooseFile.setEnabled(true);
            Log.e(TAG, "onConnectionInfoAvailable");
            Log.e(TAG, "onConnectionInfoAvailable groupFormed: " + wifiP2pInfo.groupFormed);
            Log.e(TAG, "onConnectionInfoAvailable isGroupOwner: " + wifiP2pInfo.isGroupOwner);
            Log.e(TAG, "onConnectionInfoAvailable getHostAddress: " + wifiP2pInfo.groupOwnerAddress.getHostAddress());
            StringBuilder stringBuilder = new StringBuilder();
            if (mWifiP2pDevice != null) {
                stringBuilder.append("连接的设备名：");
                stringBuilder.append(mWifiP2pDevice.deviceName);
                stringBuilder.append("\n");
                stringBuilder.append("连接的设备的地址：");
                stringBuilder.append(mWifiP2pDevice.deviceAddress);
            }
            stringBuilder.append("\n");
            stringBuilder.append("是否群主：");
            stringBuilder.append(wifiP2pInfo.isGroupOwner ? "是群主" : "非群主");
            stringBuilder.append("\n");
            stringBuilder.append("群主IP地址：");
            stringBuilder.append(wifiP2pInfo.groupOwnerAddress.getHostAddress());
            tv_status.setText(stringBuilder);
            if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
                SendFileActivity.this.wifiP2pInfo = wifiP2pInfo;
            }
        }

        @Override
        public void onDisconnection() {
            Log.e(TAG, "onDisconnection");
            btn_disconnect.setEnabled(false);
            btn_chooseFile.setEnabled(false);
            showToast("处于非连接状态");
            wifiP2pDeviceList.clear();
            deviceAdapter.notifyDataSetChanged();
            tv_status.setText(null);
            SendFileActivity.this.wifiP2pInfo = null;
        }

        @Override
        public void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice) {
            Log.e(TAG, "onSelfDeviceAvailable");
            Log.e(TAG, "DeviceName: " + wifiP2pDevice.deviceName);
            Log.e(TAG, "DeviceAddress: " + wifiP2pDevice.deviceAddress);
            Log.e(TAG, "Status: " + wifiP2pDevice.status);
            tv_myDeviceName.setText(wifiP2pDevice.deviceName);
            tv_myDeviceAddress.setText(wifiP2pDevice.deviceAddress);
            tv_myDeviceStatus.setText(MainActivity.getDeviceStatus(wifiP2pDevice.status));
        }

        @Override
        public void onPeersAvailable(Collection<WifiP2pDevice> wifiP2pDeviceList) {
            Log.e(TAG, "onPeersAvailable :" + wifiP2pDeviceList.size());
            SendFileActivity.this.wifiP2pDeviceList.clear();
            SendFileActivity.this.wifiP2pDeviceList.addAll(wifiP2pDeviceList);
            deviceAdapter.notifyDataSetChanged();
            loadingDialog.cancel();
        }

        @Override
        public void onChannelDisconnected() {
            Log.e(TAG, "onChannelDisconnected");
        }

    };

    private TextView tv_myDeviceName;

    private TextView tv_myDeviceAddress;

    private TextView tv_myDeviceStatus;

    private TextView tv_status;

    private List<WifiP2pDevice> wifiP2pDeviceList;

    private DeviceAdapter deviceAdapter;

    private Button btn_search;
    private Button btn_open_wifi;
    private Button btn_disconnect;
    private Button btn_chooseFile;

    private LoadingDialog loadingDialog;

    private BroadcastReceiver broadcastReceiver;

    private WifiP2pDevice mWifiP2pDevice;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_search: {
                    searchNewDevices();
                    break;
                }
                case R.id.btn_open_wifi: {
                    openWifi();
                    break;
                }
                case R.id.btn_disconnect: {
                    disconnect();
                    break;
                }
                case R.id.btn_chooseFile: {
//                    chooseFile();
                    sendFile();
                    break;
                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        initView();
        initEvent();
    }

    private void initEvent() {
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {
            finish();
            return;
        }
        channel = wifiP2pManager.initialize(this, getMainLooper(), directActionListener);
        broadcastReceiver = new DirectBroadcastReceiver(wifiP2pManager, channel, directActionListener);
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter());
    }

    private void initView() {
        setTitle(getString(R.string.device_old));
        tv_myDeviceName = findViewById(R.id.tv_myDeviceName);
        tv_myDeviceAddress = findViewById(R.id.tv_myDeviceAddress);
        tv_myDeviceStatus = findViewById(R.id.tv_myDeviceStatus);
        tv_status = findViewById(R.id.tv_status);
        btn_search = findViewById(R.id.btn_search);
        btn_open_wifi = findViewById(R.id.btn_open_wifi);
        btn_disconnect = findViewById(R.id.btn_disconnect);
        btn_chooseFile = findViewById(R.id.btn_chooseFile);
        btn_search.setOnClickListener(clickListener);
        btn_open_wifi.setOnClickListener(clickListener);
        btn_disconnect.setOnClickListener(clickListener);
        btn_chooseFile.setOnClickListener(clickListener);
        loadingDialog = new LoadingDialog(this);
        RecyclerView rv_deviceList = findViewById(R.id.rv_deviceList);
        wifiP2pDeviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(wifiP2pDeviceList);
        deviceAdapter.setClickListener(new DeviceAdapter.OnClickListener() {
            @Override
            public void onItemClick(int position) {
                mWifiP2pDevice = wifiP2pDeviceList.get(position);
                showToast(mWifiP2pDevice.deviceName);
                connect();
            }
        });
        rv_deviceList.setAdapter(deviceAdapter);
        rv_deviceList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_CHOOSE_FILE && resultCode == RESULT_OK) {

//            if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
//                File file = new File("/sdcard/backup.zip");
//                if (file.exists()) {
//                    Log.d("#####", "name:" + file.getName());
//                } else {
//                    Log.d("#####", "file no exists:");
//                }
//                if (file.exists() && wifiP2pInfo != null) {
//                    FileTransfer fileTransfer = new FileTransfer(file.getPath(), file.length());
//                    new WifiClientTask(this, fileTransfer).execute(wifiP2pInfo.groupOwnerAddress.getHostAddress());
//                }
//            }
        }
    }

    private void connect() {
        WifiP2pConfig config = new WifiP2pConfig();
        if (config.deviceAddress != null && mWifiP2pDevice != null) {
            config.deviceAddress = mWifiP2pDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            showLoadingDialog("正在连接 " + mWifiP2pDevice.deviceName);
            wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "connect onSuccess");
                }

                @Override
                public void onFailure(int reason) {
                    showToast("连接失败 " + reason);
                    dismissLoadingDialog();
                }
            });
        }
    }

    private void disconnect() {
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "disconnect onFailure:" + reasonCode);
            }

            @Override
            public void onSuccess() {
                Log.e(TAG, "disconnect onSuccess");
                tv_status.setText(null);
                btn_disconnect.setEnabled(false);
                btn_chooseFile.setEnabled(false);
            }
        });
    }

    private void openWifi() {
        if (wifiP2pManager != null && channel != null) {
            startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
        } else {
            showToast("当前设备不支持Wifi Direct");
        }
    }

    private void searchNewDevices() {
        if (!wifiP2pEnabled) {
            showToast("需要先打开Wifi");
            return;
        }
        loadingDialog.show("正在搜索附近设备", true, false);
        wifiP2pDeviceList.clear();
        deviceAdapter.notifyDataSetChanged();
        //搜寻附近带有 Wi-Fi P2P 的设备
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                showToast("Success");
            }

            @Override
            public void onFailure(int reasonCode) {
                showToast("Failure");
                loadingDialog.cancel();
            }
        });
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.action, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menuDirectEnable: {
//                openWifi();
//                return true;
//            }
//            case R.id.menuDirectDiscover: {
//                searchNewDevices();
//                return true;
//            }
//            default:
//                return true;
//        }
//    }

    /**
     * 客户端进行选择文件
     */
    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 10);
    }

    private void sendFile() {
        if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            File file = new File("/sdcard/backup.zip");
            if (file.exists()) {
                Log.d("#####", "name:" + file.getName());
            } else {
                Log.d("#####", "file no exists:");
            }
            if (file.exists() && wifiP2pInfo != null) {
                FileTransfer fileTransfer = new FileTransfer(file.getPath(), file.length());
                new WifiClientTask(this, fileTransfer).execute(wifiP2pInfo.groupOwnerAddress.getHostAddress());
            }
        }
    }
}