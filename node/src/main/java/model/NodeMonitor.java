package model;

import util.Nodes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class NodeMonitor implements Runnable {
    private final AtomicInteger connections;
    private final ByteBuffer buffer;
    private final String nodeName;
    private final List<ServerNode> serverNodes;

    public NodeMonitor(String nodeName) {
        this.nodeName = nodeName;

        serverNodes = Nodes.getServerNodes();
        connections = new AtomicInteger(0);
        buffer = ByteBuffer.allocate(1024);
    }

    public int getConnections() {
        return connections.get();
    }

    @Override
    public void run() {
        try {
            final Selector selector = Selector.open();
            final SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.socket().setKeepAlive(true);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            this.connectToNodes(serverNodes, nodeName, selector);

            while (true) {
                selector.select();

                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                final Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    final SelectionKey key = iterator.next();

                    if (key.isValid() && key.isAcceptable()) {
                        System.out.println("key acceptable");
                    } else if (key.isValid() && key.isReadable()) {
                        System.out.println("key readable");
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }

        System.out.println("Stopped...");
    }

    private void connectToNodes(List<ServerNode> serverNodes, String nodeName, Selector selector) {

    }
}
