package process;

import core.message.wallet.ErrorMessage;
import core.message.wallet.SuccessMessage;
import data.Ledger;
import security.KeyLoader;
import data.Command;
import util.Messages;
import util.Signatures;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

public class SimpleRequest {
    public static void processHistoryOrBalanceRequest(SelectionKey key, Map<String, String> requestMessage, Command userCommand, String nodeName, PrivateKey privateKey) throws IOException {
        final SocketChannel client = (SocketChannel) key.channel();
        final Selector selector = key.selector();
        final String signature = requestMessage.get("signature");
        final String base64PublicKey = requestMessage.get("key");
        final PublicKey clientPublicKey = KeyLoader.decodePublicKey(base64PublicKey);

        if (Signatures.verifyWalletSignature(clientPublicKey, signature)) {
            final String data = (userCommand.equals(Command.HISTORY))
                    ? Ledger.getUserHistoryAsString(nodeName, base64PublicKey)
                    : Ledger.getUserBalance(nodeName, base64PublicKey).getLineBalance();

            final String responseData = Messages.generateWalletMessage(privateKey, data);
            client.register(selector, SelectionKey.OP_WRITE, new SuccessMessage(responseData, "Wallet - ".concat(client.getLocalAddress().toString()), true));
        } else {
            client.register(selector, SelectionKey.OP_WRITE, new ErrorMessage("Invalid signature", client.getLocalAddress().toString()));
        }
    }
}
