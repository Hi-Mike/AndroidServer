package cn.me.mike.androidserver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import cn.me.mike.androidserver.utils.NetUtil;

public class MainActivity extends AppCompatActivity {
    TextView text = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.text);

        String ip = NetUtil.getWIFILocalIpAdress(this);
        if (ip != null && !ip.equals("0.0.0.0")) {
            text.setText("访问路径====>" + ip + ":" + Constant.Config.PORT);
            startService(new Intent(this, WebService.class));
        } else {
            text.setText("请检查WIFI是否连接。。。");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, WebService.class));
    }
}