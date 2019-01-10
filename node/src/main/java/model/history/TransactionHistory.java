package model.history;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TransactionHistory {
    public enum DrCrIndicator {
        DR, CR
    }

    private final DrCrIndicator drCrIndicator;
    private final long timestamp;
    private final float transactionAmount;
    private final String senderKey;
    private final String recipientKey;

    public TransactionHistory(DrCrIndicator drCrIndicator, long timestamp, float transactionAmount, String senderKey,
                              String recipientKey) {
        this.drCrIndicator = drCrIndicator;
        this.timestamp = timestamp;
        this.transactionAmount = transactionAmount;
        this.senderKey = senderKey;
        this.recipientKey = recipientKey;
    }

    public DrCrIndicator getDrCrIndicator() {
        return drCrIndicator;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTimestampAsString() {
        return Instant.ofEpochSecond(this.timestamp/1000)
                .atZone(ZoneId.of("GMT"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public float getTransactionAmount() {
        return transactionAmount;
    }

    public String getSenderKey() {
        return senderKey;
    }

    public String getRecipientKey() {
        return recipientKey;
    }

    public String getTransactionLine() {
        // Display the opposite key for the user
        String secondParty = drCrIndicator.equals(DrCrIndicator.DR)
                ? senderKey
                : recipientKey;

        return getTimestampAsString().concat(" ")
                .concat(drCrIndicator.name()).concat(" ")
                .concat(String.valueOf(transactionAmount)).concat("\n")
                .concat(secondParty).concat("\n\n");
    }
}
