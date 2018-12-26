package model;

public class Transaction {
    private final long timestamp;
    private final String hash;
    private final String guid;
    private final String senderPublicKey;
    private final String recipientPublicKey;
    private final float transactionAmount;
    private final String senderAuthorisationSignature;
    private final String verificationSignature1;
    private final String verificationSignature2;
    private final String verificationSignature3;
    private final String confirmationSignature;

    public Transaction(long timestamp, String hash, String guid, String senderPublicKey, String recipientPublicKey, float transactionAmount, String senderAuthorisationSignature, String verificationSignature1, String verificationSignature2, String verificationSignature3, String confirmationSignature) {
        this.timestamp = timestamp;
        this.hash = hash;
        this.guid = guid;
        this.senderPublicKey = senderPublicKey;
        this.recipientPublicKey = recipientPublicKey;
        this.transactionAmount = transactionAmount;
        this.senderAuthorisationSignature = senderAuthorisationSignature;
        this.verificationSignature1 = verificationSignature1;
        this.verificationSignature2 = verificationSignature2;
        this.verificationSignature3 = verificationSignature3;
        this.confirmationSignature = confirmationSignature;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "timestamp=" + timestamp +
                ", hash='" + hash + '\'' +
                ", guid='" + guid + '\'' +
                ", senderPublicKey='" + senderPublicKey + '\'' +
                ", recipientPublicKey='" + recipientPublicKey + '\'' +
                ", transactionAmount=" + transactionAmount +
                ", senderAuthorisationSignature='" + senderAuthorisationSignature + '\'' +
                ", verificationSignature1='" + verificationSignature1 + '\'' +
                ", verificationSignature2='" + verificationSignature2 + '\'' +
                ", verificationSignature3='" + verificationSignature3 + '\'' +
                ", confirmationSignature='" + confirmationSignature + '\'' +
                '}';
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getHash() {
        return hash;
    }

    public String getGuid() {
        return guid;
    }

    public String getSenderPublicKey() {
        return senderPublicKey;
    }

    public String getRecipientPublicKey() {
        return recipientPublicKey;
    }

    public float getTransactionAmount() {
        return transactionAmount;
    }

    public String getSenderAuthorisationSignature() {
        return senderAuthorisationSignature;
    }

    public String getVerificationSignature1() {
        return verificationSignature1;
    }

    public String getVerificationSignature2() {
        return verificationSignature2;
    }

    public String getVerificationSignature3() {
        return verificationSignature3;
    }

    public String getConfirmationSignature() {
        return confirmationSignature;
    }
}
