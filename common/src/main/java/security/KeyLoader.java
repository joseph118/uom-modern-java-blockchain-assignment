package mt.edu.um.las3006.assignment.security;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * A utility class to load private and public keys from files and encode / decode them in Base64
 * encoding.
 *
 * <p>This class expects a PKCS12 encoded keystore, with keys using the ECDSA algorithm. Public keys
 * are expected to be in X.509 format.
 *
 * @author Josef Bajada - josef.bajada@um.edu.mt
 * @see PublicKey
 * @see PrivateKey
 * @see Base64
 */
public class KeyLoader {

  private static final String KEYSTORE_FORMAT = "PKCS12";
  private static final String CERTIFICATE_FORMAT = "X.509";
  private static final String KEY_ALG = "EC";

  private KeyLoader() {
    // this class only provides static methods
  }

  /**
   * Loads the private key from the specified PKCS12 key store.
   *
   * @param path - the path to the keystore
   * @param alias - the alias of the key to load
   * @param keystorePassword - the keystore password
   * @param keyPassword - the password of the specified key identified by the alias
   * @return the private key
   * @see PrivateKey
   */
  public static PrivateKey loadPrivateKey(
      Path path, String alias, String keystorePassword, String keyPassword) {
    try (InputStream inputStream = Files.newInputStream(path)) {

      KeyStore keystore = KeyStore.getInstance(KEYSTORE_FORMAT);
      keystore.load(inputStream, keystorePassword.toCharArray());

      return (PrivateKey) keystore.getKey(alias, keyPassword.toCharArray());
    } catch (Exception ex) {
      throw new KeyLoadingException(ex);
    }
  }

  /**
   * Loads the public key from the specified PKCS12 key store.
   *
   * @param path - the path to the keystore
   * @param alias - the alias of the key pair for which the public key is needed
   * @param keystorePassword - the keystore password
   * @return the public key
   * @see PublicKey
   */
  public static PublicKey loadPublicKey(Path path, String alias, String keystorePassword) {
    try (InputStream inputStream = Files.newInputStream(path)) {

      KeyStore keystore = KeyStore.getInstance(KEYSTORE_FORMAT);
      keystore.load(inputStream, keystorePassword.toCharArray());

      Certificate certificate = keystore.getCertificate(alias);
      return certificate.getPublicKey();
    } catch (Exception ex) {
      throw new KeyLoadingException(ex);
    }
  }

  /**
   * Loads the public key from an X.509 certificate file.
   *
   * @param certificatePath - the path to the certificate file.
   * @return the public key
   * @see PublicKey
   */
  public static PublicKey loadPublicKey(Path certificatePath) {
    try (InputStream inputStream = Files.newInputStream(certificatePath)) {

      CertificateFactory cf = CertificateFactory.getInstance(CERTIFICATE_FORMAT);
      Certificate certificate = cf.generateCertificate(inputStream);

      return certificate.getPublicKey();
    } catch (Exception ex) {
      throw new KeyLoadingException(ex);
    }
  }

  /**
   * Encodes a public key to a Base64 encoded string.
   *
   * @param publicKey - the public key to encode
   * @return a Base64 encoded representation of the specified public key
   * @see Base64
   */
  public static String encodePublicKey(PublicKey publicKey) {
    return Base64.getEncoder().encodeToString(publicKey.getEncoded());
  }

  /**
   * Decodes a Base64 encoded public key into a {@link PublicKey} instance.
   *
   * @param encodedPublicKey - the Base64 encoded string representing the public key
   * @return an instance of PublicKey
   * @see Base64
   */
  public static PublicKey decodePublicKey(String encodedPublicKey) {
    try {
      byte[] decodedBytes = Base64.getDecoder().decode(encodedPublicKey.getBytes());
      return KeyFactory.getInstance(KEY_ALG).generatePublic(new X509EncodedKeySpec(decodedBytes));
    } catch (Exception ex) {
      throw new KeyLoadingException(ex);
    }
  }

  /**
   * An unchecked exception wrapping any of the exception from java.security that could occur when
   * loading and processing private and public keys.
   */
  private static class KeyLoadingException extends RuntimeException {
    private KeyLoadingException(Throwable cause) {
      super(cause);
    }
  }
}
