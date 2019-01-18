package util;

import java.util.Map;

public class Response {
    private Response() {

    }


    public static boolean isError(Map<String, String> nodeResponse) {
        return nodeResponse.containsKey("error") || nodeResponse.isEmpty();
    }

    public static boolean areAllTransactionKeysPresent(Map<String, String> response) {
        return response.containsKey("guid")
                && response.containsKey("senderkey")
                && response.containsKey("receiverkey")
                && response.containsKey("amount")
                && response.containsKey("sendersignature")
                && response.containsKey("timestamp")
                && response.containsKey("hash")
                && response.containsKey("signature1")
                && response.containsKey("signature2")
                && response.containsKey("signature3");
    }

    public static boolean isNodeConfirmResponseValid(Map<String, String> response) {
        return areAllTransactionKeysPresent(response)
                && response.containsKey("signature")
                && response.containsKey("nodename");
    }

    public static boolean isWalletConfirmResponseValid(Map<String, String> response) {
        return areAllTransactionKeysPresent(response)
                && response.containsKey("signature")
                && response.containsKey("command")
                && response.containsKey("key");
    }
}
