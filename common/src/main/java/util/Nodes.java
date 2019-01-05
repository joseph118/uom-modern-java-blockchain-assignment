package util;

import model.ServerNode;

import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class Nodes {
    private Nodes() {
        // this class only provides static methods
    }

    private static final String NODE_PROPERTY_FILENAME = "nodes.properties";

    public static List<ServerNode> getServerNodes() {
        final Properties properties = Nodes.getProperties();

        if (properties != null) {
            List<ServerNode> serverNodes = new ArrayList<>();
            Enumeration e = properties.propertyNames();

            while (e.hasMoreElements()) {
                String nodeName = (String) e.nextElement();
                String data = properties.getProperty(nodeName);

                serverNodes.add(Nodes.mapToServerNode(nodeName, data));
            }

            return serverNodes;
        }

        return null;
    }

    public static ServerNode getServerNode(String nodeName) {
        final Properties properties = Nodes.getProperties();

        if (properties != null) {
            return Nodes.mapToServerNode(nodeName,
                    properties.getProperty(nodeName, null));
        }

        return null;
    }

    private static Properties getProperties() {
        final URL url = ClassLoader.getSystemResource(Nodes.NODE_PROPERTY_FILENAME);

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
        final int port = Integer.parseInt(ipAndPort[1]);

        return new ServerNode(nodeName, ip, port);
    }
}
