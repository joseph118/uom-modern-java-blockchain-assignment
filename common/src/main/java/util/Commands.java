package util;

import model.Command;

public class Commands {
    private Commands() {

    }

    public static Command convertToCommand(String command) {
        switch (command) {
            case "BALANCE":
                return Command.BALANCE;
            case "HISTORY":
                return Command.HISTORY;
            case "TRANSFER":
                return Command.TRANSFER;
            case "CONNECT":
                return Command.CONNECT;
            case "VERIFY":
                return Command.VERIFY;
            case "VERIFY_OK":
                return Command.VERIFY_OK;
        }

        return Command.OTHER;
    }

}

