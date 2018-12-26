package util;

import model.Transaction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class LedgerUtilities {

    public static List<Transaction> getTransactions(String nodeName) {
        try (InputStream nodeLedgerStream = ClassLoader.getSystemResourceAsStream(nodeName.concat(".ledger"))) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(nodeLedgerStream));

            return reader.lines()
                    .map(s -> s.split(","))
                    .map(strings -> new Transaction(
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
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return null;
    }
}
