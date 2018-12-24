package mt.edu.um.las3006.assignment.security;

import static mt.edu.um.las3006.assignment.security.SecurityTestUtils.ENCODED_PUBKEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.PublicKey;
import org.junit.Test;

/**
 * Tests to verify the SignatureVerifier signature verification methods.
 * 
 * @author Josef Bajada - josef.bajada@um.edu.mt
 */
public class SignatureVerifierTest {

  private static final String GOOD_SIGNATURE = "MEUCIQDj3G/TrJYFBt36jw2YmxhBObjFCqUUneWVKZk9hLG4hAIgDeBlzmN/M3Wdy1AK00R+6HrnNtzws3DgsUdAd0MM7pA=";

  private static final String INVALID_SIGNATURE = "MEUCIQDmK8PMvfRNiQht+VpQjMAjSKu8SWGnPmkarXqvZ+i+ggIgG4l3RwTHi/2Obf+iUW1ne9/w6DV8/YLHtV9qzGPrVRM=";

  @Test
  public void verifyValid() throws Exception {
    PublicKey publicKey = KeyLoader.decodePublicKey(ENCODED_PUBKEY);
    SignatureVerifier signatureVerifier = new SignatureVerifier(publicKey);
    SecurityTestUtils.populateSignatureTestData(signatureVerifier);

    assertTrue(signatureVerifier.verify(GOOD_SIGNATURE));
  }

  @Test
  public void verifyInvalid() throws Exception {
    PublicKey publicKey = KeyLoader.decodePublicKey(ENCODED_PUBKEY);
    SignatureVerifier signatureVerifier = new SignatureVerifier(publicKey);
    SecurityTestUtils.populateSignatureTestData(signatureVerifier);

    assertFalse(signatureVerifier.verify(INVALID_SIGNATURE));
  }
}