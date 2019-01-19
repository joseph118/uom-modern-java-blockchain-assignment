package util;

import data.NodeDataRequest;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RequestVerification {
    private final static Logger logger = Logger.getLogger(RequestVerification.class);

    private RequestVerification() {

    }

    public static boolean waitForVerificationProcess(String userPublicKey, Map<String, NodeDataRequest> dataMap) {
        try {
            int requestCounter = 0;

            do {
                TimeUnit.SECONDS.sleep(3);
                requestCounter++;
            } while (!areAllRequestsReady(userPublicKey, dataMap) && requestCounter != 3);

            // TODO once done update... and clear... also add checks on threads not to blow up
        } catch (InterruptedException ex) {
            logger.error(ex);

            return false;
        }

        return true;
    }

    private static boolean areAllRequestsReady(String userPublicKey, Map<String, NodeDataRequest> dataMap) {
        final NodeDataRequest nodeDataRequest = dataMap.get(userPublicKey);
        final int totalConnectionsDone = nodeDataRequest.getSuccessfulResponseCount() + nodeDataRequest.getErrorResponseCount();

        logger.info(nodeDataRequest.toString());

        return totalConnectionsDone == nodeDataRequest.getTotalConnectionsMade();
    }

}
