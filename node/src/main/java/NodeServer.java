import exception.ArgumentsNotFoundException;
import model.Command;
import model.KeyHolder;
import model.ServerNode;
import model.core.Ledger;
import model.message.ErrorMessage;
import model.message.SuccessMessage;
import security.KeyLoader;
import util.ArgumentParser;
import util.Commands;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NodeServer {
    private final Selector selector;
    private final List<ServerNode> serverNodes;
    private final String nodeName;
    private final KeyHolder nodeKeys;
    private int connections;

    private boolean running;
    private ByteBuffer readBuffer;


    public NodeServer(Selector selector, List<ServerNode> serverNodes, String nodeName, KeyHolder nodeKeys) {
        this.selector = selector;
        this.serverNodes = serverNodes;
        this.nodeName = nodeName;
        this.nodeKeys = nodeKeys;
        this.running = false;
        connections = 0;
    }

    public void startServer(int portNumber) throws IOException {
        setRunning(true);
        readBuffer = ByteBuffer.allocate(1048);

        connectToServerNodes();

        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(portNumber));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (running) {
            try {
                selector.select();

                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                final Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    final SelectionKey key = iterator.next();

                    if (key.isValid() && key.isAcceptable()) {
                        // a connection was accepted by a ServerSocketChannel.
                        registerClient(key);

                    } else if (key.isValid() && key.isConnectable()) {
                        SocketChannel nodeClient = (SocketChannel) key.channel();
                        try {
                            if (nodeClient.finishConnect()) {
                                connections++;

                                ServerNode serverNode = (ServerNode) key.attachment();
                                System.out.println(serverNode.toString());

                                nodeClient.close();
                            }
                        } catch (Exception e) {
                            System.out.println("failed...");
                        }
                    } else if (key.isValid() && key.isWritable()) {
                        // a channel is ready for writing
                        if (key.attachment() != null) {
                            processWriteRequest(key);
                        }
                    } else if (key.isValid() && key.isReadable()) {
                        // a channel is ready for reading

                        if (key.attachment() != null) {
                            ServerNode serverNode = (ServerNode) key.attachment();
                            System.out.println(serverNode.toString());
                        } else {
                            processRequest(key);
                        }
                    }
                    iterator.remove();
                }
            } catch (Exception ex) {
                System.out.println("Unexpected error. \n".concat(ex.toString()));
            }
        }

        serverSocketChannel.close();
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private void processRequest(SelectionKey key) throws IOException, ArgumentsNotFoundException {
        final SocketChannel client = (SocketChannel) key.channel();
        client.read(readBuffer);

        final String clientData = new String(readBuffer.array()).trim();
        final Map<String, String> requestMessage = ArgumentParser
                .convertArgsToMap(clientData.split(","), "=");
        readBuffer.clear();


        // TODO: Thread start
        if (NodeUtils.isRequestArgumentsValid(requestMessage)) {
            final String command = requestMessage.get("command");
            Command userCommand = Commands.convertToCommand(command);

            if (userCommand.equals(Command.TRANSFER)) {
                processTransferRequest(requestMessage, command, client);

            } else if (userCommand.equals(Command.HISTORY) || userCommand.equals(Command.BALANCE)) {
                processHistoryOrBalanceRequest(requestMessage, userCommand, client);

            } else if (userCommand.equals(Command.CONNECT)) {
                // TODO: process node connection
                System.out.println("connect request");
            } else {
                client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid command"));
            }
        } else {
            client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid arguments"));
        }
    }

    private void processHistoryOrBalanceRequest(Map<String, String> requestMessage, Command userCommand, SocketChannel client) throws ClosedChannelException {
        final String signature = requestMessage.get("signature");
        final String base64PublicKey = requestMessage.get("key");
        final PublicKey clientPublicKey = KeyLoader.decodePublicKey(base64PublicKey);

        if (NodeUtils.verifyUserSignature(clientPublicKey, userCommand.name(), signature)) {
            final String data = (userCommand.equals(Command.HISTORY))
                    ? Ledger.getUserHistoryAsString(nodeName, base64PublicKey)
                    : Ledger.getUserBalance(nodeName, base64PublicKey);

            final String signedSignature = NodeUtils.generateSignature(nodeKeys.getPrivateKey(), data);
            final String responseData = NodeUtils.generateBody(signedSignature, data);

            client.register(selector, SelectionKey.OP_WRITE, new SuccessMessage(responseData, true));
        } else {
            client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid signature"));
        }
    }

    private void processTransferRequest(Map<String, String> requestMessage, String command, SocketChannel client) throws ClosedChannelException {
        final String destinationKey = requestMessage.get("destinationKey");
        final String guid = requestMessage.get("guid");
        final String stringAmount = requestMessage.get("amount");
        final String signature = requestMessage.get("signature");
        final String base64PublicKey = requestMessage.get("key");
        final PublicKey clientPublicKey = KeyLoader.decodePublicKey(base64PublicKey);
        final float amount = Float.parseFloat(stringAmount);

        if (NodeUtils.verifyTransferSignature(clientPublicKey, command, signature, destinationKey, guid, stringAmount)) {
            // TODO: transfer....
        } else {
            client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid signature"));
        }
    }

    private void registerClient(SelectionKey key) throws IOException {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        final SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        System.out.println("Connection Accepted: " + client.getLocalAddress().toString() + "\n");
    }

    private void processWriteRequest(SelectionKey key) {
        final SocketChannel client = (SocketChannel) key.channel();

        boolean closeConnection = true;
        String messageResult;

        if (key.attachment() instanceof ErrorMessage) {
            final ErrorMessage message = (ErrorMessage) key.attachment();
            messageResult = message.getErrorMessage();
        } else {
            final SuccessMessage message = (SuccessMessage) key.attachment();
            closeConnection = message.getCloseConnection();
            messageResult = message.getMessage();
        }

        try {
            client.write(ByteBuffer.wrap(messageResult.getBytes()));
        } catch (IOException ex) {
            System.out.println("System interrupted while writing request to client. \n".concat(ex.toString()));
        }

        if (closeConnection) {
            try {
                client.close();
            } catch (IOException ex) {
                System.out.println("Error while closing down connection. \n".concat(ex.toString()));
            }
        }
    }

    private void connectToServerNodes() {
        if (!serverNodes.isEmpty()) {
            serverNodes.forEach(serverNode -> {
                System.out.println("Connecting to: ".concat(serverNode.toString()));
                try {
                    SocketChannel channel = SocketChannel.open();
                    channel.configureBlocking(false);
                    channel.connect(
                            new InetSocketAddress(serverNode.getIp(), serverNode.getPort()));
                    channel.register(selector, SelectionKey.OP_CONNECT, serverNode);
                } catch (IOException e) {
                    System.out.println(e.toString());
                    System.out.println("Error when connecting with ".concat(serverNode.toString()));
                }
            });
        }
    }
}
