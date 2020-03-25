package com.lan.cang.backupp2p.ui;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.lan.cang.backupp2p.R;
import com.lan.cang.backupp2p.broadcast.DirectBroadcastReceiver;
import com.lan.cang.backupp2p.callback.DirectActionListener;
import com.lan.cang.backupp2p.model.FileTransfer;
import com.lan.cang.backupp2p.service.WifiServerService;

import java.io.File;
import java.util.Collection;


public class ReceiveFileActivity extends BaseActivity {

    private TextView pathView;
    private TextView logView;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private boolean connectionInfoAvailable;
    private DirectActionListener directActionListener = new DirectActionListener() {
        @Override
        public void wifiP2pEnabled(boolean enabled) {
            Log.d("#####", "wifiP2pEnabled:" + enabled);
        }

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.d("#####", "isGroupOwner：" + wifiP2pInfo.isGroupOwner);
            Log.d("#####", "groupFormed：" + wifiP2pInfo.groupFormed);
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                connectionInfoAvailable = true;
                hideCreateView();
                showRemoveView();
                log("create group success,");
                log("waiting connect...");
                if (wifiServerService != null) {
                    startService(WifiServerService.class);
                }
            }
        }

        @Override
        public void onDisconnection() {
            Log.d("#####", "onDisconnection");
            connectionInfoAvailable = false;
            showCreateView();
        }

        @Override
        public void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice) {
            Log.d("#####", "onSelfDeviceAvailable");
            Log.d("#####", "wifiP2pDevice:"+wifiP2pDevice.toString());
        }

        @Override
        public void onPeersAvailable(Collection<WifiP2pDevice> wifiP2pDeviceList) {
            Log.d("#####", "onPeersAvailable,size:" + wifiP2pDeviceList.size());
            for (WifiP2pDevice wifiP2pDevice : wifiP2pDeviceList) {
                Log.d("#####", wifiP2pDevice.toString());
            }
        }

        @Override
        public void onChannelDisconnected() {
            Log.d("#####", "onChannelDisconnected");
        }
    };
    private BroadcastReceiver broadcastReceiver;
    private WifiServerService wifiServerService;
    private ProgressDialog progressDialog;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WifiServerService.MyBinder binder = (WifiServerService.MyBinder) service;
            wifiServerService = binder.getService();
            wifiServerService.setProgressChangListener(progressChangListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiServerService = null;
            bindService();
        }
    };

    private WifiServerService.OnProgressChangListener progressChangListener = new WifiServerService.OnProgressChangListener() {
        @Override
        public void onProgressChanged(final FileTransfer fileTransfer, final int progress) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("文件名： " + new File(fileTransfer.getFilePath()).getName());
                    progressDialog.setProgress(progress);
                    progressDialog.show();
                }
            });
        }

        @Override
        public void onTransferFinished(final File file) {
            Log.d("#####", "接收到文件，文件路径：" + file.getPath());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.cancel();
                    if (file != null && file.exists()) {
                        log("接收文件保存路径："+file.getPath());
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_file);
        initView();
        initWIFIP2P();
        bindService();
    }

    private void initWIFIP2P() {
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
        setTitle(getString(R.string.device_new));
        pathView = findViewById(R.id.path_tv);
        logView = findViewById(R.id.tv_log);
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle(getString(R.string.receive_ing));
        progressDialog.setMax(100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        createGroup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiServerService != null) {
            wifiServerService.setProgressChangListener(null);
            unbindService(serviceConnection);
        }
        unregisterReceiver(broadcastReceiver);
        stopService(new Intent(this, WifiServerService.class));
        if (connectionInfoAvailable) {
            removeGroup();
        }
    }

    public void createGroup(View view) {
        createGroup();
    }

    public void createGroup() {
        if(connectionInfoAvailable) {
            return;
        }
        wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("#####","create group success,waiting change...");
                showRemoveView();
                dismissLoadingDialog();
                showToast("onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("#####","createGroup onFailure: " + reason);
                showCreateView();
                dismissLoadingDialog();
                showToast("onFailure");
            }
        });
    }

    private void showCreateView() {
        findViewById(R.id.btn_createGroup).setVisibility(View.VISIBLE);
    }

    private void hideCreateView() {
        findViewById(R.id.btn_createGroup).setVisibility(View.GONE);
    }

    private void showRemoveView() {
        findViewById(R.id.btn_removeGroup).setVisibility(View.VISIBLE);
    }

    private void hideRemoveView() {
        findViewById(R.id.btn_removeGroup).setVisibility(View.GONE);
    }

    public void removeGroup(View view) {
        removeGroup();
    }

    private void removeGroup() {
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("#####","removeGroup onSuccess");
                showToast("remove group success");
                connectionInfoAvailable =  false;
            }

            @Override
            public void onFailure(int reason) {
                Log.d("#####","removeGroup onFailure");
                showToast("remove group failure");
            }
        });
    }

    private void log(String log) {
        logView.append(log + "\n");
    }

    private void bindService() {
        Intent intent = new Intent(ReceiveFileActivity.this, WifiServerService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

}