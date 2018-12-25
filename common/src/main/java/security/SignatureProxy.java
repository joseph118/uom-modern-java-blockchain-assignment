package security;

import java.nio.charset.StandardCharsets;
import java.security.Signature;

/**
 * Abstract helper class extended by Signature utility classes.
 *
 * The algorithm used for signing is SHA256 hashing with ECDSA.
 * 
 * @author Josef Bajada - josef.bajada@um.edu.mt
 * @see Signature
 */
abstract class SignatureProxy<T extends SignatureProxy<T>>  {
  static final String SIGNING_ALG = "SHA256withECDSA";

  private final Signature signature;

  SignatureProxy(Signature signature) {
    this.signature = signature;
  }

  Signature getSignature() {
    return signature;
  }

  /**
   * Adds the specified string to be used when generating or verifying a signature.
   * The string is converted to bytes internally using the UTF-8 encoding.
   * @param data - the string to add to be signed or verified
   * @return the same instance in order to chain this method with other calls in a fluent API fashion
   */
  public T addData(String data) {
    return this.addData(data.getBytes(StandardCharsets.UTF_8));
  }

  @SuppressWarnings("unchecked")  //we know what we are doing, this is using a self-bound generic type
  private T addData(byte[] data) {
    try {
      signature.update(data);
      return (T) this;
    } catch (Exception ex) {
      throw new SigningException(ex);
    }
  }


  static class SigningException extends RuntimeException {
    SigningException(Throwable cause) {
      super(cause);
    }
  }
}
