package org.sagebionetworks.repo.model.grid.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.util.ValidateArgument;

/**
 * Utility methods for Indexed encoding format.
 * Contains shared encoding logic used by individual node encoders.
 */
public final class IndexedEncodingUtils {

	// The minimum value of length that requires extended encoding. Lengths less than this can be encoded directly in the first byte.
	// NOTE: The JSON CRDT specification claims this is 31, but in the json-joy implementation, it is actually 24.
	// See https://github.com/streamich/json-joy/issues/986
	private final static int INLINE_LENGTH_THRESHOLD = 24;

	private IndexedEncodingUtils() {
		// Utility class
	}

	/**
	 * Write the node type and length header byte(s).
	 *
	 * The first byte encodes the node type in bits 7-5 (3 bits) and the length in bits 4-0 (5 bits).
	 *
	 * NOTE: The JSON CRDT spec says lengths 0-30 are inline and 31 triggers vu57 extension.
	 * However, the json-joy library implementation uses CBOR-style encoding:
	 *   - 0-23: inline in the lower 5 bits
	 *   - 24: 1-byte unsigned length follows
	 *   - 25: 2-byte big-endian unsigned length follows
	 *   - 26: 4-byte big-endian unsigned length follows
	 * We match the library's actual behavior for interoperability.
	 *
	 * @param nodeType the node type (3 bits, 0-7)
	 * @param length the length value
	 * @param out the output stream
	 * @return the number of bytes written
	 * @throws IOException if an I/O error occurs
	 */
	public static int writeNodeHeader(int nodeType, long length, OutputStream out) throws IOException {
		ValidateArgument.required(out, "out");

		int majorOverlay = (nodeType & 0x07) << 5;

		if (length < INLINE_LENGTH_THRESHOLD) {
			// Length fits directly in the lower 5 bits
			out.write(majorOverlay | (int) length);
			return 1;
		} else if (length <= 0xFF) {
			// minor = 24: 1-byte unsigned length follows
			out.write(majorOverlay | 24);
			out.write((int) length);
			return 2;
		} else if (length <= 0xFFFF) {
			// minor = 25: 2-byte big-endian unsigned length follows
			out.write(majorOverlay | 25);
			out.write((int) (length >> 8) & 0xFF);
			out.write((int) length & 0xFF);
			return 3;
		} else {
			// minor = 26: 4-byte big-endian unsigned length follows
			out.write(majorOverlay | 26);
			out.write((int) (length >> 24) & 0xFF);
			out.write((int) (length >> 16) & 0xFF);
			out.write((int) (length >> 8) & 0xFF);
			out.write((int) length & 0xFF);
			return 5;
		}
	}

	/**
	 * Read the node type and length header from the input stream.
	 *
	 * Uses CBOR-style length decoding to match json-joy's actual behavior.
	 * See {@link #writeNodeHeader} for details on the encoding.
	 *
	 * @param in the input stream
	 * @return a TypeAndLength containing the node type and length
	 * @throws IOException if an I/O error occurs
	 */
	public static IndexedNodeHeader readNodeHeader(InputStream in) throws IOException {
		ValidateArgument.required(in, "in");

		int firstByte = in.read();
		if (firstByte == -1) {
			throw new IOException("Unexpected end of stream while reading node type and length");
		}

		int nodeType = (firstByte >> 5) & 0x07;
		int minor = firstByte & 0x1F;

		long length;
		if (minor < INLINE_LENGTH_THRESHOLD) {
			length = minor;
		} else if (minor == 24) {
			// 1-byte unsigned length
			int b = in.read();
			if (b == -1) {
				throw new IOException("Unexpected end of stream while reading 1-byte length");
			}
			length = b;
		} else if (minor == 25) {
			// 2-byte big-endian unsigned length
			int b1 = in.read();
			int b2 = in.read();
			if (b1 == -1 || b2 == -1) {
				throw new IOException("Unexpected end of stream while reading 2-byte length");
			}
			length = (b1 << 8) | b2;
		} else if (minor == 26) {
			// 4-byte big-endian unsigned length
			int b1 = in.read();
			int b2 = in.read();
			int b3 = in.read();
			int b4 = in.read();
			if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1) {
				throw new IOException("Unexpected end of stream while reading 4-byte length");
			}
			length = ((long) b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
		} else {
			throw new IOException("Unsupported minor value in node header: " + minor);
		}

		return new IndexedNodeHeader(nodeType, length);
	}
}
