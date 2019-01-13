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
        }

        return Command.OTHER;
    }

}

