package process;

import communication.GlobalSignatures;
import core.message.node.NodeMessage;
import core.message.wallet.ErrorMessage;
import data.NodeDataRequest;
import data.ServerNode;
import data.Transaction;
import org.apache.log4j.Logger;
import security.KeyLoader;
import util.Resource;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

public class Record {
    private final static Logger logger = Logger.getLogger(Record.class);

    private Record() {

    }

    public static void processRecordRequest(SelectionKey key, Map<String, String> requestMessage, PrivateKey privateKey, String nodeName) throws IOException {
        final String senderNodeName = requestMessage.get("nodename");
        final String signature = requestMessage.get("signature");
        Path path = Resource.getNodeCertificate(senderNodeName);

        if (path != null) {
            final SocketChannel client = (SocketChannel) key.channel();
            final Selector selector = key.selector();
            final Transaction transaction = Transaction.mapResponseToTransaction(requestMessage);
            final PublicKey senderNodePublicKey = KeyLoader.loadPublicKey(path);

            if (GlobalSignatures.isRecordSignatureValid(senderNodePublicKey, signature, transaction)) {

            } else {
                // Invalid signature
                client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid signature", client.getLocalAddress().toString()));
            }
        } else {
            key.cancel();
            key.channel().close();
        }

    }

    public static void triggerTransactionConfirmation(Selector selector,
                                                      List<ServerNode> connectedNodes,
                                                      String nodeName,
                                                      String message,
                                                      Map<String, NodeDataRequest> nodeDataRequestMap,
                                                      String senderKey) {

        if (!connectedNodes.isEmpty()) {
            final NodeDataRequest nodeDataRequest = new NodeDataRequest(connectedNodes.size());
            nodeDataRequestMap.put(senderKey, nodeDataRequest);
            connectedNodes.forEach(serverNode -> {
                try {
                    serverNode.getSocketChannel().register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, serverNode));
                } catch (IOException e) {
                    logger.info(nodeName.concat(" - Error when sending confirm request to ").concat(serverNode.toString()));
                    nodeDataRequestMap.get(senderKey).incrementErrorResponse();
                }
            });
        }
    }
}
