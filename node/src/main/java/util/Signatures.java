package util;

import security.SignatureBuilder;
import security.SignatureVerifier;

import java.security.PrivateKey;
import java.security.PublicKey;

public class Signatures {
    public static boolean verifyNodeSignature(PublicKey nodePublicKey, String command, String nodeName, String signature) {
        final SignatureVerifier signatureVerifier = new SignatureVerifier(nodePublicKey)
                .addData(command)
                .addData(nodeName);

        return signatureVerifier.verify(signature);
    }

    public static boolean verifyWalletSignature(PublicKey key,
                                                String signature) {
        final SignatureVerifier signatureVerifier = new SignatureVerifier(key);

        return signatureVerifier.verify(signature);
    }

    public static boolean verifyWalletTransferSignature(PublicKey key,
                                                        String publicKey,
                                                        String signature,
                                                        String destinationKey,
                                                        String guid,
                                                        String amount) {

        SignatureVerifier signatureVerifier = new SignatureVerifier(key);
        signatureVerifier.addData(guid)
                .addData(publicKey)
                .addData(destinationKey)
                .addData(amount);

        return signatureVerifier.verify(signature);
    }



    public static String generateSignature(PrivateKey privateKey, String data) {
        final SignatureBuilder signatureBuilder = new SignatureBuilder(privateKey)
                .addData(data);

        return signatureBuilder.sign();
    }

    public static String generateNodeSignature(PrivateKey privateKey, String command, String nodeName) {
        final SignatureBuilder signatureBuilder = new SignatureBuilder(privateKey)
                .addData(command)
                .addData(nodeName);

        return signatureBuilder.sign();
    }

    public static String generateWalletTransferSignature(PrivateKey privateKey,
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

        final SignatureBuilder signatureBuilder = basicTransferSignatureDetails(privateKey, guid, senderKey,
                receiverKey, amount, senderSignature, timestamp, hash, nodeName)
                .addData(signatureOne)
                .addData(signatureTwo)
                .addData(signatureThree);

        return signatureBuilder.sign();
    }

    public static String generateNodeTransferSignature(PrivateKey privateKey,
                                                       String guid,
                                                       String senderKey,
                                                       String receiverKey,
                                                       String amount,
                                                       String senderSignature,
                                                       String timestamp,
                                                       String hash,
                                                       String nodeName) {

        final SignatureBuilder signatureBuilder = basicTransferSignatureDetails(privateKey, guid, senderKey,
                receiverKey, amount, senderSignature, timestamp, hash, nodeName);

        return signatureBuilder.sign();

    }

    private static SignatureBuilder basicTransferSignatureDetails(PrivateKey privateKey,
                                                                   String guid,
                                                                   String senderKey,
                                                                   String receiverKey,
                                                                   String amount,
                                                                   String senderSignature,
                                                                   String timestamp,
                                                                   String hash,
                                                                   String nodeName) {
        return new SignatureBuilder(privateKey)
                .addData(guid)
                .addData(senderKey)
                .addData(receiverKey)
                .addData(amount)
                .addData(senderSignature)
                .addData(timestamp)
                .addData(hash)
                .addData(nodeName);
    }
}
