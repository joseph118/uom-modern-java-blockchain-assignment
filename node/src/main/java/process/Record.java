package process;

import core.message.node.NodeMessage;
import data.ServerNode;
import data.Transaction;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

public class Record {
    private final static Logger logger = Logger.getLogger(Record.class);

    private Record() {

    }

    public static void processRecordRequest(SelectionKey key, Map<String, String> requestMessage, PrivateKey privateKey, String nodeName) {
        final SocketChannel client = (SocketChannel) key.channel();
        final Selector selector = key.selector();
        final Transaction transaction = Transaction.mapResponseToTransaction(requestMessage);

    }

    public static void triggerTransactionConfirmation(Selector selector,
                                                      List<ServerNode> connectedNodes,
                                                      String nodeName,
                                                      String message) {

        if (!connectedNodes.isEmpty()) {
            connectedNodes.forEach(serverNode -> {
                try {
                    serverNode.getSocketChannel().register(selector, SelectionKey.OP_WRITE, new NodeMessage(message, serverNode));
                } catch (IOException e) {
                    logger.info(nodeName.concat(" - Error when sending confirm request to ").concat(serverNode.toString()));
                }
            });
        }
    }
}
