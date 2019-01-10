import model.KeyHolder;
import model.ServerNode;
import util.ArgumentParser;
import security.KeyLoader;
import util.Nodes;

import java.net.URL;
import java.nio.channels.Selector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

public class Node {

    /**
     *
     * @param args - ['nodename=****'], ['port=****']
     */
    public static void main(String[] args) {
        Map<String, String> map;

        try {
            map = ArgumentParser.convertArgsToMap(args, "=");

            if (Node.isMapArgumentsValid(map)) {
                final String nodeName = map.get("nodename");
                final KeyHolder nodeKeys = Node.getNodeKeys(nodeName);

                if (nodeKeys != null) {
                    final int portNumber = Integer.parseInt(map.get("port"));
                    final Selector selector = Selector.open();
                    final List<ServerNode> list = Nodes.getServerNodes()
                            .stream().filter(serverNode -> !serverNode.getName().equals(nodeName))
                            .collect(Collectors.toList());

                    NodeServer nodeServer = new NodeServer(selector, list, nodeName, nodeKeys);
                    nodeServer.startServer(portNumber);
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

    private static KeyHolder getNodeKeys(String nodeName) {
        URL url = Node.class.getResource(nodeName.concat(".pfx"));

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

    private static boolean isMapArgumentsValid(Map<String, String> map) {
        return map.containsKey("nodename")
                && map.containsKey("port");
    }

}
