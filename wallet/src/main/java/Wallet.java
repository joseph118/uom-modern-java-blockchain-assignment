import security.SignatureVerifier;
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
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class Wallet {

    /**
     *
     * @param args - ['username=****'], ['nodename=****'], ['command=****']
     */
    public static void main(String[] args) {
        Map<String, String> map;

        try {
            map = ArgumentParser.convertArgsToMap(args, "=");

            if (Wallet.isMapValid(map)) {
                final Command userCommand = Commands.convertToCommand(map.get("command").toUpperCase());
                if (!userCommand.equals(Command.OTHER)) {
                    final String username = map.get("username");

                    System.out.println("Hi "
                            .concat(username)
                            .concat(", \nKindly input your password: "));

                    final Scanner scanner = new Scanner(System.in);
                    final String userPassword = scanner.nextLine();

                    float amount = 0;
                    String destinationKey = null;

                    if (userCommand.equals(Command.TRANSFER)) {
                        System.out.println("Insert the amount, followed by the Base64 encoded public key: ");
                        final String[] data = scanner.nextLine().split(" ");

                        amount = Float.parseFloat(data[0]);
                        destinationKey = data[1];

                        if (amount == 0 || destinationKey.isEmpty()) {
                            throw new Exception("Invalid data.");
                        }
                    }

                    final URL userKeyResource = Wallet.getResource(username, ".pfx");
                    if (userKeyResource != null) {
                        final String nodeName = map.get("nodename");
                        final ServerNode node = Nodes.getServerNode(nodeName);

                        if (node != null) {
                            final URL nodeCertificateResource = Wallet.getResource(nodeName, ".crt");
                            final KeyHolder keyHolder = Wallet
                                    .getKeys(username, userPassword, userKeyResource, nodeCertificateResource);

                            if (keyHolder != null) {
                                InetSocketAddress nodeAddress = new InetSocketAddress(node.getIp(), node.getPort());
                                SocketChannel client = SocketChannel.open(nodeAddress);

                                if (client.isConnected()) {
                                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                                    StringBuilder requestBuilder = new StringBuilder();

                                    final SignatureBuilder signatureBuilder = new SignatureBuilder(keyHolder.getPrivateKey());
                                    signatureBuilder.addData(userCommand.name());

                                    requestBuilder.append("key=").append(KeyLoader.encodePublicKey(keyHolder.getPublicKey()))
                                            .append(",command=").append(userCommand.name());

                                    if (userCommand.equals(Command.TRANSFER)) {
                                        final String amountString = String.valueOf(amount);
                                        final String guid = UUID.randomUUID().toString();

                                        signatureBuilder.addData(amountString)
                                                .addData(destinationKey)
                                                .addData(guid);

                                        requestBuilder.append(",amount=").append(amountString)
                                                .append(",destinationKey=").append(destinationKey)
                                                .append(",guid=").append(guid);
                                    }

                                    requestBuilder.append(",signature=")
                                            .append(signatureBuilder.sign());

                                    client.write(ByteBuffer.wrap(requestBuilder.toString().getBytes()));

                                    Wallet.processServerResponse(client, buffer, keyHolder);
                                }
                            } else {
                                throw new Exception("Password is incorrect.");
                            }
                        } else {
                            throw new Exception("Nodes name doesn't exists.");
                        }
                    } else {
                        throw new Exception("Username is incorrect.");
                    }
                } else {
                    throw new Exception("Command must be one of the following:\n core.balance\n core.history\n transfer");
                }
            } else {
                throw new Exception("Arguments 'username', 'nodename', and 'command' are required.");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(-1);
        }
    }

    private static void processServerResponse(SocketChannel client, ByteBuffer buffer, KeyHolder keyHolder) {
        try {
            int bytesRead = client.read(buffer);
            if (bytesRead > 0) {
                byte[] byteArray = new byte[bytesRead];
                buffer.flip();
                buffer.get(byteArray);

                String response = new String(buffer.array()).trim();
                Map<String, String> responseMap = ArgumentParser
                        .convertArgsToMap(response.split(","), "=");

                final boolean containsError = responseMap.containsKey("error");

                if (containsError || responseMap.isEmpty()) {
                    final String errorMessage = containsError
                            ? responseMap.get("error")
                            : "Unexpected core.message between server and client.";

                    System.out.println(errorMessage);
                } else {
                    final String payloadEncoded = responseMap.get("payload");
                    final String payload = new String(Base64.getDecoder().decode(payloadEncoded));
                    final String signature = responseMap.get("signature");

                    if (Wallet.verifyNodeSignature(keyHolder.getNodePublicKey(), payload, signature)) {
                        System.out.println(payload);
                    } else {
                        System.out.println("Invalid signature from the server.");
                    }
                }

                client.close();
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

    private static KeyHolder getKeys(String name, String password, URL userResource, URL nodeResource) {

        try {
            final Path userPath = Paths.get(userResource.toURI());
            final Path nodePath = Paths.get(nodeResource.toURI());

            final PublicKey publicKey = KeyLoader.loadPublicKey(userPath, name, password);
            final PrivateKey privateKey = KeyLoader.loadPrivateKey(userPath, name, password, password);
            final PublicKey nodePublicKey = KeyLoader.loadPublicKey(nodePath);

            return new KeyHolder(publicKey, privateKey, nodePublicKey);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return null;
    }

    private static URL getResource(String name, String extension) {
        return Wallet.class.getResource(name.concat(extension));
    }

    private static boolean verifyNodeSignature(PublicKey nodePublicKey,
                                               String payload,
                                               String signature) {
        SignatureVerifier signatureVerifier = new SignatureVerifier(nodePublicKey);
        signatureVerifier.addData(payload);

        return signatureVerifier.verify(signature);
    }
}