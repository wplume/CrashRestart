package com.hlst.crashrestart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d("CrashHandler", "CrashHandler接收到开机广播");
            MonitorService.initService(context);
        }
    }
}
