package security;

import static org.junit.Assert.assertNotNull;

import java.security.PrivateKey;
import org.junit.Test;

/**
 * Tests to verify the SignatureBuilder signing methods.
 * 
 * @author Josef Bajada - josef.bajada@um.edu.mt
 */
public class SignatureBuilderTest {

  private PrivateKey privateKey = SecurityTestUtils.loadTestPrivateKey();
  
  @Test
  public void sign() {
    SignatureBuilder signatureBuilder = new SignatureBuilder(privateKey);
    SecurityTestUtils.populateSignatureTestData(signatureBuilder);
    String signature = signatureBuilder.sign();
    
    assertNotNull(signature);
  }
}