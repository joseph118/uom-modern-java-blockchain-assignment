package model;

import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyHolder {
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public KeyHolder(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
