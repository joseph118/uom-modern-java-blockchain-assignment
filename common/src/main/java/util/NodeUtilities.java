package util;

import model.ServerNode;

import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class NodeUtilities {
    private NodeUtilities() {
        // this class only provides static methods
    }

    private static final String propertyNodeFileName = "nodes.properties";

    public static List<ServerNode> getNodes() {
        final Properties properties = NodeUtilities.getProperties();

        if (properties != null) {
            List<ServerNode> serverNodes = new ArrayList<>();
            Enumeration e = properties.propertyNames();

            while (e.hasMoreElements()) {
                String nodeName = (String) e.nextElement();
                String data = properties.getProperty(nodeName);

                serverNodes.add(NodeUtilities.mapToServerNode(nodeName, data));
            }

            return serverNodes;
        }

        return null;
    }

    public static ServerNode getNode(String nodeName) {
        final String data = NodeUtilities.getServerNode(nodeName);

        if (data != null) {
            return NodeUtilities.mapToServerNode(nodeName, data);
        }

        return null;
    }

    private static String getServerNode(String nodeName) {
        final Properties properties = NodeUtilities.getProperties();

        return properties == null
                ? null
                : properties.getProperty(nodeName, null);
    }

    private static Properties getProperties() {
        URL url = ClassLoader.getSystemResource(NodeUtilities.propertyNodeFileName);

        try (final FileInputStream fis = new FileInputStream(url.getFile())) {
            final Properties properties = new Properties();
            properties.load(fis);

            return properties;
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return null;
    }

    private static ServerNode mapToServerNode(String nodeName, String data) {
        final String[] ipAndPort = data.split(":");
        final String ip = ipAndPort[0];
        final String port = ipAndPort[1];

        return new ServerNode(nodeName, ip, port);
    }
}
