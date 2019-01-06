package model;

import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyHolder {
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    private final PublicKey nodePublicKey;

    public KeyHolder(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.nodePublicKey = null;
    }

    public KeyHolder(PublicKey publicKey, PrivateKey privateKey, PublicKey nodePublicKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.nodePublicKey = nodePublicKey;
    }

    public PublicKey getNodePublicKey() {
        return nodePublicKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public String toString() {
        return "KeyHolder{" +
                "publicKey=" + publicKey +
                ", privateKey=" + privateKey +
                ", nodePublicKey=" + nodePublicKey +
                '}';
    }
}
