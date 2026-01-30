package org.sagebionetworks.repo.model.grid.encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for encoding and decoding b1vu56 (Boolean and Variable-Length Unsigned 56-bit Integer).
 * <p>
 * b1vu56 is a single boolean bit flag, followed by a variable-length unsigned 56-bit integer. Each b1vu56 value is
 * encoded as a variable number—from 1 to 8—of bytes.
 * <p>
 * Adheres to the b1vu56 encoding defined in the <a href="https://jsonjoy.com/specs/json-crdt/encoding/structural-encoding/binary-structural-format#b1vu56-Encoding">JSON CRDT Binary Encoding specification</a>.
 */
public class B1Vu56Utils {
    private static final int B1VU56_FLAG_BIT = 0x80;              // 10000000 - flag bit in first byte
    private static final int B1VU56_FIRST_CONTINUATION = 0x40;   // 01000000 - continuation bit in first byte
    private static final int B1VU56_FIRST_DATA_MASK = 0x3F;      // 00111111 - 6 data bits in first byte
    private static final long MAX_B1VU56_VALUE = (1L << 56) - 1; // 2^56 - 1

    /**
     * Result of decoding a b1vu56 value.
     */
    public static class B1Vu56Result {
        private final boolean flag;
        private final long value;

        public B1Vu56Result(boolean flag, long value) {
            this.flag = flag;
            this.value = value;
        }

        public boolean getFlag() {
            return flag;
        }

        public long getValue() {
            return value;
        }
    }

    private static void validateB1Vu56Value(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative: " + value);
        }
        if (value > MAX_B1VU56_VALUE) {
            throw new IllegalArgumentException("Value exceeds 56 bits: " + value);
        }
    }

    /**
     * Encodes a boolean flag and a long value as b1vu56 and writes it to the output stream.
     * <p>
     * b1vu56 encoding:
     * - Byte 1: flag bit (bit 7), continuation bit (bit 6), 6 data bits (bits 0-5)
     * - Bytes 2-7: continuation bit (bit 7), 7 data bits (bits 0-6)
     * - Byte 8: 8 data bits (no continuation bit)
     *
     * @param flag  the boolean flag to encode
     * @param value the unsigned 56-bit integer to encode (must be in range [0, 2^56-1])
     * @param out   the output stream to write to
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if value is negative or exceeds 56 bits
     */
    public static void encodeB1Vu56(boolean flag, long value, OutputStream out) throws IOException {
        validateB1Vu56Value(value);

        // First byte: flag | continuation | 6 data bits
        int firstByte = flag ? B1VU56_FLAG_BIT : 0;
        int dataBits = (int) (value & B1VU56_FIRST_DATA_MASK);
        value >>>= 6;

        if (value == 0) {
            // Single byte - no continuation needed
            firstByte |= dataBits;
            out.write(firstByte);
            return;
        }

        // More bytes follow - set continuation bit
        firstByte |= B1VU56_FIRST_CONTINUATION | dataBits;
        out.write(firstByte);

        // Encode remaining bits using 7-bit chunks (bytes 2-7)
        int byteCount = 1;
        while (value > 0x7F && byteCount < 7) {
            out.write((int) (value & EncodingConstants.DATA_MASK) | EncodingConstants.CONTINUATION_BIT);
            value >>>= 7;
            byteCount++;
        }

        // Last byte: no continuation bit
        out.write((int) (value & 0xFF));
    }

    /**
     * Encodes a boolean flag and a long value as b1vu56 and returns it as a byte array.
     *
     * @param flag  the boolean flag to encode
     * @param value the unsigned 56-bit integer to encode (must be in range [0, 2^56-1])
     * @return the encoded byte array
     * @throws IllegalArgumentException if value is negative or exceeds 56 bits
     */
    public static byte[] encodeB1Vu56(boolean flag, long value) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(8)) {
            encodeB1Vu56(flag, value, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException", e);
        }
    }

    /**
     * Decodes a b1vu56 encoded value from the input stream.
     *
     * @param in the input stream to read from
     * @return the decoded B1Vu56Result containing the flag and value
     * @throws IOException              if an I/O error occurs or end of stream is reached unexpectedly
     * @throws IllegalArgumentException if the encoded value exceeds 56 bits
     */
    public static B1Vu56Result decodeB1Vu56(InputStream in) throws IOException {
        int firstByte = in.read();
        if (firstByte == -1) {
            throw new IOException("Unexpected end of stream while decoding b1vu56");
        }

        boolean flag = (firstByte & B1VU56_FLAG_BIT) != 0;
        boolean hasContinuation = (firstByte & B1VU56_FIRST_CONTINUATION) != 0;
        long result = firstByte & B1VU56_FIRST_DATA_MASK;

        if (!hasContinuation) {
            return new B1Vu56Result(flag, result);
        }

        int shift = 6;
        int byteCount = 1;

        while (true) {
            int b = in.read();
            if (b == -1) {
                throw new IOException("Unexpected end of stream while decoding b1vu56");
            }

            byteCount++;

            // For the 8th byte, use all 8 bits (it never has a continuation bit)
            if (byteCount == 8) {
                long dataBits = b & 0xFF;
                result |= (dataBits << shift);
                break;
            }

            // Bytes 2-7: check continuation bit and extract 7 data bits
            boolean nextHasContinuation = (b & EncodingConstants.CONTINUATION_BIT) != 0;
            long dataBits = b & EncodingConstants.DATA_MASK;
            result |= (dataBits << shift);

            if (!nextHasContinuation) {
                break;
            }

            shift += 7;
        }

        return new B1Vu56Result(flag, result);
    }
}
