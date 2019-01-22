package data;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class NodeDataRequest {
    private AtomicInteger connectionsOk;
    private AtomicInteger connectionsError;
    private final int connections;
    private Queue<String> dataQueue;
    private Thread thread;

    public NodeDataRequest(int connections, Thread thread) {
        this.connectionsOk = new AtomicInteger(0);
        this.connectionsError = new AtomicInteger(0);
        this.dataQueue = new ConcurrentLinkedQueue<>();

        this.connections = connections;
        this.thread = thread;
    }

    public int incrementOkResponse() {
        return connectionsOk.incrementAndGet();
    }

    public void addDataAndIncrementOkResponse(String signature) {
        dataQueue.offer(signature);

        if (incrementOkResponse() == connections) {
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    public List<String> getData() {
        List<String> signatures = new ArrayList<>();

        String signature;
        while ((signature = dataQueue.poll()) != null) {
            signatures.add(signature);
        }

        return signatures;
    }

    public int incrementErrorResponse() {
        return connectionsError.incrementAndGet();
    }

    public int getErrorResponseCount() {
        return connectionsError.get();
    }

    public int getSuccessfulResponseCount() {
        return connectionsOk.get();
    }

    public int getTotalConnectionsMade() {
        return connections;
    }

    @Override
    public String toString() {
        return "NodeDataRequest{" +
                "connectionsOk=" + connectionsOk +
                ", connectionsError=" + connectionsError +
                ", connections=" + connections +
                ", dataQueue=" + dataQueue +
                '}';
    }
}
