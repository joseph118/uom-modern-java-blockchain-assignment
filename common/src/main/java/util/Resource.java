package util;

import data.KeyHolder;
import security.KeyLoader;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Resource {
    public static KeyHolder getWalletKeys(String name, String password, URL userResource, URL nodeResource) {

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

    public static KeyHolder getNodeKeys(String nodeName) {
        final URL url = getResource(nodeName,".pfx");

        try {
            final Path path = Paths.get(url.toURI());
            final String password = nodeName.concat("qwerty");

            final PublicKey publicKey = KeyLoader.loadPublicKey(path, nodeName, password);
            final PrivateKey privateKey = KeyLoader.loadPrivateKey(path, nodeName, password, password);

            return new KeyHolder(publicKey, privateKey);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return null;
    }

    public static Path getNodeCertificate(String nodeName) {
        final URL nodeCertificate = getResource(nodeName, ".crt");
        Path path;

        try {
            path = Paths.get(nodeCertificate.toURI());
        } catch (URISyntaxException ex) {
            path = null;
        }

        return path;
    }

    public static URL getResource(String name, String extension) {
        return Resource.class.getResource(name.concat(extension));
    }
}
