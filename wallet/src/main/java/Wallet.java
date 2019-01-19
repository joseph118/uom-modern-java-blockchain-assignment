import communication.GlobalSignatures;
import communication.TransactionVerification;
import util.Resource;
import data.Transaction;
import security.SignatureVerifier;
import util.Parser;
import data.Command;
import data.KeyHolder;
import data.ServerNode;
import security.KeyLoader;
import security.SignatureBuilder;
import util.Nodes;
import util.Response;

import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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
            map = Parser.convertArgsToMap(args, "=");

            if (Wallet.isMapValid(map)) {
                final Command userCommand = Parser.convertToCommand(map.get("command").toUpperCase());
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

                    final URL userKeyResource = Resource.getResource(username, ".pfx");
                    if (userKeyResource != null) {
                        final String nodeName = map.get("nodename");
                        final ServerNode node = Nodes.getServerNode(nodeName);

                        if (node != null) {
                            final URL nodeCertificateResource = Resource.getResource(nodeName, ".crt");
                            final KeyHolder keyHolder = Resource
                                    .getWalletKeys(username, userPassword, userKeyResource, nodeCertificateResource);

                            if (keyHolder != null) {
                                final String walletKey = KeyLoader.encodePublicKey(keyHolder.getPublicKey());

                                if (!walletKey.equals(destinationKey)) {
                                    System.out.println("\n Sending Request... \n");

                                    InetSocketAddress nodeAddress = new InetSocketAddress(node.getIp(), node.getPort());
                                    SocketChannel client = SocketChannel.open(nodeAddress);

                                    if (client.isConnected()) {
                                        ByteBuffer buffer = ByteBuffer.allocate(2048);

                                        final String request =
                                                generateWalletRequestMessage(keyHolder, userCommand, destinationKey, amount);

                                        client.write(ByteBuffer.wrap(request.getBytes()));

                                        Wallet.processServerResponse(client, buffer, keyHolder, userCommand);
                                    }
                                } else {
                                    throw new Exception("Cannot transfer money to your self.");
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
                    throw new Exception("Command must be one of the following:\n data.balance\n data.history\n transfer");
                }
            } else {
                throw new Exception("Arguments 'username', 'nodename', and 'command' are required.");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(-1);
        }
    }

    private static void processServerResponse(SocketChannel client, ByteBuffer buffer, KeyHolder keyHolder, Command command) {
        try {
            int bytesRead = client.read(buffer);
            if (bytesRead > 0) {
                byte[] byteArray = new byte[bytesRead];
                buffer.flip();
                buffer.get(byteArray);

                String response = new String(buffer.array()).trim();
                Map<String, String> responseMap = Parser
                        .convertArgsToMap(response.split(","), "=");
                buffer.clear();

                final boolean containsError = Response.isError(responseMap);

                if (containsError) {
                    final String errorMessage = responseMap.get("error");

                    System.out.println(errorMessage != null
                            ? new String(Base64.getDecoder().decode(errorMessage))
                            : "Unexpected core.message between server and client.");
                } else {
                    if (command.equals(Command.TRANSFER)) {
                        if (Response.isNodeConfirmResponseValid(responseMap)) {
                            Transaction transaction = Transaction.mapResponseToTransaction(responseMap);
                            final String nodeName = responseMap.get("nodename");
                            final String signature = responseMap.get("signature");

                            if (TransactionVerification.isNodeVerifiedTransactionSignatureValid(keyHolder.getNodePublicKey(), signature, nodeName, transaction)) {
                                if (TransactionVerification.isTransactionValid(transaction)) {
                                    String confirmationMessage = generateConfirmationMessage(keyHolder, transaction);

                                    client.write(ByteBuffer.wrap(confirmationMessage.getBytes()));

                                    processServerResponse(client, buffer, keyHolder, Command.CONFIRM); // Recursive
                                } else {
                                    System.out.println("Verification on node signatures failed.");
                                }
                            } else {
                                System.out.println("Node signature is invalid.");
                            }
                        } else {
                            System.out.println("Node response is invalid");
                        }
                    } else {
                        final String payloadEncoded = responseMap.get("payload");
                        final String payload = new String(Base64.getDecoder().decode(payloadEncoded));
                        final String signature = responseMap.get("signature");

                        System.out.println( Wallet.verifyNodeSignature(keyHolder.getNodePublicKey(), payload, signature)
                                ? payload.replace(',', '\n')
                                : "Invalid signature from the server." );

                        client.close();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            System.exit(-1);
        }
    }

    private static boolean isMapValid(Map<String, String> map) {
        return map.containsKey("username")
                && map.containsKey("nodename")
                && map.containsKey("command");
    }

    private static boolean verifyNodeSignature(PublicKey nodePublicKey,
                                               String payload,
                                               String signature) {
        SignatureVerifier signatureVerifier = new SignatureVerifier(nodePublicKey);
        signatureVerifier.addData(payload);

        return signatureVerifier.verify(signature);
    }



    private static String generateConfirmationMessage(KeyHolder keyHolder, Transaction transaction) {
        final String signature = GlobalSignatures.generateVerifiedTransactionSignature(keyHolder.getPrivateKey(), transaction);

        return "signature=".concat(signature)
                .concat(",command=").concat(Command.CONFIRM.name())
                .concat(",key=").concat(KeyLoader.encodePublicKey(keyHolder.getPublicKey()))
                .concat(",").concat(transaction.toTransactionVerifiedResponseRow());
    }

    private static String generateWalletRequestMessage(KeyHolder walletKey,
                                                       Command userCommand,
                                                       String destinationKey,
                                                       float amount) {
        StringBuilder requestBuilder = new StringBuilder();

        final SignatureBuilder signatureBuilder = new SignatureBuilder(walletKey.getPrivateKey());
        final String encodedPublicKey = KeyLoader.encodePublicKey(walletKey.getPublicKey());

        requestBuilder.append("key=").append(encodedPublicKey)
                .append(",command=").append(userCommand.name());

        if (userCommand.equals(Command.TRANSFER)) {
            final String amountString = Parser.convertAmountToString(amount);
            final String guid = UUID.randomUUID().toString();

            signatureBuilder.addData(guid)
                    .addData(encodedPublicKey)
                    .addData(destinationKey)
                    .addData(amountString);

            requestBuilder.append(",amount=").append(amountString)
                    .append(",destinationkey=").append(destinationKey)
                    .append(",guid=").append(guid);
        }

        requestBuilder.append(",signature=")
                .append(signatureBuilder.sign());

        return requestBuilder.toString();
    }
}