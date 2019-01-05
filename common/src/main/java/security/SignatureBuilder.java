package security;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

/**
 * A helper class to sign process using the provided private key using the
 * signing algorithm specified by the super class.
 *
 * <p>You can use this class to sign a payload made of multiple parts as follows:</p>
 * <pre>
 * {@code
 *    String signature = new SignatureBuilder(privateKey)
 *        .addData("part1")
 *        .addData("part2")
 *        .sign();
 * }
 * </pre>
 *
 * @author Josef Bajada - josef.bajada@um.edu.mt
 * @see PrivateKey
 * @see KeyLoader#loadPrivateKey(Path, String, String, String)
 */
public class SignatureBuilder extends SignatureProxy<SignatureBuilder> {

  /**
   * Creates an instance of this class using the specified private key to
   * eventually generate a signature.
   *
   * @param privateKey - the private key to use to sign the payload process
   * @see SignatureProxy#addData(String)
   * @see SignatureBuilder#sign()
   */
  public SignatureBuilder(PrivateKey privateKey) {
    super(initialiseSignature(privateKey));
  }

  private static Signature initialiseSignature(PrivateKey privateKey) {
    try {
      Signature signature = Signature.getInstance(SIGNING_ALG);
      signature.initSign(privateKey);
      return signature;
    } catch (Exception ex) {
      throw new SigningException(ex);
    }
  }

  public String sign() {
    try {
      return Base64.getEncoder().encodeToString(getSignature().sign());
    } catch (SignatureException ex) {
      throw new SigningException(ex);
    }
  }
}
