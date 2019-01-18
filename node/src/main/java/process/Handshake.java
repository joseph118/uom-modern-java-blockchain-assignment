package process;

import data.ServerNodes;
import security.KeyLoader;
import data.Command;
import core.message.node.NodeMessage;
import data.ServerNode;
import org.apache.log4j.Logger;
import util.Resource;
import util.Signatures;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Map;

public class Handshake {
    private final static Logger logger = Logger.getLogger(Handshake.class);

    private Handshake() {}

    public static void connectToNode(SelectionKey key,
                              Map<String, String> requestMessage,
                              Command userCommand,
                              String nodeName,
                              PrivateKey privateKey,
                              ServerNodes serverNodes) throws IOException {

        Selector selector = key.selector();
        final String phase = requestMessage.get("phase");
        final String senderNodeName = requestMessage.get("nodename");
        final ServerNode serverNode = key.attachment() == null
                ? serverNodes.getNodeByName(senderNodeName)
                : (ServerNode) key.attachment();

        logger.info(phase.concat(" at ").concat(nodeName).concat(" with ").concat(senderNodeName));

        if (!phase.equals("ok")) {
            Handshake.performServerHandshake(key, requestMessage, userCommand, phase,
                    serverNode, privateKey, nodeName, serverNodes);
        } else {
            // Handshake performed, update connections and set to wait for data.
            SocketChannel node = (SocketChannel) key.channel();
            serverNodes.updateNodeConnection(senderNodeName, true, node);

            node.register(selector, SelectionKey.OP_READ, serverNode);
        }
    }

    public static void connectToServerNode(SelectionKey key, String nodeName, PrivateKey privateKey) throws IOException {
        SocketChannel nodeClient = (SocketChannel) key.channel();
        Selector selector = key.selector();
        ServerNode serverNode = (ServerNode) key.attachment();

        try {
            if (nodeClient.finishConnect()) {
                nodeClient.configureBlocking(false);
                nodeClient.socket().setKeepAlive(true);

                logger.info("Connected to: ".concat(serverNode.toString().concat(", Performing handshake.")));

                final String message = generateNodeHandshakeMessage(privateKey, nodeName, "handshake_1");
                nodeClient.register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, serverNode));
            }
        } catch (Exception e) {
            key.cancel();
            nodeClient.close();

            logger.info("Unable to to connect to: ".concat(serverNode.toString()));
        }
    }

    private static void performServerHandshake(SelectionKey key,
                                               Map<String, String> requestMessage,
                                               Command userCommand,
                                               String phase,
                                               ServerNode node,
                                               PrivateKey privateKey,
                                               String nodeName,
                                               ServerNodes serverNodes) throws IOException {

        final SocketChannel client = (SocketChannel) key.channel();
        final Selector selector = key.selector();
        final String signature = requestMessage.get("signature");

        Path path = Resource.getNodeCertificate(node.getName());

        if (path != null
                && Signatures.verifyNodeSignature(KeyLoader.loadPublicKey(path), userCommand.name(), node.getName(), signature)) {

            String currentPhase;

            if (phase.equals("handshake_1")) {
                currentPhase = "handshake_2"; // Sending node_1 the 2nd handshake as first handshake is already performed.
            } else {
                currentPhase = "ok"; // Sending node_2 the confirmation which will confirm the handshake on both sides.
            }

            final String message = generateNodeHandshakeMessage(privateKey, nodeName, currentPhase);

            if (currentPhase.equals("ok")) {
                serverNodes.updateNodeConnection(node.getName(), true, client);
                client.register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, node));
            } else {
                client.register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, node));
            }
        } else {
            client.close();
            key.cancel();
        }
    }

    private static String generateNodeHandshakeMessage(PrivateKey privateKey, String nodeName, String phase) {
        final String command = Command.NODE_CONNECT.name();
        final String signature = Signatures.generateNodeSignature(privateKey, command, nodeName);

        return "command=".concat(command)
                .concat(",nodename=").concat(nodeName)
                .concat(",signature=").concat(signature)
                .concat(",phase=").concat(phase);
    }
}
