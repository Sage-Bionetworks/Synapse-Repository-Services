package org.sagebionetworks.repo.model.grid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.grid.encoding.B1Vu56Utils;

public class B1Vu56UtilsTest {

  public enum B1Vu56TestCase {
    FALSE(false, 0L, new byte[]{0x00}),
    TRUE(true, 0L, new byte[]{(byte) 0x80}),
    SMALL_VALUE_FALSE(false, 1L, new byte[]{0x01}),
    SMALL_VALUE_TRUE(true, 1L, new byte[]{(byte) 0x81}),
    MAX_SINGLE_BYTE_FALSE(false, 63L, new byte[]{0x3F}),
    MAX_SINGLE_BYTE_TRUE(true, 63L, new byte[]{(byte) 0xBF}),
    TWO_BYTES_FALSE(false, 64L, new byte[]{0x40, 0x01}),
    TWO_BYTES_TRUE(true, 64L, new byte[]{(byte) 0xC0, 0x01}),
    MAX_TWO_BYTES_FALSE(false, 8191L, new byte[]{0x7F, 0x7F}),
    MAX_TWO_BYTES_TRUE(true, 8191L, new byte[]{(byte) 0xFF, 0x7F}),
    THREE_BYTES(false, 8192L, new byte[]{0x40, (byte) 0x80, 0x01}),
    MAX_56_BITS_FALSE(false, (1L << 56) - 1, new byte[]{
            0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    }),
    MAX_56_BITS_TRUE(true, (1L << 56) - 1, new byte[]{
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    });

    final boolean flag;
    final long value;
    final byte[] expectedEncoding;

    B1Vu56TestCase(boolean flag, long value, byte[] expectedEncoding) {
      this.flag = flag;
      this.value = value;
      this.expectedEncoding = expectedEncoding;
    }
  }

  @ParameterizedTest
  @EnumSource(B1Vu56TestCase.class)
  public void testEncodeDecodeB1Vu56(B1Vu56TestCase testCase) throws IOException {
    boolean flag = testCase.flag;
    long value = testCase.value;
    byte[] encoded = B1Vu56Utils.encodeB1Vu56(flag, value);
    assertArrayEquals(testCase.expectedEncoding, encoded);
    B1Vu56Utils.B1Vu56Result decoded = B1Vu56Utils.decodeB1Vu56(new ByteArrayInputStream(encoded));
    assertEquals(flag, decoded.getFlag());
    assertEquals(value, decoded.getValue());

    ByteArrayInputStream in = new ByteArrayInputStream(encoded);
    decoded = B1Vu56Utils.decodeB1Vu56(in);
    assertEquals(flag, decoded.getFlag());
    assertEquals(value, decoded.getValue());
  }

  @Test
  public void testEncodeB1Vu56NegativeValue() {
    assertThrows(IllegalArgumentException.class, () -> B1Vu56Utils.encodeB1Vu56(false, -1L));
  }

  @Test
  public void testEncodeB1Vu56ExceedsMaxValue() {
    long tooLarge = (1L << 56);
    assertThrows(IllegalArgumentException.class, () -> B1Vu56Utils.encodeB1Vu56(false, tooLarge));
  }

  @Test
  public void testB1Vu56RoundTrip() throws IOException {
    long[] testValues = {
      0L,
      1L,
      63L,
      64L,
      127L,
      8191L,
      8192L,
      1_000_000L,
      1_000_000_000L,
      1_000_000_000_000L,
      (1L << 55) - 1,
      (1L << 56) - 1
    };

    for (long value : testValues) {
      for (boolean flag : new boolean[]{false, true}) {
        byte[] encoded = B1Vu56Utils.encodeB1Vu56(flag, value);
        B1Vu56Utils.B1Vu56Result result = B1Vu56Utils.decodeB1Vu56(new ByteArrayInputStream(encoded));
        assertEquals(flag, result.getFlag(), "Round trip failed for flag: " + flag + ", value: " + value);
        assertEquals(value, result.getValue(), "Round trip failed for flag: " + flag + ", value: " + value);

        ByteArrayInputStream in = new ByteArrayInputStream(encoded);
        result = B1Vu56Utils.decodeB1Vu56(in);
        assertEquals(flag, result.getFlag(), "Round trip with InputStream failed for flag: " + flag + ", value: " + value);
        assertEquals(value, result.getValue(), "Round trip with InputStream failed for flag: " + flag + ", value: " + value);
      }
    }
  }

  @Test
  public void testDecodeB1Vu56UnexpectedEndOfStream() {
    byte[] incomplete = new byte[]{0x40};

    assertThrows(IOException.class, () -> {
      ByteArrayInputStream in = new ByteArrayInputStream(incomplete);
      B1Vu56Utils.decodeB1Vu56(in);
    });
  }

  @Test
  public void testB1Vu56MultipleValuesInStream() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    B1Vu56Utils.encodeB1Vu56(true, 100L, out);
    B1Vu56Utils.encodeB1Vu56(false, 200L, out);
    B1Vu56Utils.encodeB1Vu56(true, 300L, out);

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    B1Vu56Utils.B1Vu56Result result1 = B1Vu56Utils.decodeB1Vu56(in);
    assertTrue(result1.getFlag());
    assertEquals(100L, result1.getValue());

    B1Vu56Utils.B1Vu56Result result2 = B1Vu56Utils.decodeB1Vu56(in);
    assertFalse(result2.getFlag());
    assertEquals(200L, result2.getValue());

    B1Vu56Utils.B1Vu56Result result3 = B1Vu56Utils.decodeB1Vu56(in);
    assertTrue(result3.getFlag());
    assertEquals(300L, result3.getValue());
  }
}
