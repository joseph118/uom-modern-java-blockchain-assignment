import util.ArgumentParser;
import model.Command;
import model.KeyHolder;
import model.ServerNode;
import security.KeyLoader;
import security.SignatureBuilder;
import util.Commands;
import util.Nodes;

import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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
            map = ArgumentParser.convertArgsToMap(args, "=");

            if (App.isMapValid(map)) {
                final Command userCommand = Commands.convertToCommand(map.get("command").toUpperCase());
                if (!userCommand.equals(Command.OTHER)) {
                    final String username = map.get("username");

                    System.out.println("Hi "
                            .concat(username)
                            .concat(", \n Kindly input your password: \n"));

                    final Scanner scanner = new Scanner(System.in);
                    final String userPassword = scanner.nextLine();

                    final URL userKeyResource = App.getResource(username, ".pfx");
                    if (userKeyResource != null) {
                        final KeyHolder userKeys = App.getUserKey(username, userPassword, userKeyResource);
                        if (userKeys != null) {
                            final String nodeName = map.get("nodename");
                            final ServerNode node = Nodes.getServerNode(nodeName);

                            if (node != null) {
                                //final URL nodeCertificateResource = App.getResource(nodeName, ".crt");

                                InetSocketAddress nodeAddress = new InetSocketAddress(node.getIp(), node.getPort());
                                SocketChannel client = SocketChannel.open(nodeAddress);

                                if (client.isConnected()) {
                                    ByteBuffer buffer = ByteBuffer.allocate(2048);
                                    StringBuilder requestBuilder = new StringBuilder();

                                    final SignatureBuilder signatureBuilder = new SignatureBuilder(userKeys.getPrivateKey());
                                    signatureBuilder.addData(userCommand.name());

                                    requestBuilder.append("signature=")
                                            .append(signatureBuilder.sign())
                                            .append(",key=").append(KeyLoader.encodePublicKey(userKeys.getPublicKey()))
                                            .append(",command=").append(userCommand.name());

                                    if (userCommand.equals(Command.TRANSFER)) {

                                    }

                                    client.write(ByteBuffer.wrap(requestBuilder.toString().getBytes()));

                                    int bytesRead = client.read(buffer);
                                    if (bytesRead > 0) {
                                        byte[] byteArray = new byte[bytesRead];
                                        buffer.flip();
                                        buffer.get(byteArray);

                                        String response = new String(buffer.array()).trim();
                                        System.out.println(response);
                                    }

                                    client.close();
                                }
                            } else {
                                throw new Exception("Nodes name doesn't exists.");
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

    private static URL getResource(String name, String extension) {
        return App.class.getResource(name.concat(extension));
    }
}