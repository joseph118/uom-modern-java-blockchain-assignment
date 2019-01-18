package process;

import communication.GlobalSignatures;
import communication.TransactionVerification;
import core.message.wallet.ErrorMessage;
import data.ServerNode;
import data.Transaction;
import org.apache.log4j.Logger;
import security.KeyLoader;
import util.Messages;
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
                                                   List<ServerNode> connectedNodes ) throws IOException {

        final SocketChannel client = (SocketChannel) key.channel();
        final Selector selector = key.selector();

        if (Response.isWalletConfirmResponseValid(requestMessage)) {
            final PublicKey walletKey = KeyLoader.decodePublicKey(requestMessage.get("key"));
            final String signature = requestMessage.get("signature");
            final Transaction transaction = Transaction.mapResponseToTransaction(requestMessage);

            if (GlobalSignatures.isVerifiedTransactionSignatureValid(walletKey, signature, transaction)) {
                if (TransactionVerification.isTransactionValid(transaction)) {
                    final String message = Messages.generateNodeConfirmationMessage(privateKey, nodeName, transaction);
                    Record.triggerTransactionConfirmation(selector, connectedNodes, nodeName, message);

                    // TODO --- save. -- wait --- send client ok if ok.

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
    }
}
