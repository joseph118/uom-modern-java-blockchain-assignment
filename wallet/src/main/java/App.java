import command.line.ArgumentParser;
import model.KeyHolder;
import model.ServerNode;
import model.enums.Command;
import security.KeyLoader;
import util.NodeUtilities;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.Scanner;

public class App {

    /**
     *
     * @param args - ['username=****'], ['nodename=****'], ['command=****']
     */
    public static void main(String[] args) {
        Map<String, String> map;

        try {
            map = ArgumentParser.convertCommandLineArgs(args);

            if (App.isMapValid(map)) {
                Command userCommand = App.convertToCommand(map.get("command"));
                if (!userCommand.equals(Command.OTHER)) {
                    String username = map.get("username");

                    System.out.println("Hi "
                            .concat(username)
                            .concat(", \n Kindly input your password: \n"));

                    Scanner scanner = new Scanner(System.in);
                    String userPassword = scanner.nextLine();

                    URL userResource = App.getUserResource(username);
                    if (userResource != null) {
                        KeyHolder userKeys = App.getUserKey(username, userPassword, userResource);
                        if (userKeys != null) {
                            ServerNode node = NodeUtilities.getNode(map.get("nodename"));

                            if (node != null) {
                                // TODO attempt to connect
                                // TODO send the request - signed with private key (Appendix A)
                                // TODO wait for request and sign the request to confirm true node.
                            } else {
                                throw new Exception("Node name doesn't exists.");
                            }
                        } else {
                            throw new Exception("Password is incorrect.");
                        }
                    } else {
                        throw new Exception("Username is incorrect.");
                    }
                } else {
                    throw new Exception("Command must be one of the following:\n balance\n history\n transfer");
                }
            } else {
                throw new Exception("Arguments 'username', 'nodename', and 'command' are required.");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(-1);
        }
    }

    private static boolean isMapValid(Map<String, String> map) {
        return map.containsKey("username")
                && map.containsKey("nodename")
                && map.containsKey("command");
    }

    private static Command convertToCommand(String command) {
        switch (command) {
            case "balance":
                return Command.BALANCE;
            case "history":
                return Command.HISTORY;
            case "transfer":
                return Command.TRANSFER;
        }

        return Command.OTHER;
    }

    private static KeyHolder getUserKey(String name, String password, URL url) {

        try {
            Path path = Paths.get(url.toURI());

            PublicKey publicKey = KeyLoader.loadPublicKey(path, name, password);
            PrivateKey privateKey = KeyLoader.loadPrivateKey(path, name, password, password);

            return new KeyHolder(publicKey, privateKey);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return null;
    }

    private static URL getUserResource(String name) {
        return App.class.getResource(name.concat(".pfx"));
    }
}