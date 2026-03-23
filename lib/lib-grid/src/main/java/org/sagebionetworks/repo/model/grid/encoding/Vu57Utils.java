package org.sagebionetworks.repo.model.grid.encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Utility class for encoding and decoding vu57 (Variable Length Unsigned 57-bit Integer).
 * <p>
 * vu57 encoding uses 1-8 bytes where:
 * - Each byte has 7 bits of data and 1 continuation bit (high bit)
 * - Continuation bit = 1 means another byte follows
 * - Continuation bit = 0 means this is the last byte
 * - Maximum value: 2^57 - 1
 * <p>
 * Adheres to the vu57 encoding defined in the <a href="https://jsonjoy.com/specs/json-crdt/encoding/structural-encoding/binary-structural-format#vu57-Encoding">JSON CRDT Binary Encoding specification</a>.
 */
public class Vu57Utils {
    public static final long MAX_VU57_VALUE = (1L << 57) - 1; // 2^57 - 1

    private static void validateVu57Value(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative: " + value);
        }
        if (value > MAX_VU57_VALUE) {
            throw new IllegalArgumentException("Value exceeds 57 bits: " + value);
        }
    }

    /**
     * Encodes a long value as vu57 and writes it to the output stream.
     *
     * @param value the unsigned 57-bit integer to encode (must be in range [0, 2^57-1])
     * @param out   the output stream to write to
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if value is negative or exceeds 57 bits
     */
    public static void encodeVu57(long value, OutputStream out) throws IOException {
        validateVu57Value(value);

        // Encode with continuation bits
        // Process 7 bits at a time from least significant to most significant
        int byteCount = 0;
        while (value > 0x7F && byteCount < 7) {
            // More bytes follow: set continuation bit
            out.write((int) (value & EncodingConstants.DATA_MASK) | EncodingConstants.CONTINUATION_BIT);
            value >>>= 7; // Unsigned right shift
            byteCount++;
        }

        // Last byte: no continuation bit
        // If we've written 7 bytes, the 8th byte can use all 8 bits (mask to ensure it's within 0xFF)
        // Otherwise, the last byte will be 0-127 (fits in 7 bits, but we write it without continuation bit)
        out.write((int) (value & 0xFF));
    }

    /**
     * Encodes a long value as vu57 and returns it as a byte array.
     *
     * @param value the unsigned 57-bit integer to encode (must be in range [0, 2^57-1])
     * @return the encoded byte array
     * @throws IllegalArgumentException if value is negative or exceeds 57 bits
     */
    public static byte[] encodeVu57(long value) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(8);) {
            encodeVu57(value, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException encountered while encoding integer as vu57", e);
        }
    }

    /**
     * Decodes a vu57 encoded value from the input stream.
     *
     * @param in the input stream to read from
     * @return the decoded unsigned 57-bit integer
     * @throws IOException              if an I/O error occurs or end of stream is reached unexpectedly
     * @throws IllegalArgumentException if the encoded value exceeds 57 bits
     */
    public static long decodeVu57(InputStream in) throws IOException {
        long result = 0;
        int shift = 0;
        int byteCount = 0;

        while (true) {
            int b = in.read();
            if (b == -1) {
                throw new IOException("Unexpected end of stream while decoding vu57");
            }

            byteCount++;

            // For the 8th byte, use all 8 bits (it never has a continuation bit)
            if (byteCount == 8) {
                // 8th byte: use all 8 bits, no continuation bit
                long dataBits = b & 0xFF;
                result |= (dataBits << shift);
                break; // 8th byte is always the last byte
            }

            // Bytes 1-7: check continuation bit and extract 7 data bits
            boolean hasContinuation = (b & EncodingConstants.CONTINUATION_BIT) != 0;
            long dataBits = b & EncodingConstants.DATA_MASK;

            // Add the data bits to the result
            result |= (dataBits << shift);

            if (!hasContinuation) {
                // This was the last byte
                break;
            }

            shift += 7;
        }

        return result;
    }
}
