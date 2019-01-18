package util;

import communication.GlobalSignatures;
import data.Command;
import data.Transaction;

import java.security.PrivateKey;
import java.util.Base64;

public class Messages {
    public static String generateNodeVerifyMessage(PrivateKey privateKey,
                                                   String command,
                                                   String guid,
                                                   String senderKey,
                                                   String receiverKey,
                                                   String amount,
                                                   String senderSignature,
                                                   String timestamp,
                                                   String hash,
                                                   String nodeName) {
        final String signature = Signatures.generateNodeTransferSignature(privateKey, guid, senderKey,
                receiverKey, amount, senderSignature, timestamp, hash, nodeName);

        return "command=".concat(command)
                .concat(",nodename=").concat(nodeName)
                .concat(",signature=").concat(signature)
                .concat(",guid=").concat(guid)
                .concat(",senderkey=").concat(senderKey)
                .concat(",receiverkey=").concat(receiverKey)
                .concat(",amount=").concat(amount)
                .concat(",sendersignature=").concat(senderSignature)
                .concat(",timestamp=").concat(timestamp)
                .concat(",hash=").concat(hash);
    }

    public static String generateNodeVerifyErrorMessage(PrivateKey privateKey, String nodeName, Command command) {
        final String signature = Signatures.generateNodeSignature(privateKey, command.name(), nodeName);

        return "command=".concat(command.name())
                .concat(",nodename=").concat(nodeName)
                .concat(",signature=").concat(signature);
    }

    public static String generateWalletMessage(PrivateKey privateKey, String data) {
        final String signature = Signatures.generateSignature(privateKey, data);
        final String encodedData = new String(Base64.getEncoder().encode(data.getBytes()));

        return "payload="
                .concat(encodedData)
                .concat(",signature=")
                .concat(signature);
    }

    public static String generateWalletTransferMessage(PrivateKey privateKey,
                                                       String guid,
                                                       String senderKey,
                                                       String receiverKey,
                                                       String amount,
                                                       String senderSignature,
                                                       String timestamp,
                                                       String hash,
                                                       String nodeName,
                                                       String signatureOne,
                                                       String signatureTwo,
                                                       String signatureThree) {

        final String signature = Signatures.generateWalletTransferSignature(privateKey, guid, senderKey, receiverKey,
                amount, senderSignature, timestamp, hash, nodeName, signatureOne, signatureTwo, signatureThree);

        return "signature=".concat(signature)
                .concat(",guid=").concat(guid)
                .concat(",senderkey=").concat(senderKey)
                .concat(",receiverkey=").concat(receiverKey)
                .concat(",amount=").concat(amount)
                .concat(",sendersignature=").concat(senderSignature)
                .concat(",timestamp=").concat(timestamp)
                .concat(",hash=").concat(hash)
                .concat(",nodename=").concat(nodeName)
                .concat(",signature1=").concat(signatureOne)
                .concat(",signature2=").concat(signatureTwo)
                .concat(",signature3=").concat(signatureThree);
    }

    public static String generateNodeConfirmationMessage(PrivateKey privateKey, String nodeName, Transaction transaction) {
        final String signature = GlobalSignatures.generateConfirmedTransactionSignature(privateKey, transaction);

        return "signature=".concat(signature)
                .concat(",nodename=").concat(nodeName)
                .concat(",command=").concat(Command.RECORD.name()).concat(",")
                .concat(transaction.toTransactionConfirmationResponseRow());
    }
}
