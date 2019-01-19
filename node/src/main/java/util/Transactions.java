package util;

import data.Ledger;
import org.apache.log4j.Logger;
import security.HashBuilder;

public class Transactions {
    final static Logger logger = Logger.getLogger(Transactions.class);

    public static String generateTransactionHash(String senderKey, String receiverKey, String guid, String amount, String senderSignature, String timestamp, String nodeName) {
        final String senderLastHash = Ledger.getUserLastTransaction(nodeName, senderKey).getHash();
        String receiverLastHash = Ledger.getUserLastTransaction(nodeName, receiverKey).getHash();

        return new HashBuilder(senderLastHash)
                .addData(receiverLastHash)
                .addData(guid)
                .addData(amount)
                .addData(senderSignature)
                .addData(timestamp)
                .addData(nodeName)
                .hash();
    }
}
