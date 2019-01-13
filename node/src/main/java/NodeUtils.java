import model.Command;
import security.SignatureBuilder;
import security.SignatureVerifier;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

public class NodeUtils {
    private NodeUtils() {

    }

    public static boolean verifyTransferSignature(PublicKey key,
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

    public static boolean isRequestArgumentsValid(Map<String, String> map) {
        if (map.containsKey("command") && map.containsKey("signature")) {
            if (map.get("command").equals(Command.CONNECT.name())) {
                // Server Node
                return map.containsKey("nodename") && map.containsKey("phase");
            } else {
                // Wallet
                if (map.containsKey("key")) {
                    if (map.get("command").equals(Command.TRANSFER.name())) {
                        return map.containsKey("destinationKey")
                                && map.containsKey("amount")
                                && map.containsKey("guid");
                    } else {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean verifyNodeSignature(PublicKey nodePublicKey, String command, String nodeName, String signature, String phase) {
        final SignatureVerifier signatureVerifier = new SignatureVerifier(nodePublicKey)
                .addData(command)
                .addData(nodeName)
                .addData(phase);

        return signatureVerifier.verify(signature);
    }

    public static boolean verifyUserSignature(PublicKey key,
                                               String signature) {
        final SignatureVerifier signatureVerifier = new SignatureVerifier(key);

        return signatureVerifier.verify(signature);
    }

    public static String generateNodeMessage(PrivateKey privateKey, String nodeName, String phase) {
        final String command = Command.CONNECT.name();
        final String signature = generateNodeSignature(privateKey, command, nodeName, phase);

        return "command=".concat(command)
                .concat(",nodename=").concat(nodeName)
                .concat(",signature=").concat(signature)
                .concat(",phase=").concat(phase);
    }

    public static String generateWalletMessage(PrivateKey privateKey, String data) {
        final String signature = generateSignature(privateKey, data);
        final String encodedData = new String(Base64.getEncoder().encode(data.getBytes()));

        return "payload="
                .concat(encodedData)
                .concat(",signature=")
                .concat(signature);
    }

    private static String generateSignature(PrivateKey privateKey, String data) {
        final SignatureBuilder signatureBuilder = new SignatureBuilder(privateKey)
                .addData(data);

        return signatureBuilder.sign();
    }

    public static String generateNodeSignature(PrivateKey privateKey, String command, String nodeName, String phase) {
        final SignatureBuilder signatureBuilder = new SignatureBuilder(privateKey)
                .addData(command)
                .addData(nodeName)
                .addData(phase);

        return signatureBuilder.sign();
    }
}
