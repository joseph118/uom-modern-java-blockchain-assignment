import com.sun.media.sound.InvalidDataException;
import command.line.ArgumentParser;
import exception.ArgumentsNotFoundException;

import java.util.Map;
import java.util.Scanner;

public class App {
    private String userPublicKey;
    private String userPrivateKey;

    /**
     *
     * @param args - ['username=****'], ['nodename=****'], ['command=****']
     */
    public static void main(String[] args) {
        Map<String, String> map;

        try {
            map = ArgumentParser.convertCommandLineArgs(args);

            if (App.isMapValid(map)) {
                String username = map.get("username");

                System.out.println("Hi "
                        .concat(username)
                        .concat(", \n Kindly input your password: \n"));

                Scanner scanner = new Scanner(System.in);
                String userPassword = scanner.nextLine();

                // TODO lookup get IP and Port from node.properties
                // TODO attempt to connect
                // TODO send the request - signed with private key (Appendix A)
                // TODO wait for request and sign the request to confirm true node.

            } else {
                throw new InvalidDataException("Arguments 'username', 'nodename', and 'command' are required.");
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
}