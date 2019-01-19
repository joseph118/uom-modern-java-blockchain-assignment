import core.message.Message;
import data.*;
import core.message.wallet.ErrorMessage;
import core.message.node.NodeMessage;
import core.message.wallet.SuccessMessage;
import process.*;
import data.NodeDataRequest;
import data.Command;
import org.apache.log4j.Logger;
import util.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NodeServer {
    final static Logger logger = Logger.getLogger(NodeServer.class);

    private final String nodeName;
    private final KeyHolder nodeKeys;

    private volatile ServerNodes serverNodes;
    private volatile Map<String, NodeDataRequest> nodeDataMap;

    private boolean running;

    public NodeServer(List<ServerNode> serverNodes, String nodeName, KeyHolder nodeKeys) {
        this.serverNodes = new ServerNodes(serverNodes);
        this.nodeName = nodeName;
        this.nodeKeys = nodeKeys;
        this.running = false;

        this.nodeDataMap = new ConcurrentHashMap<>();
    }

    public void startServer(int portNumber, Selector selector) throws IOException {
        setRunning(true);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(portNumber));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        Handshake.triggerConnectionToServerNodes(selector, this.serverNodes.getNodes(nodeName), nodeName);

        while (running) {
            try {
                selector.select();

                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                final Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    final SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isValid() && key.isAcceptable()) {
                        // a connection was accepted by a ServerSocketChannel.
                        registerClient(key);

                    } else if (key.isValid() && key.isConnectable()) {
                        Handshake.connectToServerNode(key, this.nodeName, nodeKeys.getPrivateKey(), executor);

                    } else if (key.isValid() && key.isWritable()) {
                        // a channel is ready for writing
                        if (key.attachment() instanceof NodeMessage) {
                            writeToNode(key);
                        } else if (key.attachment() instanceof Message) {
                            writeToWallet(key);
                        }
                    } else if (key.isValid() && key.isReadable()) {
                        // a channel is ready for reading
                        try {
                            final String clientData = getClientData((SocketChannel) key.channel());
                            key.interestOps(SelectionKey.OP_WRITE);

                            executor.submit(() ->
                                    processRequest(key, clientData));

                        } catch (Exception e) {
                            logger.error(e);
                            key.cancel();
                        }
                    }

                }
            } catch (Exception ex) {
                logger.info("Unexpected error. \n".concat(ex.toString()));
            }
        }

        serverSocketChannel.close();
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private void processRequest(SelectionKey key, String clientData) {
        final SocketChannel client = (SocketChannel) key.channel();
        final Selector selector = key.selector();

        try {
            final Map<String, String> requestMessage = Parser
                    .convertArgsToMap(clientData.split(","), "=");
            logger.info("Received: ".concat(requestMessage.toString()));

            if (isRequestArgumentsValid(requestMessage)) {
                final String command = requestMessage.get("command");
                Command userCommand = Parser.convertToCommand(command);

                // Wallet Requests
                if (userCommand.equals(Command.TRANSFER)) {
                    logger.info("Transfer verification received from wallet:".concat(client.getLocalAddress().toString()));
                    Transfer.processTransferRequest(key, requestMessage, nodeName, this.serverNodes.getConnectedNodes(nodeName), nodeKeys.getPrivateKey(), nodeDataMap);

                } else if (userCommand.equals(Command.HISTORY) || userCommand.equals(Command.BALANCE)) {
                    logger.info("Balance or history received from wallet: ".concat(client.getLocalAddress().toString()));
                    SimpleRequest.processHistoryOrBalanceRequest(key, requestMessage, userCommand, nodeName, nodeKeys.getPrivateKey());

                } else if (userCommand.equals(Command.CONFIRM)) {
                    logger.info("Transaction Confirm received from wallet: ".concat(client.getLocalAddress().toString()));
                    Confirmation.processTransferConfirmation(key, requestMessage, nodeKeys.getPrivateKey(), nodeName, serverNodes.getConnectedNodes(nodeName), nodeDataMap);



                // Node Requests
                } else if (userCommand.equals(Command.NODE_CONNECT)) {
                    logger.info("Node >Connection< received from ".concat(requestMessage.get("nodename")));
                    Handshake.connectToNode(key, requestMessage, userCommand, this.nodeName, nodeKeys.getPrivateKey(), serverNodes);

                } else if (userCommand.equals(Command.VERIFY)) {
                    logger.info("Node >Verify< received from ".concat(requestMessage.get("nodename")));
                    Verification.nodeVerifyTransaction(key, requestMessage, userCommand, nodeName, nodeKeys.getPrivateKey(), nodeDataMap);

                } else if (userCommand.equals(Command.VERIFY_OK)) {
                    logger.info("Node >Verify Ok< received from ".concat(requestMessage.get("nodename")));
                    Verification.nodeVerifyTransaction(key, requestMessage, userCommand, nodeName, nodeKeys.getPrivateKey(), nodeDataMap);

                } else if (userCommand.equals(Command.VERIFY_ERR)) {
                    logger.info("Node >Verify Error< received from ".concat(requestMessage.get("nodename")));
                    Verification.processVerifyError(key, requestMessage, command, nodeDataMap);

                } else if (userCommand.equals(Command.RECORD)) {
                    logger.info("Node >Save Record< received from ".concat(requestMessage.get("nodename")));
                    Record.processRecordRequest(key, requestMessage, nodeKeys.getPrivateKey(), nodeName, serverNodes, nodeDataMap);

                } else if (userCommand.equals(Command.RECORD_OK) || userCommand.equals(Command.RECORD_ERR)) {
                    logger.info("Node >Record Ok/Error< received from ".concat(requestMessage.get("nodename")));
                    Record.processRecordResponse(key, requestMessage, nodeKeys.getPrivateKey(), nodeDataMap);



                } else {
                    // Wallet request incorrect command

                    logger.info("Incorrect verification from ".concat(client.getLocalAddress().toString()));
                    client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid command", client.getLocalAddress().toString()));
                }
            } else {
                logger.info("Invalid arguments from ".concat(client.getLocalAddress().toString()));
                client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid arguments", client.getLocalAddress().toString()));
            }
        } catch (Exception ex) {
            if (key.attachment() != null) {
                // Node has been disconnected
                ServerNode serverNode = (ServerNode) key.attachment();
                try {
                    key.channel().close();
                } catch (Exception e) {}
                key.cancel();
                serverNodes.updateNodeConnection(serverNode.getName(), false, null);

                logger.info("Node: ".concat(serverNode.getName().concat(", has been disconnected.")));
                logger.error(ex);
            }
        }
    }

    private String getClientData(SocketChannel client) throws IOException {
        String message;

        ByteBuffer readBuffer = ByteBuffer.allocate(2048);

        client.read(readBuffer);
        message = new String(readBuffer.array()).trim();
        readBuffer.clear();

        return message;
    }

    private void registerClient(SelectionKey key) {
        final Selector selector = key.selector();
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        try {
            final SocketChannel client = serverSocketChannel.accept();
            client.configureBlocking(false);
            client.socket().setKeepAlive(true);
            client.register(selector, SelectionKey.OP_READ);

            logger.info("Accepted connection: ".concat(client.getLocalAddress().toString()));
        } catch (Exception e) {
            logger.error(e.toString());

            key.cancel();
        }
    }

    private void writeToNode(SelectionKey key) {
        final Selector selector = key.selector();
        final SocketChannel client = (SocketChannel) key.channel();
        final NodeMessage message = (NodeMessage) key.attachment();

        logger.info("To: ".concat(message.getServerNode().getName())
                .concat(" Sending: ".concat(message.getMessage())));

        try {
            writeToClient(message.getMessage(), client);
        } catch (IOException ex) {
            logger.info("System interrupted while writing to node: ".concat(message.getServerNode().getName())
                    .concat(". \n").concat(ex.toString()));
        }

        try {
            client.register(selector, SelectionKey.OP_READ, message.getServerNode());
        } catch (IOException ex) {
            logger.error(ex.toString());
        }

    }

    private void writeToWallet(SelectionKey key) {
        final SocketChannel client = (SocketChannel) key.channel();
        boolean closeConnection = true;

        final String messageResult;
        final String receiverName;
        if (key.attachment() instanceof ErrorMessage) {
            final ErrorMessage message = (ErrorMessage) key.attachment();
            messageResult = message.getErrorMessage();
            receiverName = message.getReceiverName();
        } else {
            final SuccessMessage message = (SuccessMessage) key.attachment();
            closeConnection = message.getCloseConnection();
            messageResult = message.getMessage();
            receiverName = message.getReceiverName();
        }

        logger.info("To: ".concat(receiverName).concat(" Sending: ".concat(messageResult)));

        try {
            writeToClient(messageResult, client);
        } catch (IOException ex) {
            logger.info("System interrupted while writing to wallet: ".concat(receiverName).concat(". \n")
                    .concat(ex.toString()));
            key.cancel();
        }

        if (closeConnection) {
            try {
                client.close();
            } catch (IOException ex) {
                logger.info(receiverName.concat("Error while closing down connection. \n").concat(ex.toString()));
            }

            key.cancel();
        } else {
            try {
                client.register(key.selector(), SelectionKey.OP_READ, null);
            } catch (IOException ex) {
                logger.error(ex.toString());
            }
        }
    }

    private void writeToClient(String message, SocketChannel client) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        // TODO is it sending all the data? or not receiving? check..
        while (buffer.remaining() > 0) {
            client.write(buffer);
        }
    }

    private static boolean isRequestArgumentsValid(Map<String, String> map) {
        if (map.containsKey("command") && map.containsKey("signature")) {
            if (map.get("command").equals(Command.NODE_CONNECT.name())) {
                // Server Node - Handshake
                return map.containsKey("nodename") && map.containsKey("phase");
            } else if (map.get("command").equals(Command.VERIFY.name())
                    || map.get("command").equals(Command.VERIFY_OK.name())
                    || map.get("command").equals(Command.CONFIRM.name())
                    || map.get("command").equals(Command.RECORD.name())) {

                if (map.containsKey("guid")
                    && map.containsKey("senderkey")
                    && map.containsKey("receiverkey")
                    && map.containsKey("amount")
                    && map.containsKey("sendersignature")
                    && map.containsKey("timestamp")
                    && map.containsKey("hash")) {

                    if (map.get("command").equals(Command.CONFIRM.name())
                            || map.get("command").equals(Command.RECORD.name())) {

                        if (map.containsKey("signature1")
                                && map.containsKey("signature2")
                                && map.containsKey("signature3")) {

                            if (map.get("command").equals(Command.RECORD.name())) {
                                return map.containsKey("confirmsignature");
                            } else {
                                return true;
                            }
                        }
                    } else {
                        return map.containsKey("nodename");
                    }
                }
            } else if (map.get("command").equals(Command.VERIFY_ERR.name())) {
                return map.containsKey("nodename");
            } else if (map.get("command").equals(Command.RECORD_OK.name())
                    || map.get("command").equals(Command.RECORD_ERR.name())) {
                return map.containsKey("senderkey");
            }else {
                // Wallet
                if (map.containsKey("key")) {
                    if (map.get("command").equals(Command.TRANSFER.name())) {
                        return map.containsKey("destinationkey")
                                && map.containsKey("amount")
                                && map.containsKey("guid");
                    } else {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
