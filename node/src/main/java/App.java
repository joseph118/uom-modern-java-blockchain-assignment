import com.sun.media.sound.InvalidDataException;
import command.line.ArgumentParser;

import java.util.Map;

public class App {
    private String nodePublicKey;
    private String nodePrivateKey;

    /**
     *
     * @param args - ['nodename=****'], ['port=****']
     */
    public static void main(String[] args) {
        Map<String, String> map;

        try {
            map = ArgumentParser.convertCommandLineArgs(args);

            if (App.isMapValid(map)) {
                // TODO: Connect with other nodes, if it fails it must reconnect later on but still success....
                // TODO: This will only handle balance and history request if this node is connected to less than 3 nodes.
                // TODO: Each node will have its own ledger (csv file) (appendix b)
            } else {
                throw new InvalidDataException("Arguments 'nodename' and 'port' are required.");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(-1);
        }
    }

    private static boolean isMapValid(Map<String, String> map) {
        return map.containsKey("nodename")
                && map.containsKey("port");
    }
}
