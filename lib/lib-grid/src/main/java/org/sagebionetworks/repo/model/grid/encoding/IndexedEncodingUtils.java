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

	private IndexedEncodingUtils() {
		// Utility class
	}

	/**
	 * Node type constants for the Indexed encoding format.
	 */
	public static final int NODE_TYPE_CONSTANT = 0b000;
	public static final int NODE_TYPE_OBJECT = 0b010;
	public static final int NODE_TYPE_VECTOR = 0b011;
	public static final int NODE_TYPE_ARRAY = 0b110;

	/**
	 * Write the node type and length header byte(s).
	 *
	 * The first byte encodes the node type in bits 7-5 (3 bits) and the length in bits 4-0 (5 bits).
	 * When the length is 31 or greater, bits 4-0 are set to 0x1F and the actual length is encoded
	 * as a vu57 integer in the following bytes.
	 *
	 * @param nodeType the node type (3 bits, 0-7)
	 * @param length the length value
	 * @param out the output stream
	 * @return the number of bytes written
	 * @throws IOException if an I/O error occurs
	 */
	public static int writeNodeTypeAndLength(int nodeType, long length, OutputStream out) throws IOException {
		ValidateArgument.required(out, "out");

		int bytesWritten = 0;
		// Type occupies bits 7-5 (3 bits), length occupies bits 4-0 (5 bits)
		nodeType = (byte) (nodeType << 5);
		if (length < 31) {
			// When length e is less than 31, the first 3 bits of TL encode the node type c and the remaining 5 bits
			// encode the length e.
			nodeType |= (int) length;
			out.write(nodeType);
			bytesWritten += 1;
		} else {
			// When length is 31 or greater, the first byte encodes the node type c, and the remaining bits are set to 1.
			// The length is encoded as a vu57 integer.
			nodeType |= 0b0001_1111; // length extension indicator
			out.write(nodeType);
			bytesWritten += 1;
			byte[] encodedLength = Vu57Utils.encodeVu57(length);
			out.write(encodedLength);
			bytesWritten += encodedLength.length;
		}
		return bytesWritten;
	}

	/**
	 * Read the node type and length header from the input stream.
	 *
	 * @param in the input stream
	 * @return a TypeAndLength containing the node type and length
	 * @throws IOException if an I/O error occurs
	 */
	public static IndexedNodeHeader readNodeTypeAndLength(InputStream in) throws IOException {
		ValidateArgument.required(in, "in");

		int firstByte = in.read();
		if (firstByte == -1) {
			throw new IOException("Unexpected end of stream while reading node type and length");
		}

		// Type occupies bits 7-5 (3 bits), length occupies bits 4-0 (5 bits)
		int nodeType = (firstByte >> 5) & 0x07;

		// Extract length from lower 5 bits
		int lengthPart = firstByte & 0x1F;

		long length;
		if (lengthPart < 31) {
			// Length is directly encoded in the lower 5 bits
			length = lengthPart;
		} else {
			// Length extension: read vu57 for actual length
			length = Vu57Utils.decodeVu57(in);
		}

		return new IndexedNodeHeader(nodeType, length);
	}
}
