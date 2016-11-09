package cn.me.mike.androidserver.http;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import cn.me.mike.androidserver.http.FileHandler;
import okio.BufferedSource;
import okio.Okio;

/**
 * Created by ske on 2016/11/7.
 */

public class WorkThread extends Thread {
    private Socket socket;

    public WorkThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = socket.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//                Logger.d(reader.readLine());
            BufferedSource source = Okio.buffer(Okio.source(inputStream));
            String filePath = parseRequest(source);
            Logger.d("为什么会无缘无故调用。。。");

            FileHandler fileHandler = new FileHandler(socket, filePath, isWeChat(source));
            fileHandler.handle();
        } catch (IOException e) {
            e.printStackTrace();
            interrupt();
        } finally {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //获取请求路径
    private String parseRequest(BufferedSource source) {
        try {
            String path = source.readUtf8Line();
            if (path == null)
                return "";
            String token[] = path.split("\\s+");
            return token[1];
        } catch (IOException e) {
            e.printStackTrace();
            interrupt();
        }
        return "";
    }

    private boolean isWeChat(BufferedSource source) {
        String match;
        try {
            while ((match = source.readUtf8Line()) != null) {
                if (match.contains("Mobile MQQBrowser") || match.contains("MicroMessenger")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
