package util;

import data.Command;
import exception.ArgumentsNotFoundException;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Parser {
    private Parser() {

    }

    /**
     * Converts the given arguments into a Map. Keys are converted to lower case.
     *
     * @param args The comma delimited rows.
     * @return A Map of key value pairs.
     * @throws ArgumentsNotFoundException Empty arguments will throw an exception.
     */
    public static Map<String, String> convertArgsToMap(String[] args, String delimiter) throws ArgumentsNotFoundException {
        return Arrays.stream(args)
                .map(s -> s.split(delimiter, 2)) // [0] key, [1] String
                .map(strings -> {
                    Map<String, String> map = new HashMap<>();

                    map.put(strings[0].toLowerCase(), strings[1]);

                    return map;
                })
                .reduce((map, map2) -> {
                    map.putAll(map2);

                    return map;
                })
                .orElseThrow(ArgumentsNotFoundException::new);
    }

    /**
     * Converts the float amount to a fully padded value as string.
     * @param amount the transaction amount.
     * @return The amount padded to 6 decimal places.
     */
    public static String convertAmountToString(float amount) {
        DecimalFormat df = new DecimalFormat("#.000000");
        df.setRoundingMode(RoundingMode.DOWN);

        return df.format(amount);
    }

    /**
     * Converts the command to Enum.
     * @param command The string command.
     * @return The command.
     */
    public static Command convertToCommand(String command) {
        switch (command) {
            case "BALANCE":
                return Command.BALANCE;
            case "HISTORY":
                return Command.HISTORY;
            case "TRANSFER":
                return Command.TRANSFER;
            case "NODE_CONNECT":
                return Command.NODE_CONNECT;
            case "VERIFY":
                return Command.VERIFY;
            case "VERIFY_OK":
                return Command.VERIFY_OK;
        }

        return Command.OTHER;
    }
}
