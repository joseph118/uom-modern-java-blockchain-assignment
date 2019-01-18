import data.KeyHolder;
import data.ServerNode;
import util.Parser;
import util.Nodes;
import util.Resource;

import java.nio.channels.Selector;
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
            map = Parser.convertArgsToMap(args, "=");

            if (Node.isMapArgumentsValid(map)) {
                final String nodeName = map.get("nodename");
                final KeyHolder nodeKeys = Resource.getNodeKeys(nodeName);

                if (nodeKeys != null) {
                    final int portNumber = Integer.parseInt(map.get("port"));
                    final Selector selector = Selector.open();
                    final List<ServerNode> list = Nodes.getServerNodes()
                            .stream().filter(serverNode -> !serverNode.getName().equals(nodeName))
                            .collect(Collectors.toList());

                    NodeServer nodeServer = new NodeServer(list, nodeName, nodeKeys);
                    nodeServer.startServer(portNumber, selector);
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

    private static boolean isMapArgumentsValid(Map<String, String> map) {
        return map.containsKey("nodename")
                && map.containsKey("port");
    }

}
