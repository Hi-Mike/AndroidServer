package cn.me.mike.androidserver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import net.glxn.qrgen.android.QRCode;

import cn.me.mike.androidserver.utils.NetUtil;

import static com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q;

public class MainActivity extends AppCompatActivity {
    TextView text = null;
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.text);
        image = (ImageView) findViewById(R.id.image);

        setView();
    }

    private void setView() {
        String ip = NetUtil.getWIFILocalIpAdress(this);
        if (ip != null && !ip.equals("0.0.0.0")) {
            text.setText("访问路径====" + ip + ":" + Constant.Config.PORT + "\n或扫描二维码用浏览器打开");
            image.setImageBitmap(QRCode.from("http://" + ip + ":" + Constant.Config.PORT).bitmap());
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