package util;

import data.Ledger;
import security.HashBuilder;

public class Transactions {
    public static String generateTransactionHash(String senderKey, String receiverKey, String guid, String amount, String signature, String timestamp, String nodeName) {
        final String senderLastHash = Ledger.getUserLastTransaction(nodeName, senderKey).getHash();
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
}
