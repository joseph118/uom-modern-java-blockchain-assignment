import command.line.ArgumentParser;
import model.KeyHolder;
import model.Ledger;
import security.KeyLoader;
import util.LedgerUtilities;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

public class App {

    /**
     *
     * @param args - ['nodename=****'], ['port=****']
     */
    public static void main(String[] args) {
        Map<String, String> map;

        try {
            map = ArgumentParser.convertCommandLineArgs(args);

            if (App.isMapValid(map)) {
                final String nodeName = map.get("nodename");
                KeyHolder nodeKeys = App.getNodeKeys(nodeName);

                if (nodeKeys != null) {
                    Ledger ledger = LedgerUtilities.getTransactions(nodeName);
                    System.out.println(nodeKeys);
                    ledger.print();
                    // TODO: Connect with other nodes, if it fails it must reconnect later on but still success....
                    // TODO: This will only handle balance and history request if this node is connected to less than 3 nodes.
                    // TODO: Each node will have its own ledger (csv file) (appendix b)
                } else {
                    throw new Exception("Invalid node name.");
                }
            } else {
                throw new Exception("Arguments 'nodename' and 'port' are required.");
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

    private static KeyHolder getNodeKeys(String nodeName) {
        URL url = App.class.getResource(nodeName.concat(".pfx"));

        try {
            Path path = Paths.get(url.toURI());
            String password = nodeName.concat("qwerty");

            PublicKey publicKey = KeyLoader.loadPublicKey(path, nodeName, password);
            PrivateKey privateKey = KeyLoader.loadPrivateKey(path, nodeName, password, password);

            return new KeyHolder(publicKey, privateKey);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return null;
    }
}
