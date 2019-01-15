package core;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DataHolder {
    private AtomicInteger connectionsOk;
    private AtomicInteger connectionsError;
    private final int connections;
    private Queue<String> signatureQueue;

    public DataHolder(int connections) {
        this.connectionsOk = new AtomicInteger(0);
        this.connectionsError = new AtomicInteger(0);
        this.signatureQueue = new ConcurrentLinkedQueue<>();
        this.connections = connections;
    }

    private void incrementOk() {
        connectionsOk.incrementAndGet();
    }

    public void addSignatureAndIncrement(String signature) {
        signatureQueue.offer(signature);
        incrementOk();
    }

    public List<String> getSignatures() {
        List<String> signatures = new ArrayList<>();

        String signature;
        while ((signature = signatureQueue.poll()) != null) {
            signatures.add(signature);
        }

        return signatures;
    }

    public int incrementError() {
        return connectionsError.incrementAndGet();
    }

    public int getConnectionsError() {
        return connectionsError.get();
    }

    public int getConnectionsOk() {
        return connectionsOk.get();
    }

    public int getConnections() {
        return connections;
    }
}
