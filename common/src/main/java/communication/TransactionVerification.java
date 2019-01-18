package communication;

import data.Transaction;
import security.KeyLoader;
import security.SignatureVerifier;
import util.Resource;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;

public class TransactionVerification {
    public static boolean isTransactionValid(Transaction transaction) {
        return verifySignature(transaction.getVerificationSignature1(), transaction)
                && verifySignature(transaction.getVerificationSignature2(), transaction)
                && verifySignature(transaction.getVerificationSignature3(), transaction);
    }

    private static boolean verifySignature(String signature, Transaction transaction) {
        final String[] data = signature.split(":", 2);
        final String nodeName = data[0];
        final String nodeSignature = data[1];

        URL url = Resource.getResource(nodeName, ".crt");
        try {
            final Path nodeCertificatePath = Paths.get(url.toURI());
            final PublicKey nodePublicKey = KeyLoader.loadPublicKey(nodeCertificatePath);

            return GlobalSignatures.isNodeVerifiedTransactionSignatureValid(nodePublicKey, nodeSignature, transaction.getGuid(), transaction.getSenderPublicKey(), transaction.getRecipientPublicKey(), transaction.getStringTransactionAmount(), transaction.getSenderAuthorisationSignature(), transaction.getStringTimestamp(), transaction.getHash(), nodeName);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return false;
    }

    public static boolean isNodeVerifiedTransactionSignatureValid(PublicKey key,
                                                                  String signature,
                                                                  String nodeName,
                                                                  Transaction transaction) {

        final SignatureVerifier signatureVerifier = new SignatureVerifier(key)
                .addData(transaction.getGuid())
                .addData(transaction.getSenderPublicKey())
                .addData(transaction.getRecipientPublicKey())
                .addData(transaction.getStringTransactionAmount())
                .addData(transaction.getSenderAuthorisationSignature())
                .addData(transaction.getStringTimestamp())
                .addData(transaction.getHash())
                .addData(nodeName)
                .addData(transaction.getVerificationSignature1())
                .addData(transaction.getVerificationSignature2())
                .addData(transaction.getVerificationSignature3());

        return signatureVerifier.verify(signature);
    }
}
