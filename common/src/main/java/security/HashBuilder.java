package security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Generates a hash out of the supplied process parts. This class uses the SHA-256 algorithm after
 * encoding the process parts to UTF-8.
 *
 * @implNote This class makes use of a StringBuilder to concatenate the process parts. Do not share
 *     this class between threads.
 * @author Josef Bajada - josef.bajada@um.edu.mt
 * @see StringBuilder
 * @see MessageDigest
 */
public class HashBuilder {
  private static final String HASHING_ALG = "SHA-256";

  private final StringBuilder stringBuilder;

  /**
   * Constructs a new instance that will hash this provided process. More process parts can be appended to
   * the initial process part using {@link HashBuilder#addData(String)}.
   *
   * @param initialData - the initial process part to hash
   */
  public HashBuilder(String initialData) {
    stringBuilder = new StringBuilder(initialData);
  }

  /**
   * Appends another process part to the process already accumulated to be hashed.
   * @param data - the process part to be appended
   * @return this instance, so that this method can be used in the style of a fluent API
   */
  public HashBuilder addData(String data) {
    stringBuilder.append(data);
    return this;
  }

  /**
   * Generates the hash code computed using SHA-256 of all the accumulated
   * process parts concatenated together, and encodes it using Base64.
   *
   * @return a string containing the Base64 encoded hashcode.
   * @see Base64
   * @see MessageDigest
   */
  public String hash() {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASHING_ALG);
      byte[] hash = digest.digest(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (Exception ex) {
      throw new HashingException(ex);
    }
  }

  private class HashingException extends RuntimeException {
    HashingException(Throwable cause) {
      super(cause);
    }
  }
}
