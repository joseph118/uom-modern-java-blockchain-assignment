package data;

import data.balance.Balance;
import data.history.HistoryLine;
import data.history.TransactionHistory;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Ledger {

    final static Logger logger = Logger.getLogger(Ledger.class);

    private static final String LEDGER_EXTENSION = ".ledger";
    private static final String CSV_DELIMITER = ",";

    private Ledger() {
    }

    public static synchronized boolean addTransaction(Transaction transaction, String nodeName) {
        final String newLine = System.lineSeparator();
        final String row = transaction.toCsvRow().concat(newLine);

        try {
            URL ledgerUrl = ClassLoader.getSystemResource(nodeName.concat(LEDGER_EXTENSION));

            Files.write(Paths.get(ledgerUrl.toURI()), row.getBytes(), StandardOpenOption.APPEND);

            logger.info("Added: ".concat(row));

            return true;
        }catch (Exception e) {
            System.out.println(e.toString());
        }

        return false;
    }

    public static Balance getUserBalance(String nodeName, String publicKey) {
        List<TransactionHistory> transactionHistories = getTransactionHistory(nodeName, publicKey);

        return transactionHistories.stream()
                .reduce(
                        new Balance(0,0, "", 0),
                        (balance, transactionHistory) -> {
                            final float crValue;
                            final float drValue;

                            if (transactionHistory.getDrCrIndicator().equals(TransactionHistory.DrCrIndicator.DR)){
                                drValue = transactionHistory.getTransactionAmount() + balance.getDrValue();
                                crValue = balance.getCrValue();
                            } else {
                                drValue = balance.getDrValue();
                                crValue = transactionHistory.getTransactionAmount() + balance.getCrValue();
                            }

                            final String timestamp;
                            final long longTimestamp;

                            if (balance.getLongTimestamp() > transactionHistory.getTimestamp()) {
                                timestamp = balance.getTimestamp();
                                longTimestamp = balance.getLongTimestamp();
                            } else {
                                timestamp = transactionHistory.getTimestampAsString();
                                longTimestamp = transactionHistory.getTimestamp();
                            }

                            return new Balance(drValue, crValue, timestamp, longTimestamp);
                        }, (balance1, balance2) -> {
                            final String timestamp;
                            final long longTimestamp;

                            if (balance1.getLongTimestamp() > balance2.getLongTimestamp()) {
                                timestamp = balance1.getTimestamp();
                                longTimestamp = balance1.getLongTimestamp();
                            } else {
                                timestamp = balance2.getTimestamp();
                                longTimestamp = balance2.getLongTimestamp();
                            }

                            return new Balance(
                                balance1.getDrValue() + balance2.getDrValue(),
                                balance1.getCrValue() + balance2.getCrValue(),
                                    timestamp,
                                    longTimestamp);
                        }
                        );
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
                InputStreamReader streamReader = new InputStreamReader(nodeLedgerStream, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(streamReader);

                return reader
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

    public static TransactionHistory getUserLastTransaction(String nodeName, String publicKey) {
        return getTransactionHistory(nodeName, publicKey).parallelStream()
                .reduce((transactionHistory, transactionHistory2) -> {
                    return (transactionHistory.getTimestamp() > transactionHistory2.getTimestamp())
                            ? transactionHistory
                            : transactionHistory2;
                }).orElse(null);

    }

    public static String getUserHistoryAsString(String nodeName, String publicKey) {
        List<TransactionHistory> transactionHistories = getTransactionHistory(nodeName, publicKey);

        return transactionHistories.parallelStream()
                .sorted(Comparator.comparingLong(TransactionHistory::getTimestamp))
                .reduce(
                    new HistoryLine("", 0, ""),
                    (historyLine, transactionHistory) -> {
                        int a = transactionHistory.getDrCrIndicator().equals(TransactionHistory.DrCrIndicator.DR)
                                ? 1 : -1;

                        float balance = historyLine.getBalance() + (transactionHistory.getTransactionAmount() * a);

                        return new HistoryLine(
                                historyLine.getTransactionList().concat(transactionHistory.getTransactionLine()),
                                balance,
                                transactionHistory.getTimestampAsString());
                    },
                    (historyLine, historyLine2) -> {
                        return new HistoryLine(
                                historyLine.getTransactionList().concat(historyLine2.getTransactionList()),
                        historyLine.getBalance() + historyLine2.getBalance(),
                                historyLine2.getLastTimestamp());
                    }
                ).getTotal();
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
                transaction.getRecipientPublicKey(),
                transaction.getHash());
    }
}
