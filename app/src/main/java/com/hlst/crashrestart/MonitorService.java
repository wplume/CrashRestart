package com.hlst.crashrestart;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.hlst.drivingtestkiosk.IMyAidlInterface;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.jaredrummler.android.processes.models.Stat;

import java.io.IOException;
import java.util.List;

import static com.hlst.crashrestart.Global.TAG;

public class MonitorService extends Service {

    private boolean restart;
    private String pkgName = "com.hlst.drivingtestkiosk";
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    startAnotherApp();
                    break;
                case 1:
                    startMonitor();
                    break;
            }
            return false;
        }
    });

    private void startAnotherApp() {
        String text = "监听者启动被监听程序：" + pkgName;
        Log.d(TAG, text);
        Toast.makeText(MonitorService.this, text, Toast.LENGTH_SHORT).show();
        Intent intent1 = getPackageManager().getLaunchIntentForPackage(pkgName);
        startActivity(intent1);
    }

    public MonitorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Global.TAG, "监听者服务启动");
        Toast.makeText(this, "监听者服务启动", Toast.LENGTH_SHORT).show();

        handler.sendMessageDelayed(Message.obtain(handler, 1), 10000);

//        demonPro();
    }

    private void startMonitor() {
        final Intent intent = new Intent();
        intent.setAction("com.hlst.drivingtestkiosk.AIDLService");
        intent.setPackage("com.hlst.drivingtestkiosk");
        ServiceConnection serviceConnection = new ServiceConnection() {

            private Thread thread;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                final IMyAidlInterface myAidlInterface = IMyAidlInterface.Stub.asInterface(service);
                thread = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        while (true) {
                            try {
                                sleep(5000);
                                Log.d(TAG, pkgName + myAidlInterface.getString() + getId());
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                Log.d(TAG, "中止循环检测线程 RemoteException");
                                break;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                Log.d(TAG, "中止循环检测线程 InterruptedException");
                                break;
                            }
                        }
                    }
                };
                thread.start();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "与 " + pkgName + " 服务断开连接");
                unbindService(this);
                if (thread != null) {
                    thread.interrupt();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                handler.sendMessageDelayed(Message.obtain(handler, 0), 5000);
                handler.sendMessageDelayed(Message.obtain(handler, 1), 6000);
            }
        };

        Log.d(TAG, "监听者绑定" + pkgName + "服务");
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

//        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
//        if (runningAppProcesses != null) {
//            for (final ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
//                if (processInfo.processName.equals(pkgName)) {
//                    bindService(intent, serviceConnection, BIND_AUTO_CREATE);
//                } else {
//                    startAnotherApp();
//                    bindService(intent, serviceConnection, BIND_AUTO_CREATE);
//                }
//            }
//        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void initService(Context context) {
        Intent i = new Intent(context, MonitorService.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(i);
    }

    private void demonPro() {
        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    while (!isInterrupted()) {
                        sleep(2500);

                        List<AndroidAppProcess> runningAppProcesses = AndroidProcesses.getRunningAppProcesses();

                        if (runningAppProcesses == null) {
                            return;
                        }

                        for (AndroidAppProcess process : runningAppProcesses) {
                            String name = process.name;

                            if (name.equals(pkgName)) {
                                Stat stat = process.stat();
                                char state = stat.state();

                                if (state == 'R' || state == 'S' || state == 'D') restart = false;
                                else restart = true;
                            }
                        }

                        if (restart) {
                            Intent intent = getPackageManager().getLaunchIntentForPackage(pkgName);
                            startActivity(intent);
                            sleep(10000);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
