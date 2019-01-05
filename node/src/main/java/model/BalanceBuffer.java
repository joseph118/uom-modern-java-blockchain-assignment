package model;

public class BalanceBuffer {
    private final float drValue;
    private final float crValue;
    private final String timestamp;
    private final long longTimestamp;

    public BalanceBuffer(float drValue, float crValue, String timestamp, long longTimestamp) {
        this.drValue = drValue;
        this.crValue = crValue;
        this.timestamp = timestamp;
        this.longTimestamp = longTimestamp;
    }

    public float getDrValue() {
        return drValue;
    }

    public float getCrValue() {
        return crValue;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public long getLongTimestamp() {
        return longTimestamp;
    }

    @Override
    public String toString() {
        final float balance = drValue - crValue;
        final String indicator = balance >= 0 ? "DR" : "CR";

        return "DR ".concat(String.valueOf(drValue))
                .concat("\nCR ").concat(String.valueOf(crValue))
                .concat("\n").concat(timestamp)
                .concat(" TOTAL ").concat(indicator).concat(" ")
                .concat(String.valueOf(balance));
    }
}
