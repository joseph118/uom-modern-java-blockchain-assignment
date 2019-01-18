package communication;

import data.Transaction;
import security.SignatureBuilder;
import security.SignatureVerifier;

import java.security.PrivateKey;
import java.security.PublicKey;

public class GlobalSignatures {
    private GlobalSignatures() {}


    public static boolean isNodeVerifiedTransactionSignatureValid(PublicKey key,
                                                                  String signature,
                                                                  String guid,
                                                                  String senderKey,
                                                                  String receiverKey,
                                                                  String amount,
                                                                  String senderSignature,
                                                                  String timestamp,
                                                                  String hash,
                                                                  String nodeName) {
        final SignatureVerifier signatureVerifier = new SignatureVerifier(key)
                .addData(guid)
                .addData(senderKey)
                .addData(receiverKey)
                .addData(amount)
                .addData(senderSignature)
                .addData(timestamp)
                .addData(hash)
                .addData(nodeName);

        return signatureVerifier.verify(signature);
    }

    public static SignatureBuilder generateTransactionSignature(PrivateKey key, Transaction transaction) {
        return new SignatureBuilder(key)
                .addData(transaction.getGuid())
                .addData(transaction.getSenderPublicKey())
                .addData(transaction.getRecipientPublicKey())
                .addData(transaction.getStringTransactionAmount())
                .addData(transaction.getSenderAuthorisationSignature())
                .addData(transaction.getStringTimestamp())
                .addData(transaction.getHash());
    }

    public static String generateVerifiedTransactionSignature(PrivateKey key, Transaction transaction) {
        final SignatureBuilder signatureBuilder = generateTransactionSignature(key, transaction)
                .addData(transaction.getVerificationSignature1())
                .addData(transaction.getVerificationSignature2())
                .addData(transaction.getVerificationSignature3());

        return signatureBuilder.sign();
    }

    public static String generateConfirmedTransactionSignature(PrivateKey key, Transaction transaction) {
        final SignatureBuilder signatureBuilder = generateTransactionSignature(key, transaction)
                .addData(transaction.getVerificationSignature1())
                .addData(transaction.getVerificationSignature2())
                .addData(transaction.getVerificationSignature3())
                .addData(transaction.getConfirmationSignature());

        return signatureBuilder.sign();
    }

    public static boolean isVerifiedTransactionSignatureValid(PublicKey key,
                                                              String signature,
                                                              Transaction transaction) {

        final SignatureVerifier signatureVerifier = new SignatureVerifier(key)
                .addData(transaction.getGuid())
                .addData(transaction.getSenderPublicKey())
                .addData(transaction.getRecipientPublicKey())
                .addData(transaction.getStringTransactionAmount())
                .addData(transaction.getSenderAuthorisationSignature())
                .addData(transaction.getStringTimestamp())
                .addData(transaction.getHash())
                .addData(transaction.getVerificationSignature1())
                .addData(transaction.getVerificationSignature2())
                .addData(transaction.getVerificationSignature3());

        return signatureVerifier.verify(signature);
    }
}
