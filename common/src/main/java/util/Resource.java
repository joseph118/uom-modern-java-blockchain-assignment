package util;

import data.KeyHolder;
import security.KeyLoader;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Resource {
    private Resource() {

    }
    
    public static KeyHolder getWalletKeys(String name, String password, Path userPath, Path nodePath) {

        try {
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
        try {
            final Path path = getResource(nodeName,".pfx");
            System.out.println(path);
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
        return getResource(nodeName, ".crt");
    }

    public static Path getResource(String name, String extension) {
        try {
            File directory = new File("./");
            String stringPath = directory.getAbsolutePath()
                    .substring(0, directory.getAbsolutePath().length() - 1)
                    .concat("resources\\").concat(name).concat(extension);

            return Paths.get(stringPath);
        } catch (Exception e) {
            return  null;
        }
    }

}
