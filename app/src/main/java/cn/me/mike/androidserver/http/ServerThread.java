package cn.me.mike.androidserver.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ske on 2016/11/7.
 */

public class ServerThread extends Thread {
    private int port;
    private String webRoot;

    private ServerSocket serversocket;
    private ExecutorService executorService;

    public ServerThread(int port, String webRoot) {
        this.port = port;
        this.webRoot = webRoot;

        executorService = Executors.newCachedThreadPool();//创建一个缓存线程池
    }

    @Override
    public void run() {
        initServer();

        while (!this.isInterrupted()) {
            try {
                Socket socket = serversocket.accept();//3.等待接收数据
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(5 * 1000);
                socket.setKeepAlive(false);

                WorkThread thread = new WorkThread(socket);
                thread.setDaemon(true);
                executorService.execute(thread);
            } catch (IOException e) {
                e.printStackTrace();
                interrupt();
            }
        }
    }

    private void initServer() {
        try {
            serversocket = new ServerSocket();//1.创建socket
            serversocket.setReuseAddress(true);
            serversocket.bind(new InetSocketAddress(port));//2.绑定监听端口
        } catch (IOException e) {
            e.printStackTrace();
            interrupt();
        }
    }

    @Override
    public void destroy() {
        try {
            serversocket.close();
            executorService.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
