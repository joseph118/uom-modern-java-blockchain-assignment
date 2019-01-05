import exception.ArgumentsNotFoundException;
import model.*;
import security.SignatureBuilder;
import security.SignatureVerifier;
import util.ArgumentParser;
import security.KeyLoader;
import util.Commands;
import util.Nodes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class App {

    /**
     *
     * @param args - ['nodename=****'], ['port=****']
     */
    public static void main(String[] args) {
        Map<String, String> map;
        boolean run = true;

        try {
            map = ArgumentParser.convertArgsToMap(args, "=");

            if (App.isMapArgumentsValid(map)) {
                final String nodeName = map.get("nodename");
                final KeyHolder nodeKeys = App.getNodeKeys(nodeName);

                if (nodeKeys != null) {
                    final int portNumber = Integer.parseInt(map.get("port"));

                    final List<ServerNode> serverNodes = Nodes.getServerNodes();

                    final Selector selector = Selector.open();

                    final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.socket().bind(new InetSocketAddress(portNumber));
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                    // 1 Byte -> 1 char
                    final ByteBuffer buffer = ByteBuffer.allocate(1048);

                    // TODO: Connect with other nodes, if it fails it must reconnect later on
                    //  but still success....
                    //  create a thread which will stay idle to handle the connections while still
                    //  receiving connection
                    //  check daemon threads....

                    // Check NIO slides.... for client-side socket channel

                    while (run) {
                        selector.select();

                        final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        final Iterator<SelectionKey> iterator = selectedKeys.iterator();

                        while (iterator.hasNext()) {
                            final SelectionKey key = iterator.next();

                            if (key.isValid() && key.isAcceptable()) {
                                App.registerClient((ServerSocketChannel) key.channel(), selector);

                            } else if (key.isValid() && key.isReadable()) {
                                App.processRequest(nodeName, key, buffer);

                            }

                            iterator.remove();
                        }
                    }

                    serverSocketChannel.close();
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

    private static void registerClient(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        final SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        System.out.println("Connection Accepted: " + client.getLocalAddress().toString() + "\n");
    }

    private static void processRequest(String nodeName, SelectionKey key, ByteBuffer buffer) throws IOException, ArgumentsNotFoundException {
        final SocketChannel client = (SocketChannel) key.channel();
        client.read(buffer);

        final String data = new String(buffer.array()).trim();
        final Map<String, String> requestMessage = ArgumentParser
                .convertArgsToMap(data.split(","), "=");
        buffer.clear();

        if (App.isRequestArgumentsValid(requestMessage)) {
            final String signature = requestMessage.get("signature");
            final String command = requestMessage.get("command");
            final String base64PublicKey = requestMessage.get("key");
            final PublicKey publicKey = KeyLoader.decodePublicKey(base64PublicKey);

            if (App.verifyUserSignature(publicKey, command, signature)) {
                Command userCommand = Commands.convertToCommand(command);

                if (userCommand.equals(Command.TRANSFER)) {

                } else if (userCommand.equals(Command.HISTORY)) {
                    String history = Ledger.getUserHistoryAsString(nodeName, base64PublicKey);

                    client.write(ByteBuffer.wrap(history.getBytes()));
                    client.close();
                } else if (userCommand.equals(Command.BALANCE)) {
                    String balance = Ledger.getUserBalance(nodeName, base64PublicKey);

                    client.write(ByteBuffer.wrap(balance.getBytes()));
                    client.close();
                } else {
                    client.write(ByteBuffer.wrap("Invalid command".getBytes()));
                    client.close();
                }
            } else {
                System.out.println("Invalid signature");
                client.write(ByteBuffer.wrap("Invalid signature".getBytes()));
                client.close();
            }
        } else {
            System.out.println("Invalid arguments");
            client.write(ByteBuffer.wrap("Invalid arguments".getBytes()));
            client.close();
        }
    }

    private static boolean verifyUserSignature(PublicKey key, String command, String signature) {
        SignatureVerifier signatureVerifier = new SignatureVerifier(key);
        signatureVerifier.addData(command);

        return signatureVerifier.verify(signature);
    }

    private static boolean isRequestArgumentsValid(Map<String, String> map) {
        if (map.containsKey("signature")
                && map.containsKey("key")
                && map.containsKey("command")) {

            if (map.get("command").equals(Command.TRANSFER.name())) {
                return true;
            } else {
                return true;
            }
        }

        return false;
    }

    private static boolean isMapArgumentsValid(Map<String, String> map) {
        return map.containsKey("nodename")
                && map.containsKey("port");
    }

}
