package cn.me.mike.androidserver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import cn.me.mike.androidserver.http.ServerThread;

/**
 * Created by ske on 2016/11/7.
 */

public class WebService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    ServerThread thread;

    @Override
    public void onCreate() {
        super.onCreate();

        thread = new ServerThread(Constant.Config.PORT, Constant.Config.Web_Root);
        thread.setDaemon(false);
        thread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        new Thread() {
            public void run() {
                if (thread != null)
                    thread.destroy();
            }
        }.start();
    }
}
