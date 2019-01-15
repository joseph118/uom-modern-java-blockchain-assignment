import core.DataHolder;
import core.Ledger;
import core.message.ErrorMessage;
import core.message.NodeMessage;
import core.message.SuccessMessage;
import core.request.VerifyRequest;
import exception.ArgumentsNotFoundException;
import model.Command;
import model.KeyHolder;
import model.ServerNode;
import model.ServerNodeVerify;
import org.apache.log4j.Logger;
import security.HashBuilder;
import security.KeyLoader;
import util.ArgumentParser;
import util.Commands;
import util.Conversion;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NodeServer {
    final static Logger logger = Logger.getLogger(NodeServer.class);
    private final Selector selector;
    private final List<ServerNode> serverNodes;
    private final String nodeName;
    private final KeyHolder nodeKeys;
    private final Map<String, DataHolder> verificationMap;
    private boolean running;

    public NodeServer(Selector selector, List<ServerNode> serverNodes, String nodeName, KeyHolder nodeKeys) {
        this.selector = selector;
        this.serverNodes = serverNodes;
        this.nodeName = nodeName;
        this.nodeKeys = nodeKeys;
        this.running = false;

        this.verificationMap = new ConcurrentHashMap<>();
    }

    public void startServer(int portNumber) throws IOException {
        setRunning(true);

        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(portNumber));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        triggerConnectionToServerNodes();

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
                        connectToServerNode(key);

                    } else if (key.isValid() && key.isWritable()) {
                        // a channel is ready for writing
                        if (key.attachment() != null) {
                            processWriteRequest(key);
                        }
                    } else if (key.isValid() && key.isReadable()) {
                        // a channel is ready for reading
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

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private long getConnectedNodesCount() {
        return this.serverNodes.parallelStream()
                .filter(ServerNode::isConnected).count();
    }

    private void connectToServerNode(SelectionKey key) throws IOException {
        SocketChannel nodeClient = (SocketChannel) key.channel();
        ServerNode serverNode = (ServerNode) key.attachment();

        try {
            if (nodeClient.finishConnect()) {
                nodeClient.configureBlocking(false);
                nodeClient.socket().setKeepAlive(true);

                logger.info("Connected to: ".concat(serverNode.toString().concat(", Performing handshake.")));

                final String message = NodeUtils.generateNodeHandshakeMessage(nodeKeys.getPrivateKey(), nodeName, "handshake_1");
                nodeClient.register(this.selector, SelectionKey.OP_WRITE, new NodeMessage(message, serverNode));
            }
        } catch (Exception e) {
            key.cancel();
            nodeClient.close();

            logger.info("Unable to to connect to: ".concat(serverNode.toString()));
        }
    }

    private void processRequest(SelectionKey key) throws IOException, ArgumentsNotFoundException {
        final SocketChannel client = (SocketChannel) key.channel();

        logger.info("Read request received.");
        try {
            final String clientData = getClientData(client);
            final Map<String, String> requestMessage = ArgumentParser
                    .convertArgsToMap(clientData.split(","), "=");
            logger.info("Received: ".concat(requestMessage.toString()));

            // TODO: Thread start
            if (NodeUtils.isRequestArgumentsValid(requestMessage)) {
                final String command = requestMessage.get("command");
                Command userCommand = Commands.convertToCommand(command);

                if (userCommand.equals(Command.TRANSFER)) {
                    logger.info("Transfer request received from ".concat(client.getLocalAddress().toString()));
                    processTransferRequest(requestMessage, command, client);

                } else if (userCommand.equals(Command.HISTORY) || userCommand.equals(Command.BALANCE)) {
                    logger.info("Balance or history request received from ".concat(client.getLocalAddress().toString()));
                    processHistoryOrBalanceRequest(requestMessage, userCommand, client);

                } else if (userCommand.equals(Command.CONNECT)) {
                    logger.info("Node connection request received from ".concat(requestMessage.get("nodename")));
                    connectToNode(key, requestMessage, userCommand);

                } else if (userCommand.equals(Command.VERIFY)) {
                    logger.info("Node verify request received from ".concat(requestMessage.get("nodename")));
                    nodeVerifyTransaction(key, requestMessage, Command.VERIFY);

                } else if (userCommand.equals(Command.VERIFY_OK)) {
                    logger.info("Node verify ok received from ".concat(requestMessage.get("nodename")));
                    nodeVerifyTransaction(key, requestMessage, Command.VERIFY_OK);

                } else if (userCommand.equals(Command.VERIFY_ERR)) {
                    logger.info("Node verify error received from ".concat(requestMessage.get("nodename")));
                    processVerifyError(key, requestMessage, command);

                } else {
                    logger.info("Incorrect request from ".concat(client.getLocalAddress().toString()));
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
                updateNodeConnection(serverNode.getName(), false, null);

                logger.info("Node: ".concat(serverNode.getName().concat(", has been disconnected.")));
            }
        }
    }

    private void processVerifyError(SelectionKey key, Map<String, String> requestMessage, String command) throws IOException {
        final String nodeName = requestMessage.get("nodename");
        final String signature = requestMessage.get("signature");
        final Path path = getNodeCertificate(nodeName);
        final PublicKey nodePublicKey = KeyLoader.loadPublicKey(path);

        if (path != null) {
            if (NodeUtils.verifyNodeSignature(nodePublicKey, command, nodeName, signature)) {
                final ServerNodeVerify serverNodeVerify = (ServerNodeVerify) key.attachment();
                final SocketChannel nodeClient = (SocketChannel) key.channel();

                verificationMap.get(serverNodeVerify.getSenderKey()).incrementError();

                nodeClient.register(selector, SelectionKey.OP_READ, new ServerNode(serverNodeVerify.getName(),
                        serverNodeVerify.getIp(),
                        serverNodeVerify.getPort()));
            }
        } else {
            key.cancel();
            key.channel().close();
        }
    }

    private void nodeVerifyTransaction(SelectionKey key, Map<String, String> requestMessage, Command command) throws IOException {
        final VerifyRequest verifyRequest = new VerifyRequest(requestMessage);

        Path path = getNodeCertificate(verifyRequest.getNodeName());

        if (path != null) {
            final PublicKey nodePublicKey = KeyLoader.loadPublicKey(path);
            final ServerNode serverNode = (ServerNode) key.attachment();
            final SocketChannel nodeClient = (SocketChannel) key.channel();

            if (NodeUtils.verifyNodeTransferSignature(nodePublicKey,
                    verifyRequest.getSignature(),
                    verifyRequest.getGuid(),
                    verifyRequest.getSenderKey(),
                    verifyRequest.getReceiverKey(),
                    verifyRequest.getAmountString(),
                    verifyRequest.getSenderSignature(),
                    verifyRequest.getTimestamp(),
                    verifyRequest.getHash(),
                    verifyRequest.getNodeName())) {

                final float amount = Float.parseFloat(verifyRequest.getAmountString());
                final float userBalance = Ledger.getUserBalance(nodeName, verifyRequest.getSenderKey()).calculateBalance();

                if (userBalance >= amount) {
                    if (command.equals(Command.VERIFY)) {
                        final String newHash = generateTransactionHash(verifyRequest.getSenderKey(),
                                verifyRequest.getReceiverKey(),
                                verifyRequest.getGuid(),
                                verifyRequest.getAmountString(),
                                verifyRequest.getSignature(),
                                verifyRequest.getTimestamp(),
                                verifyRequest.getNodeName());

                        if (verifyRequest.getHash().equals(newHash)) {
                            final String message = NodeUtils.generateNodeVerifyMessage(nodeKeys.getPrivateKey(),
                                    Command.VERIFY_OK.name(),
                                    verifyRequest.getGuid(),
                                    verifyRequest.getSenderKey(),
                                    verifyRequest.getReceiverKey(),
                                    verifyRequest.getAmountString(),
                                    verifyRequest.getSenderSignature(),
                                    verifyRequest.getTimestamp(),
                                    verifyRequest.getHash(),
                                    this.nodeName);

                            nodeClient.register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, serverNode));
                        } else {
                            final String message = NodeUtils
                                    .generateNodeVerifyErrorMessage(nodeKeys.getPrivateKey(), this.nodeName, Command.VERIFY_ERR);
                            nodeClient.register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, serverNode));
                        }
                    } else {
                        // Verify ok
                        final String signature = verifyRequest.getNodeName()
                                .concat(":").concat(verifyRequest.getSignature());

                        verificationMap.get(verifyRequest.getSenderKey())
                                .addSignatureAndIncrement(signature);

                        nodeClient.register(selector, SelectionKey.OP_READ, serverNode);
                    }
                } else {
                    final String message = NodeUtils
                            .generateNodeVerifyErrorMessage(nodeKeys.getPrivateKey(), this.nodeName, Command.VERIFY_ERR);
                    nodeClient.register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, serverNode));
                }
            } else {
                final String message = NodeUtils
                        .generateNodeVerifyErrorMessage(nodeKeys.getPrivateKey(), this.nodeName, Command.VERIFY_ERR);
                nodeClient.register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, serverNode));
            }
        } else {
            key.cancel();
            key.channel().close();
        }
    }

    private boolean waitForVerificationProcess(String userPublicKey) {
        try {
            int requestCounter = 0;
            while (!areAllRequestsReady(userPublicKey) || requestCounter != 3) {
                Thread.sleep(5000);
                requestCounter++;
            }
        } catch (InterruptedException ex) {
            logger.error(ex);

            return false;
        }

        return true;
    }

    private boolean areAllRequestsReady(String userPublicKey) {
        DataHolder dataHolder = verificationMap.get(userPublicKey);
        final int totalConnectionsDone = dataHolder.getConnectionsOk() + dataHolder.getConnectionsError();
        return totalConnectionsDone == dataHolder.getConnections();
    }

    private void connectToNode(SelectionKey key, Map<String, String> requestMessage, Command userCommand) throws IOException {
        final String phase = requestMessage.get("phase");
        final String nodeName = requestMessage.get("nodename");
        final ServerNode serverNode = key.attachment() == null
                ? getNodeByName(nodeName)
                : (ServerNode) key.attachment();

        logger.info(phase.concat(" at ").concat(this.nodeName).concat(" with ").concat(nodeName));

        if (!phase.equals("ok")) {
            performServerHandshake(key, requestMessage, userCommand, phase, serverNode);
        } else {
            // Handshake performed, update connections and set to wait for data.
            SocketChannel node = (SocketChannel) key.channel();
            updateNodeConnection(nodeName, true, node);

            node.register(selector, SelectionKey.OP_READ, serverNode);
        }
    }

    private Path getNodeCertificate(String nodeName) {
        final URL nodeCertificate = NodeServer.class.getResource(nodeName.concat(".crt"));
        Path path;

        try {
            path = Paths.get(nodeCertificate.toURI());
        } catch (URISyntaxException ex) {
            path = null;
        }

        return path;
    }

    private void performServerHandshake(SelectionKey key, Map<String, String> requestMessage, Command userCommand, String phase, ServerNode node) throws IOException {
        final SocketChannel client = (SocketChannel) key.channel();
        final String signature = requestMessage.get("signature");

        Path path = getNodeCertificate(node.getName());

        if (path != null
                && NodeUtils.verifyNodeSignature(KeyLoader.loadPublicKey(path), userCommand.name(), node.getName(), signature)) {

            String currentPhase;

            if (phase.equals("handshake_1")) {
                currentPhase = "handshake_2"; // Sending node_1 the 2nd handshake as first handshake is already performed.
            } else {
                currentPhase = "ok"; // Sending node_2 the confirmation which will confirm the handshake on both sides.
            }

            final String message = NodeUtils.generateNodeHandshakeMessage(nodeKeys.getPrivateKey(), this.nodeName, currentPhase);

            if (currentPhase.equals("ok")) {
                updateNodeConnection(node.getName(), true, client);
                client.register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, node, true));
            } else {
                client.register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, node));
            }
        } else {
            client.close();
            key.cancel();
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

    private void processHistoryOrBalanceRequest(Map<String, String> requestMessage, Command userCommand, SocketChannel client) throws IOException {
        final String signature = requestMessage.get("signature");
        final String base64PublicKey = requestMessage.get("key");
        final PublicKey clientPublicKey = KeyLoader.decodePublicKey(base64PublicKey);

        if (NodeUtils.verifyWalletSignature(clientPublicKey, signature)) {
            final String data = (userCommand.equals(Command.HISTORY))
                    ? Ledger.getUserHistoryAsString(nodeName, base64PublicKey)
                    : Ledger.getUserBalance(nodeName, base64PublicKey).getLineBalance();

            final String responseData = NodeUtils.generateWalletMessage(nodeKeys.getPrivateKey(), data);
            client.register(selector, SelectionKey.OP_WRITE, new SuccessMessage(responseData, "Wallet - ".concat(client.getLocalAddress().toString()), true));
        } else {
            client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid signature", client.getLocalAddress().toString()));
        }
    }

    private String generateTransactionHash(String senderKey, String receiverKey, String guid, String amount, String signature, String timestamp, String nodeName) {
        final String senderLastHash = Ledger.getUserLastTransaction(this.nodeName, senderKey).getHash();
        String receiverLastHash = Ledger.getUserLastTransaction(nodeName, receiverKey).getHash();

        return new HashBuilder(senderLastHash)
                .addData(receiverLastHash)
                .addData(guid)
                .addData(amount)
                .addData(signature)
                .addData(timestamp)
                .addData(nodeName)
                .hash();
    }

    private void processTransferRequest(Map<String, String> requestMessage, String command, SocketChannel client) throws IOException {
        final String destinationKey = requestMessage.get("destinationkey");
        final String guid = requestMessage.get("guid");
        final String stringAmount = requestMessage.get("amount");
        final String signature = requestMessage.get("signature");
        final String base64PublicKey = requestMessage.get("key");
        final PublicKey clientPublicKey = KeyLoader.decodePublicKey(base64PublicKey);

        if (NodeUtils.verifyTransferSignature(clientPublicKey, base64PublicKey, signature, destinationKey, guid, stringAmount)) {
            final float amount = Float.parseFloat(stringAmount);
            final float userBalance = Ledger.getUserBalance(nodeName, base64PublicKey).calculateBalance();

            if (userBalance >= amount) {
                final long timestamp = Instant.now().toEpochMilli();
                final String transactionHash = generateTransactionHash(base64PublicKey,
                        destinationKey,
                        guid,
                        Conversion.convertAmountToString(amount),
                        signature,
                        String.valueOf(timestamp),
                        nodeName);

                List<ServerNode> serverNodes = getConnectedNodes();
                if (serverNodes.size() >= 2) {
                    String timestampString = String.valueOf(timestamp);
                    // Send request to nodes
                    sendVerificationRequests(serverNodes,
                            serverNodes.size(),
                            Command.VERIFY.name(),
                            guid,
                            base64PublicKey,
                            destinationKey,
                            stringAmount,
                            signature,
                            timestampString,
                            transactionHash);
                    if (waitForVerificationProcess(base64PublicKey)) {
                        DataHolder dataHolder = verificationMap.get(base64PublicKey);
                        List<String> signatures = dataHolder.getSignatures();

                        if (signatures.size() >= 2) {
                            final String signatureOne = nodeName.concat(":").concat(NodeUtils.generateNodeTransferSignature(
                                    nodeKeys.getPrivateKey(),
                                    guid,
                                    base64PublicKey,
                                    destinationKey,
                                    stringAmount,
                                    signature,
                                    timestampString,
                                    transactionHash,
                                    nodeName));
                            final String signatureTwo = signatures.get(0);
                            final String signatureThree = signatures.get(1);

                            final String message = NodeUtils.generateWalletTransferMessage(
                                    nodeKeys.getPrivateKey(),
                                    guid,
                                    base64PublicKey,
                                    destinationKey,
                                    stringAmount,
                                    signature,
                                    timestampString,
                                    transactionHash,
                                    nodeName,
                                    signatureOne,
                                    signatureTwo,
                                    signatureThree);

                            client.register(selector, SelectionKey.OP_WRITE,
                                    new SuccessMessage(message, client.getLocalAddress().toString(), false));
                        } else {
                            client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Verification failed", client.getLocalAddress().toString()));
                        }
                    } else {
                        client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Node connection timed out.", client.getLocalAddress().toString()));
                    }
                } else {
                    client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Unable to fulfill your request. Please try again later.", client.getLocalAddress().toString()));
                }
            } else {
                client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Insufficient Funds", client.getLocalAddress().toString()));
            }
        } else {
            client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid signature", client.getLocalAddress().toString()));
        }
    }

    private void sendVerificationRequests(List<ServerNode> serverNodes,
                                          int serverConnections,
                                          String command,
                                          String guid,
                                          String senderKey,
                                          String receiverKey,
                                          String amount,
                                          String senderSignature,
                                          String timestamp,
                                          String hash) {
        DataHolder dataHolder = new DataHolder(serverConnections);
        verificationMap.put(senderKey, dataHolder);
        serverNodes.forEach(serverNode -> {
            final String message = NodeUtils.generateNodeVerifyMessage(nodeKeys.getPrivateKey(), command,
                    guid, senderKey, receiverKey, amount, senderSignature, timestamp, hash, this.nodeName);
            try {
                serverNode.getSocketChannel().register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, new ServerNodeVerify(serverNode, senderKey)));
            } catch (ClosedChannelException ex) {
                verificationMap.get(senderKey).incrementError();
            }
        });
    }

    private void registerClient(SelectionKey key) throws IOException {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        final SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        logger.info("Accepted connection: ".concat(client.getLocalAddress().toString()));
    }

    private void writeToNode(SelectionKey key) throws IOException {
        final SocketChannel client = (SocketChannel) key.channel();
        final NodeMessage message = (NodeMessage) key.attachment();

        logger.info("To: ".concat(message.getServerNode().getName())
                .concat(" Sending: ".concat(message.getMessage())));

        try {
            writeToClient(message.getMessage(), client);
        } catch (IOException ex) {
            logger.info("System interrupted while writing request to node: ".concat(message.getServerNode().getName())
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
            logger.info("System interrupted while writing request to wallet: ".concat(receiverName).concat(". \n")
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

    private void triggerConnectionToServerNodes() {
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

    private void updateNodeConnection(String nodeName, boolean isConnected, SocketChannel client) {
        Optional<ServerNode> serverNodeOptional = this.serverNodes.parallelStream()
                .filter(serverNode -> serverNode.getName().equals(nodeName))
                .findFirst();

        if (serverNodeOptional.isPresent()) {
            ServerNode serverNode = serverNodeOptional.get();
            serverNode.setSocketChannel(client);
            serverNode.setConnected(isConnected);
        }

        logger.info("Updating server node list. New list: ".concat(serverNodes.toString()));
    }

    private ServerNode getNodeByName(String name) {
        return this.serverNodes.parallelStream()
                .filter(serverNode -> serverNode.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private List<ServerNode> getConnectedNodes() {
        return this.serverNodes.parallelStream()
                .filter(ServerNode::isConnected)
                .collect(Collectors.toList());
    }
}
