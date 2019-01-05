package model;

public class HistoryBuffer {
    private final String lines;
    private final float balance;
    private final String lastTimestamp;

    public HistoryBuffer(String lines, float balance, String lastTimestamp) {
        this.lines = lines;
        this.balance = balance;
        this.lastTimestamp = lastTimestamp;
    }

    public String getLines() {
        return lines;
    }

    public float getBalance() {
        return balance;
    }

    public String getLastTimestamp() {
        return lastTimestamp;
    }

    @Override
    public String toString() {
        return lines.concat(lastTimestamp)
                .concat(" BALANCE ")
                .concat(String.valueOf(balance));
    }
}
