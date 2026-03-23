package org.sagebionetworks.repo.model.grid.encoding;

import org.sagebionetworks.util.ValidateArgument;

/**
 * Utility class for Base36 encoding and decoding of node keys.
 *
 * Node keys in the indexed model format are encoded as "&lt;sid&gt;_&lt;seq&gt;" where:
 * - sid is the session index (index in clock table) encoded as Base36
 * - seq is the sequence number encoded as Base36
 */
public class Base36Utils {

	private static final int RADIX = 36;

	/**
	 * Encode a long value to Base36 string.
	 *
	 * @param value the value to encode (must be non-negative)
	 * @return the Base36 encoded string
	 */
	public static String encodeBase36(long value) {
		if (value < 0) {
			throw new IllegalArgumentException("Value must be non-negative: " + value);
		}
		return Long.toString(value, RADIX);
	}

	/**
	 * Decode a Base36 string to a long value.
	 *
	 * @param encoded the Base36 encoded string
	 * @return the decoded value
	 */
	public static long decodeBase36(String encoded) {
		ValidateArgument.required(encoded, "encoded");
		if (encoded.isEmpty()) {
			throw new IllegalArgumentException("Encoded string cannot be empty");
		}

		return Long.parseLong(encoded, RADIX);
	}
}
