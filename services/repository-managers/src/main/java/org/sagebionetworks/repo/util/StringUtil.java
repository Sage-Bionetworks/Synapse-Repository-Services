package org.sagebionetworks.repo.util;

import java.util.Arrays;
import java.util.stream.Stream;

public class StringUtil {
	
	public static final int LEADING_CHARS = 3;
	public static final int TRAILING_CHARS = 1;
	public static final String OBFUSCATION_STRING = "...";
	public static final String EOL_PATTERN = "\\r?\\n|\\r";
	
	/**
	 * Obfuscate e-mail address by replacing the middle characters of the local
	 * part of the address with an ellipsis.
	 * 
	 * @param address
	 * @return
	 */
	public static String obfuscateEmailAddress(String address) {
		if (address == null)
			throw new IllegalArgumentException("e-mail address cannot be null");
		StringBuilder sb = new StringBuilder(address.length());
		if (address != null) {
			
			// Process local part of address
			int atDelim = address.lastIndexOf("@");
			if (atDelim < 3) {
				// Local part too short to obfuscate at all
				// Keep all characters
				return address;
			} else if (atDelim < LEADING_CHARS + TRAILING_CHARS) {
				// Local part too short to obfuscate as requested
				// Keep only first and last characters
				sb.append(address.charAt(0));
				sb.append(OBFUSCATION_STRING);
				sb.append(address.charAt(atDelim-1));
			} else {
				// Local part long enough to obfuscate as requested
				// Keep specified number of characters
				sb.append(address.substring(0, LEADING_CHARS));
				sb.append(OBFUSCATION_STRING);
				sb.append(address.substring(atDelim-TRAILING_CHARS, atDelim));
			}
			
			// Preserve domain part of address
			sb.append(address.substring(atDelim, address.length()));
		}		
		return sb.toString();
	}
	
	/**
	 * @param input
	 * @return A stream of lines extracted from this string, separated by line terminators. 
	 */
	public static Stream<String> lines(String input) {
		if (input == null) {
			return Stream.empty();
		}
		return Arrays.stream(input.split(EOL_PATTERN));
	}

}
