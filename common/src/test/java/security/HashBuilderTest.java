package security;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Josef Bajada
 */
public class HashBuilderTest {

  private static final String EXPECTED_SINGLEPART_HASH = "k36NX7tIvUlJU2zWW401xCa4DS+DDFwwjizexCKuIkQ=";

  private static final String EXPECTED_MULTIPART_HASH = "IAW3d6ZYMlOkTKAFp3SbBSerIjthenJCZYgBkQ4EDOU=";

  @Test
  public void hash() throws Exception {
    String hash = new HashBuilder("test1234").hash();
    assertEquals(EXPECTED_SINGLEPART_HASH, hash);
  }

  @Test
  public void multipartHash() throws Exception {
    String hash = new HashBuilder("test1234")
        .addData("test4567")
        .addData("test8910")
        .hash();

    assertEquals(EXPECTED_MULTIPART_HASH, hash);
  }
}