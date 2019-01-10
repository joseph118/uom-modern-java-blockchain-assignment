package model.history;

public class HistoryLine {
    private String transactionList;
    private final float balance;
    private final String lastTimestamp;

    public HistoryLine(String transactionList, float balance, String lastTimestamp) {
        this.transactionList = transactionList;
        this.balance = balance;
        this.lastTimestamp = lastTimestamp;
    }

    public String getTransactionList() {
        return transactionList;
    }

    public float getBalance() {
        return balance;
    }

    public String getLastTimestamp() {
        return lastTimestamp;
    }

    public String getTotal() {
        return transactionList.concat(lastTimestamp)
                .concat(" BALANCE ")
                .concat(String.valueOf(balance));
    }
}
