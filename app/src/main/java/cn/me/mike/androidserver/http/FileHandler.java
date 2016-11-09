package cn.me.mike.androidserver.http;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.StringTokenizer;

import cn.me.mike.androidserver.Constant;
import okio.BufferedSink;
import okio.Okio;


/**
 * Created by ske on 2016/11/8.
 * 遍历文件目录：
 * 1.文件夹：点击进入
 * 2.文件：点击下载
 * 遇到的问题：手机端请求文件时，响应头用byte类型输入才正确
 */

public class FileHandler {
    private Socket socket;
    private String filePath;
    private boolean isWeChatOrQQ;

    public FileHandler(Socket socket, String filePath, boolean weChat) {
        this.socket = socket;
        this.filePath = filePath;
        this.isWeChatOrQQ = weChat;
    }

    public void handle() {
        try {
            BufferedSink sink = Okio.buffer(Okio.sink(socket.getOutputStream()));
            File requestRootFile = new File(filePath);

            //文件不存在
            if (!requestRootFile.exists()) {
                setHeader(sink, "HTTP/1.1 404 Not Found\r\n", "text/html;charset=UTF-8", Constant.NOT_FOUND.length());
                sink.writeUtf8(Constant.NOT_FOUND);
            } else if (requestRootFile.canRead()) {
                if (requestRootFile.isDirectory()) {
                    setHeader(sink, "HTTP/1.1 200 OK\r\n", "text/html;charset=UTF-8", 0);
                    sink.writeUtf8("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;"
                            + "charset=utf-8\"><title>文件服务</title></head><body><h1>Directory "
                            + filePath + "</h1><br/>");
                    String[] files = requestRootFile.list();
                    for (String fName : files) {
                        File file = new File(requestRootFile, fName);
                        boolean isDir = file.isDirectory();
                        if (isDir) {//如果是文件夹，加粗并添加分隔符
                            sink.writeUtf8("<b>");
                            fName += "/";
                        }
                        sink.writeUtf8("<a href=\"" + encodeUri(filePath + fName) + "\">" + fName + "</a>");
                        if (file.isFile()) {//是文件时显示大小
                            long len = file.length();
                            sink.writeUtf8(" &nbsp;<font size=2>(");
                            String msg = "";
                            if (len < 1024)
                                msg += len + " bytes";
                            else if (len < 1024 * 1024)
                                msg += len / 1024 + "." + (len % 1024 / 10 % 100) + " KB";
                            else
                                msg += len / (1024 * 1024) + "." + len % (1024 * 1024) / 10 % 100 + " MB";
                            sink.writeUtf8(msg).writeUtf8(")</font>");
                        }
                        sink.writeUtf8("<br/>");
                        if (isDir)
                            sink.writeUtf8("</b>");
                    }
                    sink.writeUtf8("</body></html>");
                } else {//下载文件
                    String mime = null;
                    String realPath = requestRootFile.getCanonicalPath();
                    int dot = realPath.lastIndexOf(".");
                    if (dot >= 0) {
                        //获取文件MIME类型
                        mime = (String) Constant.theMimeTypes.get(realPath.substring(dot + 1).toLowerCase(Locale.ENGLISH));
                    }
                    //当类型未知或者没有后缀
                    if (TextUtils.isEmpty(mime))
                        mime = Constant.MIME_DEFAULT_BINARY;

                    long fileLength = requestRootFile.length();

//                        sink.writeUtf8("HTTP/1.1 200 OK").writeUtf8("\r\n");
//                        sink.writeUtf8("Content-Length: ").writeLong(fileLength).writeUtf8("\r\n");
//                        sink.writeUtf8("Content-Type: ").writeUtf8(mime).writeUtf8("\r\n");
//                        sink.writeUtf8("Content-Description: File Transfer").writeUtf8("\r\n");
//                        sink.writeUtf8("Content-Disposition: ").writeUtf8("attachment;filename=").writeUtf8(encodeFilename(requestRootFile)).writeUtf8("\r\n");
//                        sink.writeUtf8("Content-Transfer-Encoding: binary").writeUtf8("\r\n\r\n");
                    //设置响应头
                    setDownloadHeader(sink, requestRootFile, fileLength, mime);

//                    普通io
//                        OutputStream out = socket.getOutputStream();
//                        out.write(sb.toString().getBytes(), 0, sb.toString().length());
//                        write(requestRootFile, out);

//                    okio
                    sink.writeAll(Okio.source(requestRootFile));
                }
            } else {//没有权限访问
                setHeader(sink, "HTTP/1.1 403 Forbidden\r\n", "text/html;charset=UTF-8", Constant.FORBIDDEN.length());
                sink.writeUtf8(Constant.FORBIDDEN);
            }
            sink.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setHeader(BufferedSink sink, String responseLine, String contentType, long contentLength) throws IOException {
        sink.writeUtf8(responseLine);
        if (contentLength != 0)
            sink.writeUtf8("Content-Length: ").writeLong(contentLength).writeUtf8("\r\n");
        sink.writeUtf8("Content-Type: ").writeUtf8(contentType).writeUtf8("\r\n\r\n");
    }

    private void setDownloadHeader(BufferedSink sink, File file, long fileLength, String mime) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n")
                .append("Content-Length: ").append(fileLength).append("\r\n")
                .append("Content-Type: ").append(mime).append("\r\n")
                .append("Content-Description: File Transfer\r\n");
        if (!isWeChatOrQQ)//是微信qq等时让可以浏览的文件直接浏览
            sb.append("Content-Disposition: ").append("attachment;filename=").append(encodeFilename(file)).append("\r\n");
        sb.append("Content-Transfer-Encoding: binary").append("\r\n\r\n");

        //在手机端访问时 响应头 不用byte写居然不行。。。
        sink.write(sb.toString().getBytes(), 0, sb.toString().length());
    }

    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/"))
                newUri += "/";
            else if (tok.equals(" "))
                newUri += "%20";
            else {
                newUri += URLEncoder.encode(tok);
            }
        }
        return newUri;
    }

    private String encodeFilename(File file) throws IOException {
        String filename = URLEncoder.encode(getFilename(file), Constant.ENCODING);
        return filename.replace("+", "%20");
    }

    private String getFilename(File file) {
        return file.isFile() ? file.getName() : file.getName() + ".zip";
    }

    //用okio写
    private void write(File inputFile, BufferedSink sink) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(inputFile);
            int len;
            byte[] data = new byte[8 * 1024];
            while ((len = in.read(data)) != -1) {
                sink.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in.close();
            sink.close();
        }
    }

    //普通io写
    private void write(File inputFile, OutputStream outStream) throws IOException {
        FileInputStream fis = new FileInputStream(inputFile);
        try {
            int count;
            byte[] buffer = new byte[Constant.BUFFER_LENGTH];
            while ((count = fis.read(buffer)) != -1) {
                outStream.write(buffer, 0, count);
            }
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            fis.close();
            outStream.close();
        }
    }
}
