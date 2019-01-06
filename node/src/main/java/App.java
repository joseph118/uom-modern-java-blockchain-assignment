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
                                App.processRequest(nodeName, key, buffer, nodeKeys.getPrivateKey());

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

    private static void processRequest(String nodeName, SelectionKey key, ByteBuffer buffer, PrivateKey privateKey) throws IOException, ArgumentsNotFoundException {
        final SocketChannel client = (SocketChannel) key.channel();
        client.read(buffer);

        final String clientData = new String(buffer.array()).trim();
        final Map<String, String> requestMessage = ArgumentParser
                .convertArgsToMap(clientData.split(","), "=");
        buffer.clear();

        if (App.isRequestArgumentsValid(requestMessage)) {
            final String command = requestMessage.get("command");
            Command userCommand = Commands.convertToCommand(command);

            final String signature = requestMessage.get("signature");
            final String base64PublicKey = requestMessage.get("key");
            final PublicKey publicKey = KeyLoader.decodePublicKey(base64PublicKey);

            if (userCommand.equals(Command.TRANSFER)) {
                final String destinationKey = requestMessage.get("destinationKey");
                final String guid = requestMessage.get("guid");
                final String stringAmount = requestMessage.get("amount");
                final float amount = Float.parseFloat(stringAmount);

                if (App.verifyUserSignature(publicKey, command, signature, destinationKey, guid, stringAmount)) {

                    // TODO: transfer....
                } else {
                    client.write(ByteBuffer.wrap("error=Invalid signature".getBytes()));
                    client.close();
                }
            } else if (userCommand.equals(Command.HISTORY) || userCommand.equals(Command.BALANCE)) {
                if (App.verifyUserSignature(publicKey, command, signature)) {
                    final String data = (userCommand.equals(Command.HISTORY))
                            ? Ledger.getUserHistoryAsString(nodeName, base64PublicKey)
                            : Ledger.getUserBalance(nodeName, base64PublicKey);

                    final String signedSignature = App.generateSignature(privateKey, data);
                    final String responseData = App.generateBody(signedSignature, data);

                    client.write(ByteBuffer.wrap(responseData.getBytes()));
                } else {
                    client.write(ByteBuffer.wrap("error=Invalid signature".getBytes()));
                }

                client.close();
            } else {
                client.write(ByteBuffer.wrap("error=Invalid command".getBytes()));
                client.close();
            }
        } else {
            client.write(ByteBuffer.wrap("error=Invalid arguments".getBytes()));
            client.close();
        }
    }

    private static boolean verifyUserSignature(PublicKey key,
                                               String command,
                                               String signature) {
        SignatureVerifier signatureVerifier = new SignatureVerifier(key);
        signatureVerifier.addData(command);

        return signatureVerifier.verify(signature);
    }

    private static boolean verifyUserSignature(PublicKey key,
                                               String command,
                                               String signature,
                                               String destinationKey,
                                               String guid,
                                               String amount) {

        SignatureVerifier signatureVerifier = new SignatureVerifier(key);
        signatureVerifier.addData(command)
                .addData(amount)
                .addData(destinationKey)
                .addData(guid);

        return signatureVerifier.verify(signature);
    }

    private static String generateSignature(PrivateKey privateKey, String data) {
        SignatureBuilder signatureBuilder = new SignatureBuilder(privateKey);
        signatureBuilder.addData(data);

        return signatureBuilder.sign();
    }

    private static boolean isRequestArgumentsValid(Map<String, String> map) {
        if (map.containsKey("signature")
                && map.containsKey("key")
                && map.containsKey("command")) {

            if (map.get("command").equals(Command.TRANSFER.name())) {
                return map.containsKey("destinationKey")
                        && map.containsKey("amount")
                        && map.containsKey("guid");
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

    private static String generateBody(String signature, String data) {
        final String encodedData = new String(Base64.getEncoder().encode(data.getBytes()));

        return "payload="
                .concat(encodedData)
                .concat(",signature=")
                .concat(signature);
    }

}
