package model;

import java.util.List;

public class Ledger {
    final List<Transaction> transactions;

    public Ledger(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void print() {
        this.transactions.forEach(System.out::println);
    }

    @Override
    public String toString() {
        return "Ledger{" +
                "transactions=" + transactions +
                '}';
    }
}
