package security;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static security.SecurityTestUtils.ENCODED_PUBKEY;
import static security.SecurityTestUtils.KEYSTORE_PASSWORD;
import static security.SecurityTestUtils.KEY_ALIAS;
import static security.SecurityTestUtils.KEY_PASSWORD;

import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.Test;

/**
 * A set of tests to verify the functionality of the KeyLoader class.
 * 
 * @author Josef Bajada - josef.bajada@um.edu.mt
 */
public class KeyLoaderTest  {
  
  @Test
  public void loadPrivateKey() {
    PrivateKey key = KeyLoader.loadPrivateKey(SecurityTestUtils.getTestKeystorePath(), KEY_ALIAS, KEYSTORE_PASSWORD, KEY_PASSWORD);
    assertNotNull(key);
  }

  @Test
  public void loadPublicKeyFromKeystore() {
    PublicKey publicKey = KeyLoader.loadPublicKey(SecurityTestUtils.getTestKeystorePath(), KEY_ALIAS, KEYSTORE_PASSWORD);
    assertNotNull(publicKey);
  }

  @Test
  public void loadPublicKeyFromCertificate() {
    PublicKey publicKey = KeyLoader.loadPublicKey(SecurityTestUtils.getTestCertificatePath());
    assertNotNull(publicKey);
  }


  @Test
  public void encodePublicKey(){
    PublicKey publicKey = SecurityTestUtils.loadTestPublicKey();
    assertNotNull(publicKey);
    String encoded = KeyLoader.encodePublicKey(publicKey);
    assertEquals(ENCODED_PUBKEY, encoded);
  }

  @Test
  public void decodePublicKey(){
    PublicKey publicKey = KeyLoader.decodePublicKey(ENCODED_PUBKEY);
    assertNotNull(publicKey);
  }
}