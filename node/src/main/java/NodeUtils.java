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
                                            String command,
                                            String signature,
                                            String destinationKey,
                                            String guid,
                                            String amount) {

        SignatureVerifier signatureVerifier = new SignatureVerifier(key);
        signatureVerifier.addData(command)
                .addData(amount)
                .addData(destinationKey)
                .addData(guid);

        return signatureVerifier.verify(signature);
    }

    public static boolean isRequestArgumentsValid(Map<String, String> map) {
        if (map.containsKey("signature")
                && map.containsKey("key")
                && map.containsKey("command")) {

            if (map.get("command").equals(Command.TRANSFER.name())) {
                return map.containsKey("destinationKey")
                        && map.containsKey("amount")
                        && map.containsKey("guid");
            } else {
                return true;
            }
        }

        return false;
    }

    public static boolean verifyUserSignature(PublicKey key,
                                               String command,
                                               String signature) {
        SignatureVerifier signatureVerifier = new SignatureVerifier(key);
        signatureVerifier.addData(command);

        return signatureVerifier.verify(signature);
    }

    public static String generateBody(String signature, String data) {
        final String encodedData = new String(Base64.getEncoder().encode(data.getBytes()));

        return "payload="
                .concat(encodedData)
                .concat(",signature=")
                .concat(signature);
    }

    public static String generateSignature(PrivateKey privateKey, String data) {
        SignatureBuilder signatureBuilder = new SignatureBuilder(privateKey);
        signatureBuilder.addData(data);

        return signatureBuilder.sign();
    }
}
