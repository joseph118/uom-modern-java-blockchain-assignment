package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Ledger {

    private static final String LEDGER_EXTENSION = ".ledger";
    private static final String CSV_DELIMITER = ",";

    private Ledger() {
    }

    public static boolean addTransaction(Transaction transaction, String nodeName) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(ClassLoader.getSystemResource(nodeName.concat(LEDGER_EXTENSION)).toURI()))) {

            writer.write(transaction.toCsvRow());
            return true;

        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return false;
    }

    public static String getUserBalance(String nodeName, String publicKey) {
        List<TransactionHistory> transactionHistories = getTransactionHistory(nodeName, publicKey);

        return transactionHistories.parallelStream()
                .reduce(
                        new BalanceBuffer(0,0, "", 0),
                        (balanceBuffer, transactionHistory) -> {
                            final float crValue;
                            final float drValue;

                            if (transactionHistory.getDrCrIndicator().equals(TransactionHistory.DrCrIndicator.DR)){
                                drValue = transactionHistory.getTransactionAmount() + balanceBuffer.getDrValue();
                                crValue = balanceBuffer.getCrValue();
                            } else {
                                drValue = balanceBuffer.getDrValue();
                                crValue = transactionHistory.getTransactionAmount() + balanceBuffer.getCrValue();
                            }

                            final String timestamp;
                            final long longTimestamp;

                            if (balanceBuffer.getLongTimestamp() > transactionHistory.getTimestamp()) {
                                timestamp = balanceBuffer.getTimestamp();
                                longTimestamp = balanceBuffer.getLongTimestamp();
                            } else {
                                timestamp = transactionHistory.getTimestampAsString();
                                longTimestamp = transactionHistory.getTimestamp();
                            }

                            return new BalanceBuffer(drValue, crValue, timestamp, longTimestamp);
                        }, (balanceBuffer, balanceBuffer2) -> {
                            final String timestamp;
                            final long longTimestamp;

                            if (balanceBuffer.getLongTimestamp() > balanceBuffer2.getLongTimestamp()) {
                                timestamp = balanceBuffer.getTimestamp();
                                longTimestamp = balanceBuffer.getLongTimestamp();
                            } else {
                                timestamp = balanceBuffer2.getTimestamp();
                                longTimestamp = balanceBuffer2.getLongTimestamp();
                            }

                            return new BalanceBuffer(
                                balanceBuffer.getDrValue() + balanceBuffer2.getDrValue(),
                                balanceBuffer.getCrValue() + balanceBuffer2.getCrValue(),
                                    timestamp,
                                    longTimestamp);
                        }
                        ).toString();
    }

    public static List<Transaction> getTransactions(String nodeName) {
        try (InputStream nodeLedgerStream = ClassLoader.getSystemResourceAsStream(nodeName.concat(LEDGER_EXTENSION))) {
            if (nodeLedgerStream != null) {
                return new BufferedReader(new InputStreamReader(nodeLedgerStream))
                        .lines()
                        .parallel()
                        .map(s -> s.split(CSV_DELIMITER))
                        .map(Ledger::mapToTransaction)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return new ArrayList<>(0);
    }

    public static List<TransactionHistory> getTransactionHistory(String nodeName, String publicKey) {
        try (InputStream nodeLedgerStream = ClassLoader.getSystemResourceAsStream(nodeName.concat(LEDGER_EXTENSION))) {
            if (nodeLedgerStream != null) {
                return new BufferedReader(new InputStreamReader(nodeLedgerStream))
                        .lines()
                        .parallel()
                        .map(s -> s.split(CSV_DELIMITER))
                        .map(Ledger::mapToTransaction)
                        .filter(transaction -> transaction.getRecipientPublicKey().equals(publicKey)
                                || transaction.getSenderPublicKey().equals(publicKey))
                        .map(transaction -> Ledger.mapToTransactionHistory(transaction, publicKey))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return new ArrayList<>(0);
    }

    public static String getUserHistoryAsString(String nodeName, String publicKey) {
        List<TransactionHistory> transactionHistories = getTransactionHistory(nodeName, publicKey);

        return transactionHistories.parallelStream()
                .sorted(Comparator.comparingLong(TransactionHistory::getTimestamp))
                .reduce(
                    new HistoryBuffer("", 0, ""),
                    (historyBuffer, transactionHistory) -> {
                        int a = transactionHistory.getDrCrIndicator().equals(TransactionHistory.DrCrIndicator.DR)
                                ? 1 : -1;

                        float balance = historyBuffer.getBalance() + (transactionHistory.getTransactionAmount() * a);

                        return new HistoryBuffer(
                                historyBuffer.getLines().concat(transactionHistory.toString()),
                                balance,
                                transactionHistory.getTimestampAsString());
                    },
                    (historyBuffer, historyBuffer2) -> {
                        return new HistoryBuffer(
                                historyBuffer.getLines().concat(historyBuffer2.getLines()),
                        historyBuffer.getBalance() + historyBuffer2.getBalance(),
                                historyBuffer2.getLastTimestamp());
                    }
                ).toString();
    }

    private static Transaction mapToTransaction(String[] strings) {
        return new Transaction(
                Long.parseLong(strings[0]),   // Timestamp
                strings[1],                   // Hash
                strings[2],                   // GUID
                strings[3],                   // Sender Public Key
                strings[4],                   // Recipient Public Key
                Float.parseFloat(strings[5]), // Transaction Amount
                strings[6],                   // Sender Authorisation Signature
                strings[7],                   // Verification Signature 1
                strings[8],                   // Verification Signature 2
                strings[9],                   // Verification Signature 3
                strings[10]                   // Confirmation Signature
        );
    }

    private static TransactionHistory mapToTransactionHistory(Transaction transaction, String publicKey) {
        TransactionHistory.DrCrIndicator drCrIndicator = (transaction.getRecipientPublicKey().equals(publicKey))
                ? TransactionHistory.DrCrIndicator.DR
                : TransactionHistory.DrCrIndicator.CR;

        return new TransactionHistory(drCrIndicator,
                transaction.getTimestamp(),
                transaction.getTransactionAmount(),
                transaction.getSenderPublicKey(),
                transaction.getRecipientPublicKey());
    }
}
