package org.sagebionetworks.repo.model.grid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.grid.encoding.Vu57Utils;

public class Vu57UtilsTest {

  public enum Vu57TestCase {
    ZERO(0L, new byte[]{0x00}),
    SMALL_VALUE(1L, new byte[]{0x01}),
    MAX_SINGLE_BYTE(127L, new byte[]{0x7F}),
    TWO_BYTES(128L, new byte[]{(byte) 0x80, 0x01}),
    MAX_TWO_BYTES(16383L, new byte[]{(byte) 0xFF, 0x7F}),
    THREE_BYTES(16384L, new byte[]{(byte) 0x80, (byte) 0x80, 0x01}),
    LARGE_VALUE(1_000_000L, new byte[]{(byte) 0xc0, (byte) 0x84, (byte) 0x3d}),
    MAX_57_BITS((1L << 57) - 1, new byte[]{
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    });

    final long value;
    final byte[] expectedEncoding;

    Vu57TestCase(long value, byte[] expectedEncoding) {
      this.value = value;
      this.expectedEncoding = expectedEncoding;
    }
  }

  @ParameterizedTest
  @EnumSource(Vu57TestCase.class)
  public void testEncodeDecodeVu57(Vu57TestCase testCase) throws IOException {
    long value = testCase.value;
    byte[] encoded = Vu57Utils.encodeVu57(value);
    assertArrayEquals(testCase.expectedEncoding, encoded);
    long decoded = Vu57Utils.decodeVu57(new ByteArrayInputStream(encoded));
    assertEquals(value, decoded);

    ByteArrayInputStream in = new ByteArrayInputStream(encoded);
    decoded = Vu57Utils.decodeVu57(in);
    assertEquals(value, decoded);
  }

  @Test
  public void testEncodeVu57NegativeValue() {
    assertThrows(IllegalArgumentException.class, () -> Vu57Utils.encodeVu57(-1L));
  }

  @Test
  public void testEncodeVu57ExceedsMaxValue() {
    long tooLarge = (1L << 57);
    assertThrows(IllegalArgumentException.class, () -> Vu57Utils.encodeVu57(tooLarge));
  }

  @Test
  public void testDecodeVu57UnexpectedEndOfStream() {
    byte[] incomplete = new byte[]{(byte) 0x80};

    assertThrows(IOException.class, () -> {
      ByteArrayInputStream in = new ByteArrayInputStream(incomplete);
      Vu57Utils.decodeVu57(in);
    });
  }

  @Test
  public void testVu57MultipleValuesInStream() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Vu57Utils.encodeVu57(100L, out);
    Vu57Utils.encodeVu57(200L, out);
    Vu57Utils.encodeVu57(300L, out);

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    assertEquals(100L, Vu57Utils.decodeVu57(in));
    assertEquals(200L, Vu57Utils.decodeVu57(in));
    assertEquals(300L, Vu57Utils.decodeVu57(in));
  }
}
