package mt.edu.um.las3006.assignment.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Generates a hash out of the supplied data parts. This class uses the SHA-256 algorithm after
 * encoding the data parts to UTF-8.
 *
 * @implNote This class makes use of a StringBuilder to concatenate the data parts. Do not share
 *     this class between threads.
 * @author Josef Bajada - josef.bajada@um.edu.mt
 * @see StringBuilder
 * @see MessageDigest
 */
public class HashBuilder {
  private static final String HASHING_ALG = "SHA-256";

  private final StringBuilder stringBuilder;

  /**
   * Constructs a new instance that will hash this provided data. More data parts can be appended to
   * the initial data part using {@link HashBuilder#addData(String)}.
   *
   * @param initialData - the initial data part to hash
   */
  public HashBuilder(String initialData) {
    stringBuilder = new StringBuilder(initialData);
  }

  /**
   * Appends another data part to the data already accumulated to be hashed.
   * @param data - the data part to be appended
   * @return this instance, so that this method can be used in the style of a fluent API
   */
  public HashBuilder addData(String data) {
    stringBuilder.append(data);
    return this;
  }

  /**
   * Generates the hash code computed using SHA-256 of all the accumulated
   * data parts concatenated together, and encodes it using Base64.
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
