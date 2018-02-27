package com.gonyb.io.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NIOServer {
    private Selector selector;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private Map<SelectionKey,String> nameMap = new HashMap<>();

    private NIOServer(int port) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(port));
        selector = Selector.open();
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务启动完成，监听端口："+port);
    }
    
    private void listener() throws IOException {
        
        while (true){
            int count = selector.select();
            if(count==0){
                continue;
            }
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()){
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    addClient(key);
                } else if (key.isReadable()) {
                    handleClientMsg(key);
                } else if (key.isWritable()) {
                    responseClient(key);
                }
                iterator.remove();
            }
        }
    }

    private void responseClient(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.configureBlocking(false);
        String msg = nameMap.get(key)+",接受到你的请求,处理成功";
        buffer.clear();
        byte[] bytes = msg.getBytes();
        buffer.put(bytes);
        buffer.flip();
        channel.write(buffer);
        channel.register(selector,SelectionKey.OP_READ);
    }

    private void handleClientMsg(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.configureBlocking(false);
        buffer.clear();
        int len = channel.read(buffer);
        buffer.flip();
        String name = new String(buffer.array(), 0, len);
        nameMap.put(key,name);
        System.out.println(name+",发来请求");
        channel.register(selector,SelectionKey.OP_WRITE);
    }

    private void addClient(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector,SelectionKey.OP_READ);
    }

    public static void main(String[] args) throws IOException {
        new NIOServer(8080).listener();
    }
    
}
