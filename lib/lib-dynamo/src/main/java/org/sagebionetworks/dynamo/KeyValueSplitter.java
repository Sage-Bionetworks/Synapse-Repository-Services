package org.sagebionetworks.dynamo;

import java.util.regex.Pattern;

/**
 * Splits parts of a hash key or rang key so that we can build composite keys.
 */
public class KeyValueSplitter {

	public static final String SEPARATOR = "#";
	public static final Pattern SEPARATOR_PATTERN = Pattern.compile(SEPARATOR);

	/**
	 * Splits a string by the defined separator. Null or empty string will
	 * get back an empty array.
	 */
	public static final String[] split(String string) {

		if (string == null) {
			return EMPTY_ARRAY;
		}

		if (string.length() == 0) {
			// Pattern.split("") will return an array consists of one empty string
			// We change it to return an empty array
			return EMPTY_ARRAY;
		}

		return SEPARATOR_PATTERN.split(string);
	}

	public static String createKey(Object... parts) {
		StringBuilder sb = new StringBuilder();
		for (Object part : parts) {
			if (part == null) {
				throw new IllegalArgumentException("All parts of the key are required");
			}
			if (sb.length() > 0) {
				sb.append(SEPARATOR);
			}
			sb.append(part.toString());
		}
		return sb.toString();
	}

	private static final String[] EMPTY_ARRAY = new String[]{};
}
