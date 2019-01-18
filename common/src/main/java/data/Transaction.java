package data;

import util.Parser;

import java.util.Map;

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

    public Transaction(long timestamp, String hash, String guid, String senderPublicKey, String recipientPublicKey,
                       float transactionAmount, String senderAuthorisationSignature, String verificationSignature1,
                       String verificationSignature2, String verificationSignature3, String confirmationSignature) {
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

    public Transaction(long timestamp, String hash, String guid, String senderPublicKey, String recipientPublicKey,
                       float transactionAmount, String senderAuthorisationSignature, String verificationSignature1,
                       String verificationSignature2, String verificationSignature3) {
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
        this.confirmationSignature = "";
    }

    public Transaction(long timestamp, String hash, String guid, String senderPublicKey, String recipientPublicKey,
                       float transactionAmount, String senderAuthorisationSignature) {
        this.timestamp = timestamp;
        this.hash = hash;
        this.guid = guid;
        this.senderPublicKey = senderPublicKey;
        this.recipientPublicKey = recipientPublicKey;
        this.transactionAmount = transactionAmount;
        this.senderAuthorisationSignature = senderAuthorisationSignature;
        this.verificationSignature1 = "";
        this.verificationSignature2 = "";
        this.verificationSignature3 = "";
        this.confirmationSignature = "";
    }

    /**
     * Maps the client response to a transaction. Undefined values will be replaced by empty strings or 0 for numbers.
     * @param response The valid response retrieved from the client.
     * @return A new transaction instance.
     */
    public static Transaction mapResponseToTransaction(Map<String, String> response) {
        return new Transaction(
                Long.parseLong(response.getOrDefault("timestamp", "0")),
                response.getOrDefault("hash", ""),
                response.getOrDefault("guid", ""),
                response.getOrDefault("senderkey", ""),
                response.getOrDefault("receiverkey", ""),
                Float.parseFloat(response.getOrDefault("amount", "0")),
                response.getOrDefault("sendersignature", ""),
                response.getOrDefault("signature1", ""),
                response.getOrDefault("signature2", ""),
                response.getOrDefault("signature3", ""),
                response.getOrDefault("signature", ""));
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

    /**
     * Generates a comma separated list representing a CSV row.
     * @return The transaction in a string format.
     */
    public String toCsvRow() {
        String timestamp = String.valueOf(this.timestamp);
        String transactionAmount = String.valueOf(this.transactionAmount);

        return timestamp.concat(",")
                .concat(hash).concat(",")
                .concat(guid).concat(",")
                .concat(senderPublicKey).concat(",")
                .concat(recipientPublicKey).concat(",")
                .concat(transactionAmount).concat(",")
                .concat(senderAuthorisationSignature).concat(",")
                .concat(verificationSignature1).concat(",")
                .concat(verificationSignature2).concat(",")
                .concat(verificationSignature3).concat(",")
                .concat(confirmationSignature);
    }

    /**
     * Generates a String of the current transaction details which are separated by comma and then
     *  the key value pair are separated by equal.
     * @return The transaction in a string format.
     */
    public String toTransactionVerifiedResponseRow() {
        return getBasicTransactionDetailRow().concat(",")
                .concat(getTransactionVerificationRow());
    }

    /**
     * Generates a String of the current transaction details which are separated by comma and then
     *  the key value pair are separated by equal.
     * @return The transaction in a string format.
     */
    public String toTransactionConfirmationResponseRow() {
        return getBasicTransactionDetailRow().concat(",")
                .concat(getTransactionVerificationRow().concat(","))
                .concat(getTransactionConfirmationSignature());
    }

    /**
     * A long integer, representing the timestamp (UTC) with milliseconds precision, of when
     * the transaction was requested by the wallet, generated by the node that receives the transaction.
     * @return Returns unformatted timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns String representation of the current timestamp.
     * @return EPOCH timestamp.
     */
    public String getStringTimestamp() {
        return String.valueOf(timestamp);
    }

    /**
     * A string representing the hash computed for the transaction.
     * @return String hash.
     */
    public String getHash() {
        return hash;
    }

    /**
     * A GUID generated by the Wallet as the global unique identifier of the transaction.
     * @return GUID.
     */
    public String getGuid() {
        return guid;
    }

    /**
     * Public key of the sender of the transaction, Base64 encoded.
     * @return
     */
    public String getSenderPublicKey() {
        return senderPublicKey;
    }

    /**
     * Public key of the recipient of the transaction, Base64 encoded.
     * @return
     */
    public String getRecipientPublicKey() {
        return recipientPublicKey;
    }

    /**
     * A floating-point number, with the dot used as the decimal separator, up to 6 decimal places.
     * @return
     */
    public float getTransactionAmount() {
        return transactionAmount;
    }

    /**
     * 6 decimal string float number.
     * @return
     */
    public String getStringTransactionAmount() {
        return Parser.convertAmountToString(transactionAmount);
    }

    /**
     * The Base64 encoded signature of the sender when sending the first authorisation verification.
     * This is computed from the GUID, public key of the sender, public key of the recipient, and
     * the transaction amount (as a string with 6 decimal places), hashed with SHA-256 and signed
     * using the private key of the sender.
     * @return
     */
    public String getSenderAuthorisationSignature() {
        return senderAuthorisationSignature;
    }

    /**
     * The node name followed by the Base64 encoded signature of the first node that verified the
     * transaction, separated by a colon. The signature is computed from the GUID, the sender public key,
     * the recipient public key, the amount (as a string with 6 decimal places), the timestamp (as a string),
     * the transaction hash, and the node name, hashed with SHA256 and signed using the private key of the node.
     * @return
     */
    public String getVerificationSignature1() {
        return verificationSignature1;
    }

    /**
     * The node name followed by the Base64 encoded signature of the second node that verified the transaction,
     * separated by a colon. Computed in the same way as Verification Signature 1.
     * @return
     */
    public String getVerificationSignature2() {
        return verificationSignature2;
    }

    /**
     * The node name followed by the Base64 encoded signature of the third node that verified the transaction,
     * separated by a colon. Computed in the same way as Verification Signature 1.
     * @return
     */
    public String getVerificationSignature3() {
        return verificationSignature3;
    }

    /**
     * The Base64 encoded signature confirming the transaction by the sender. This is computed from the GUID,
     * sender public key, recipient public key, amount (as a string), sender authorisation signature, timestamp
     * (as a string), hash, verification signature 1, verification signature 2 and verification signature 3
     * (each as a full string including the colon character).
     * @return
     */
    public String getConfirmationSignature() {
        return confirmationSignature;
    }

    /**
     * Returns the transaction details. Comma delimited columns with equal delimited map data.
     * @return
     */
    private String getBasicTransactionDetailRow() {
        return ("guid=").concat(getGuid())
                .concat(",senderkey=").concat(getSenderPublicKey())
                .concat(",receiverkey=").concat(getRecipientPublicKey())
                .concat(",amount=").concat(getStringTransactionAmount())
                .concat(",sendersignature=").concat(getSenderAuthorisationSignature())
                .concat(",timestamp=").concat(getStringTimestamp())
                .concat(",hash=").concat(getHash());
    }

    /**
     * Returns the verification signatures. Comma delimited columns with equal delimited map data.
     * @return Signatures
     */
    private String getTransactionVerificationRow() {
        return "signature1=".concat(getVerificationSignature1())
                .concat(",signature2=").concat(getVerificationSignature2())
                .concat(",signature3=").concat(getVerificationSignature3());
    }

    /**
     * Returns the Confirmation signature. Comma delimited columns with equal delimited map data.
     * @return Confirmation signature
     */
    private String getTransactionConfirmationSignature() {
        return "confirmsignature=".concat(getConfirmationSignature());
    }
}
