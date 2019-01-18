package util;

import data.NodeDataRequest;
import org.apache.log4j.Logger;

import java.util.Map;

public class RequestVerification {
    private final static Logger logger = Logger.getLogger(RequestVerification.class);

    private RequestVerification() {

    }

    public static boolean waitForVerificationProcess(String userPublicKey, Map<String, NodeDataRequest> dataMap) {
        try {
            int requestCounter = 0;
            while (!areAllRequestsReady(userPublicKey, dataMap) || requestCounter != 3) {
                Thread.sleep(5000);
                requestCounter++;
            }
        } catch (InterruptedException ex) {
            logger.error(ex);

            return false;
        }

        return true;
    }

    private static boolean areAllRequestsReady(String userPublicKey, Map<String, NodeDataRequest> dataMap) {
        NodeDataRequest nodeDataRequest = dataMap.get(userPublicKey);
        final int totalConnectionsDone = nodeDataRequest.getSuccessfulResponseCount() + nodeDataRequest.getErrorResponseCount();
        return totalConnectionsDone == nodeDataRequest.getTotalConnectionsMade();
    }

}
