package security;

import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

/**
 * A helper class that verifies the signature using the specified public key,
 * using the signing algorithm specified by the super class.
 *
 * <p>You can use this class to verify a payload made of multiple parts as follows:</p>
 * <pre>
 * {@code
 *    boolean verified = new SignatureVerifier(publicKey)
 *        .addData("part1")
 *        .addData("part2")
 *        .verify(signature);
 * }
 * </pre>

 * @author Josef Bajada - josef.bajada@um.edu.mt
 * @see PublicKey
 * @see KeyLoader#decodePublicKey(String)
 */
public class SignatureVerifier extends SignatureProxy<SignatureVerifier> {

  /**
   * Creates an instance of this class using the specified private key to
   * eventually generate a signature.
   *
   * @param publicKey - the public key to use to verify the signature of the payload data
   * @see SignatureProxy#addData(String)
   * @see SignatureVerifier#verify(String)
   */
  public SignatureVerifier(PublicKey publicKey) {
    super(initialiseSignature(publicKey));
  }

  private static Signature initialiseSignature(PublicKey publicKey) {
    try {
      Signature signature = Signature.getInstance(SIGNING_ALG);
      signature.initVerify(publicKey);
      return signature;
    } catch (Exception ex) {
      throw new SigningException(ex);
    }
  }

  /**
   * Verifies the specified signature for the added payload using the initialised public key.
   *
   * @param signature - the signature to verify
   * @return true if the signature was verified, false otherwise
   */
  public boolean verify(String signature) {
    try {
      byte[] signatureData = Base64.getDecoder().decode(signature);
      return getSignature().verify(signatureData);
    } catch (SignatureException ex) {
      throw new SigningException(ex);
    }
  }
}
