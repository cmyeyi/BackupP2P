package com.lan.cang.backupp2p.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.lan.cang.backupp2p.common.LoadingDialog;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    private LoadingDialog loadingDialog;

    protected void setTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    protected void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(this);
        }
        loadingDialog.show(message, true, false);
    }

    protected void dismissLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    protected <T extends Activity> void startActivity(Class<T> tClass) {
        startActivity(new Intent(this, tClass));
    }

    protected <T extends Service> void startService(Class<T> tClass) {
        startService(new Intent(this, tClass));
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
