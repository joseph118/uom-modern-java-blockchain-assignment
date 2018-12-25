package security;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Utility methods for testing the signing classes with some test data and test keys.
 * 
 * @author Josef Bajada - josef.bajada@um.edu.mt
 */
class SecurityTestUtils {
  static final String KEYSTORE_FILENAME = "src/test/resources/alice.pfx";
  static final String CERTIFICATE_FILENAME = "src/test/resources/alice.crt";
  static final String KEY_ALIAS = "alice";
  static final String KEYSTORE_PASSWORD = "alice1234";
  static final String KEY_PASSWORD = "alice1234";

  static final String ENCODED_PUBKEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEU1qA6B6ypl0/w9CQ8SRSsMNGKA1+204MGG0snLBV6JskPdBDi7wigCyY8zQLgYfaO7KQ5psBaNDLQzKlueEyw==";


  static void populateSignatureTestData(SignatureProxy signatureProxy) {
    signatureProxy.addData("1234")
        .addData("source")
        .addData("destination")
        .addData(Float.toString(100.50f));
  }

  static PrivateKey loadTestPrivateKey()  {
    return KeyLoader.loadPrivateKey(getTestKeystorePath(), KEY_ALIAS, KEYSTORE_PASSWORD, KEY_PASSWORD);
  }

  static PublicKey loadTestPublicKey()  {
    return KeyLoader.loadPublicKey(getTestCertificatePath());
  }

  static Path getTestKeystorePath() {
    try {
      URI filename = Thread.currentThread().getContextClassLoader().getResource(KEYSTORE_FILENAME).toURI();
      return Paths.get(filename);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  static Path getTestCertificatePath() {
    try {
      URI filename = Thread.currentThread().getContextClassLoader().getResource(CERTIFICATE_FILENAME).toURI();
      return Paths.get(filename);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
