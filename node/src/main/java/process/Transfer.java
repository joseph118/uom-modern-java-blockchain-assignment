package process;

import core.message.wallet.ErrorMessage;
import core.message.wallet.SuccessMessage;
import data.Command;
import data.Ledger;
import data.ServerNode;
import data.NodeDataRequest;
import org.apache.log4j.Logger;
import security.KeyLoader;
import util.*;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class Transfer {
    private final static Logger logger = Logger.getLogger(Transfer.class);

    private Transfer() {}

    public static void processTransferRequest(SelectionKey key, Map<String, String> requestMessage, String nodeName,
                                              List<ServerNode> connectedNodes, PrivateKey privateKey,
                                              Map<String, NodeDataRequest> dataMap) throws IOException {

        final String destinationKey = requestMessage.get("destinationkey");
        final String guid = requestMessage.get("guid");
        final String stringAmount = requestMessage.get("amount");
        final String signature = requestMessage.get("signature");
        final String base64PublicKey = requestMessage.get("key");
        final PublicKey clientPublicKey = KeyLoader.decodePublicKey(base64PublicKey);
        final Selector selector = key.selector();
        final SocketChannel client = (SocketChannel) key.channel();

        if (Signatures.verifyWalletTransferSignature(clientPublicKey, base64PublicKey, signature, destinationKey, guid, stringAmount)) {
            final float amount = Float.parseFloat(stringAmount);
            final float userBalance = Ledger.getUserBalance(nodeName, base64PublicKey).calculateBalance();

            if (userBalance >= amount) {
                final long timestamp = Instant.now().toEpochMilli();
                final String transactionHash = Transactions.generateTransactionHash(base64PublicKey,
                        destinationKey,
                        guid,
                        Parser.convertAmountToString(amount),
                        signature,
                        String.valueOf(timestamp),
                        nodeName);

                if (connectedNodes.size() >= 2) {
                    String timestampString = String.valueOf(timestamp);
                    // Send verification to nodes
                    Verification.sendVerificationRequests(
                            key,
                            connectedNodes,
                            privateKey,
                            dataMap,
                            nodeName,
                            Command.VERIFY.name(),
                            guid,
                            base64PublicKey,
                            destinationKey,
                            stringAmount,
                            signature,
                            timestampString,
                            transactionHash);
                    if (RequestVerification.waitForVerificationProcess(base64PublicKey, dataMap)) {
                        NodeDataRequest nodeDataRequest = dataMap.get(base64PublicKey);
                        List<String> signatures = nodeDataRequest.getData();

                        if (signatures.size() >= 2) {
                            final String signatureOne = nodeName.concat(":").concat(Signatures.generateNodeTransferSignature(
                                    privateKey,
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

                            final String message = Messages.generateWalletTransferMessage(
                                    privateKey,
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
                    client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Unable to fulfill your verification. Please try again later.", client.getLocalAddress().toString()));
                }
            } else {
                client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Insufficient Funds", client.getLocalAddress().toString()));
            }
        } else {
            client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid signature", client.getLocalAddress().toString()));
        }
    }
}
