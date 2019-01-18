import data.*;
import core.message.wallet.ErrorMessage;
import core.message.node.NodeMessage;
import core.message.wallet.SuccessMessage;
import process.*;
import data.verification.VerificationRequest;
import exception.ArgumentsNotFoundException;
import util.Command;
import org.apache.log4j.Logger;
import util.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NodeServer {
    final static Logger logger = Logger.getLogger(NodeServer.class);

    private final ServerNodes serverNodes;
    private final String nodeName;
    private final KeyHolder nodeKeys;
    private final Map<String, VerificationRequest> verificationMap;
    private boolean running;

    public NodeServer(List<ServerNode> serverNodes, String nodeName, KeyHolder nodeKeys) {
        this.serverNodes = new ServerNodes(serverNodes);
        this.nodeName = nodeName;
        this.nodeKeys = nodeKeys;
        this.running = false;

        this.verificationMap = new ConcurrentHashMap<>();
    }

    public void startServer(int portNumber, Selector selector) throws IOException {
        setRunning(true);

        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(portNumber));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        triggerConnectionToServerNodes(selector);

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
                        Handshake.connectToServerNode(key, this.nodeName, nodeKeys.getPrivateKey());

                    } else if (key.isValid() && key.isWritable()) {
                        // a channel is ready for writing
                        if (key.attachment() != null) {
                            processWriteRequest(key);
                        }
                    } else if (key.isValid() && key.isReadable()) {
                        // a channel is ready for reading
                        // TODO Processing has to go to another thread/s
                        processRequest(key);
                    }
                    iterator.remove();
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

    private void processRequest(SelectionKey key) throws IOException, ArgumentsNotFoundException {
        final SocketChannel client = (SocketChannel) key.channel();
        final Selector selector = key.selector();

        logger.info("Read verification received.");
        try {
            final String clientData = getClientData(client);
            final Map<String, String> requestMessage = Parser
                    .convertArgsToMap(clientData.split(","), "=");
            logger.info("Received: ".concat(requestMessage.toString()));

            if (isRequestArgumentsValid(requestMessage)) {
                final String command = requestMessage.get("command");
                Command userCommand = Parser.convertToCommand(command);

                if (userCommand.equals(Command.TRANSFER)) {
                    logger.info("Transfer verification received from ".concat(client.getLocalAddress().toString()));
                    Transfer.processTransferRequest(key, requestMessage, nodeName, this.serverNodes.getConnectedNodes(), nodeKeys.getPrivateKey(), verificationMap);

                } else if (userCommand.equals(Command.HISTORY) || userCommand.equals(Command.BALANCE)) {
                    logger.info("Balance or history verification received from ".concat(client.getLocalAddress().toString()));
                    SimpleRequest.processHistoryOrBalanceRequest(key, requestMessage, userCommand, nodeName, nodeKeys.getPrivateKey());

                } else if (userCommand.equals(Command.CONNECT)) {
                    logger.info("Node connection verification received from ".concat(requestMessage.get("nodename")));
                    Handshake.connectToNode(key, requestMessage, userCommand, this.nodeName, nodeKeys.getPrivateKey(), serverNodes);

                } else if (userCommand.equals(Command.VERIFY)) {
                    logger.info("Node verify verification received from ".concat(requestMessage.get("nodename")));
                    Verification.nodeVerifyTransaction(key, requestMessage, Command.VERIFY, nodeName, nodeKeys.getPrivateKey(), verificationMap);

                } else if (userCommand.equals(Command.VERIFY_OK)) {
                    logger.info("Node verify ok received from ".concat(requestMessage.get("nodename")));
                    Verification.nodeVerifyTransaction(key, requestMessage, Command.VERIFY_OK, nodeName, nodeKeys.getPrivateKey(), verificationMap);

                } else if (userCommand.equals(Command.VERIFY_ERR)) {
                    logger.info("Node verify error received from ".concat(requestMessage.get("nodename")));
                    Verification.processVerifyError(key, requestMessage, command, verificationMap);

                } else if (userCommand.equals(Command.CONFIRM)) {
                    logger.info("Confirm verification received from ".concat(client.getLocalAddress().toString()));
                    Confirmation.processTransferConfirmation(key, requestMessage, nodeKeys.getPrivateKey(), nodeName);

                } else {
                    logger.info("Incorrect verification from ".concat(client.getLocalAddress().toString()));
                    client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid command", client.getLocalAddress().toString()));
                }
            } else {
                logger.info("Invalid arguments from ".concat(client.getLocalAddress().toString()));
                client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid arguments", client.getLocalAddress().toString()));
            }
        } catch (IOException ex) {
            if (key.attachment() != null) {
                // Node has been disconnected
                ServerNode serverNode = (ServerNode) key.attachment();
                key.channel().close();
                key.cancel();
                serverNodes.updateNodeConnection(serverNode.getName(), false, null);

                logger.info("Node: ".concat(serverNode.getName().concat(", has been disconnected.")));
            }
        }
    }

    private String getClientData(SocketChannel client) throws IOException {
        String message;

        ByteBuffer readBuffer = ByteBuffer.allocate(1048);

        client.read(readBuffer);
        message = new String(readBuffer.array()).trim();
        readBuffer.clear();

        return message;
    }

    private void registerClient(SelectionKey key) throws IOException {
        final Selector selector = key.selector();
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        final SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        logger.info("Accepted connection: ".concat(client.getLocalAddress().toString()));
    }

    private void writeToNode(SelectionKey key) throws IOException {
        final Selector selector = key.selector();
        final SocketChannel client = (SocketChannel) key.channel();
        final NodeMessage message = (NodeMessage) key.attachment();

        logger.info("To: ".concat(message.getServerNode().getName())
                .concat(" Sending: ".concat(message.getMessage())));

        try {
            writeToClient(message.getMessage(), client);
        } catch (IOException ex) {
            logger.info("System interrupted while writing verification to node: ".concat(message.getServerNode().getName())
                    .concat(". \n").concat(ex.toString()));
        }

        client.register(selector, SelectionKey.OP_READ, message.getServerNode());
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
            logger.info("System interrupted while writing verification to wallet: ".concat(receiverName).concat(". \n")
                    .concat(ex.toString()));
        }

        if (closeConnection) {
            try {
                client.close();
            } catch (IOException ex) {
                logger.info(receiverName.concat("Error while closing down connection. \n").concat(ex.toString()));
            }

            key.cancel();
        }
    }

    private void writeToClient(String message, SocketChannel client) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());

        while (buffer.remaining() > 0) {
            client.write(buffer);
        }
    }

    private void processWriteRequest(SelectionKey key) throws IOException {
        if (key.attachment() instanceof NodeMessage) {
            writeToNode(key);
        } else {
            writeToWallet(key);
        }
    }

    private void triggerConnectionToServerNodes(Selector selector) {
        final List<ServerNode> serverNodes = this.serverNodes.getConnectedNodes();

        if (!serverNodes.isEmpty()) {
            serverNodes.forEach(serverNode -> {
                logger.info("Connecting to: ".concat(serverNode.toString()));
                try {
                    SocketChannel nodeServer = SocketChannel.open();
                    nodeServer.configureBlocking(false);
                    nodeServer.connect(
                            new InetSocketAddress(serverNode.getIp(), serverNode.getPort()));
                    nodeServer.register(selector, SelectionKey.OP_CONNECT, serverNode);

                    serverNode.setSocketChannel(nodeServer);
                } catch (IOException e) {
                    logger.info(nodeName.concat("Error when connecting with ").concat(serverNode.toString()));
                }
            });
        }
    }

    private static boolean isRequestArgumentsValid(Map<String, String> map) {
        if (map.containsKey("command") && map.containsKey("signature")) {
            if (map.get("command").equals(Command.CONNECT.name())) {
                // Server Node - Handshake
                return map.containsKey("nodename") && map.containsKey("phase");
            } else if (map.get("command").equals(Command.VERIFY.name())
                    || map.get("command").equals(Command.VERIFY_OK.name())) {
                // Server Node - Verify
                return map.containsKey("nodename")
                        && map.containsKey("guid")
                        && map.containsKey("senderkey")
                        && map.containsKey("receiverkey")
                        && map.containsKey("amount")
                        && map.containsKey("sendersignature")
                        && map.containsKey("timestamp")
                        && map.containsKey("hash");
            } else if (map.get("command").equals(Command.VERIFY_ERR.name())) {
                return map.containsKey("nodename");
            } else {
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
