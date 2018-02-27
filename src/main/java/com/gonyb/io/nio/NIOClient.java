package com.gonyb.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class NIOClient {
    private SocketChannel client;
    private Selector selector;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    private NIOClient() throws IOException {
        client = SocketChannel.open();
        client.configureBlocking(false);
        client.connect(new InetSocketAddress(8080));
        selector = Selector.open();
        client.register(selector, SelectionKey.OP_CONNECT);
    }

    private void session() throws IOException {
        if (client.isConnectionPending()) {
            client.finishConnect();
            client.register(selector, SelectionKey.OP_WRITE);
            Scanner scanner = new Scanner(System.in);
            System.out.println("请在控制台登记姓名：");
            while (scanner.hasNextLine()) {
                String name = scanner.nextLine().trim();
                if ("".equals(name)) {
                    continue;
                }
                process(name);
            }
        } else {
            System.out.print("连接失败");
        } 
    }

    private void process(String name) throws IOException {
        boolean isFinish = true;
        while (isFinish){
            int count = selector.select();
            if(count==0){
                continue;
            }
            for (SelectionKey key : selector.keys()) {
                if(key.isWritable()){
                    byteBuffer.clear();
                    byteBuffer.put(name.getBytes());
                    byteBuffer.flip();
                    client.write(byteBuffer);
                    client.register(selector,SelectionKey.OP_READ);
                }else if (key.isReadable()){
                    isFinish = false;
                    byteBuffer.clear();
                    int len = client.read(byteBuffer);
                    if(len==-1){
                        continue;
                    }
                    System.out.println("接受到服务端返回的信息："+new String(byteBuffer.array(),0,len));
                    byteBuffer.flip();
                    client.register(selector,SelectionKey.OP_WRITE);
                }
            }

        }
    }

    public static void main(String[] args) throws IOException {
        new NIOClient().session();
    }
}
