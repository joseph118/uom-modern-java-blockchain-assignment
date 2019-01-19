package process;

import communication.GlobalSignatures;
import communication.TransactionVerification;
import core.message.wallet.ErrorMessage;
import core.message.wallet.SuccessMessage;
import data.Ledger;
import data.NodeDataRequest;
import data.ServerNode;
import data.Transaction;
import org.apache.log4j.Logger;
import security.KeyLoader;
import util.Messages;
import util.RequestVerification;
import util.Response;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

public class Confirmation {
    private final static Logger logger = Logger.getLogger(Transfer.class);

    private Confirmation() {}

    public static void processTransferConfirmation(SelectionKey key,
                                                   Map<String, String> requestMessage,
                                                   PrivateKey privateKey,
                                                   String nodeName,
                                                   List<ServerNode> connectedNodes,
                                                   Map<String, NodeDataRequest> nodeDataRequestMap) throws IOException {

        final SocketChannel client = (SocketChannel) key.channel();
        final Selector selector = key.selector();

        String senderKey = requestMessage.getOrDefault("key", "");

        if (Response.isWalletConfirmResponseValid(requestMessage)) {
            final PublicKey walletKey = KeyLoader.decodePublicKey(requestMessage.get("key"));
            final String signature = requestMessage.get("signature");
            final Transaction transaction = Transaction.mapResponseToTransaction(requestMessage, "signature");

            if (GlobalSignatures.isVerifiedTransactionSignatureValid(walletKey, signature, transaction)) {
                if (TransactionVerification.isTransactionValid(transaction)) {
                    final String nodeMessage = Messages.generateNodeConfirmationMessage(privateKey, nodeName, transaction);
                    Record.triggerTransactionConfirmation(selector, connectedNodes, nodeName, nodeMessage, nodeDataRequestMap, transaction.getSenderPublicKey());

                    if (RequestVerification.waitForVerificationProcess(transaction.getSenderPublicKey(), nodeDataRequestMap)) {
                        Ledger.addTransaction(transaction, nodeName);

                        final String walletMessage = Messages.generateWalletConfirmationMessage(privateKey, nodeName, transaction);
                        client.register(selector, SelectionKey.OP_WRITE, new SuccessMessage(walletMessage, client.getLocalAddress().toString(), true));
                    } else {
                        client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Error while adding new record", client.getLocalAddress().toString()));
                    }

                    nodeDataRequestMap.remove(transaction.getSenderPublicKey());
                } else {
                    // Invalid node verification
                    client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid node verifications", client.getLocalAddress().toString()));
                }
            } else {
                // Invalid signature
                client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid signature", client.getLocalAddress().toString()));
            }
        } else {
            // Invalid response
            client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid response", client.getLocalAddress().toString()));
        }

        nodeDataRequestMap.remove(senderKey);
    }
}
