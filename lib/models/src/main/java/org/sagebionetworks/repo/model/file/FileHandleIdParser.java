package org.sagebionetworks.repo.model.file;

/**
 * FileHandleId::= [fh]<id>
 *
 * This instance will parse the string in a single pass.
 */
public class FileHandleIdParser {

	// Null char is used to indicate parser termination.
	private static final int NULL_CHAR = 0x0;
	private static int MAX_LONG_DIGITS = 19;
	private int index;
	private char[] chars;
	private char currentChar;

	FileHandleIdParser(String toParse) {
		if (toParse == null) {
			throw new IllegalArgumentException("Id string cannot be null");
		}
		index = 0;
		chars = toParse.toCharArray();
		if (chars.length < 1) {
			throw new IllegalArgumentException("Id must contain at least one character.");
		}
		currentChar = chars[index];
	}

	/**
	 * Parse the string in a single pass.
	 * 
	 * @return
	 */
	String parse() {
		try {
			// ignore starting white space
			consumeWhiteSpace();
			// consume 'fh'.
			consumeFh();
			// first long is the ID
			long id = consumeLong();
			String fileHandleId = Long.toString(id);
			if (!isEnd()) {
				// ignore trailing whitespace.
				consumeWhiteSpace();
			}
			// Must be at the end
			if (!isEnd()) {
				throw new ParseException(index);
			}
			return fileHandleId;
		} catch (ParseException e) {
			throw new IllegalArgumentException("Invalid File Handle ID: " + new String(chars), e);
		}
	}

	/**
	 * Consume the current character and fetch the next.
	 */
	private void consumeCharacter() {
		index++;
		if (index < chars.length) {
			currentChar = chars[index];
		} else {
			// set to null
			currentChar = NULL_CHAR;
		}
	}

	/**
	 * Parser is at the end if the current character is the null character.
	 * 
	 * @return
	 */
	private boolean isEnd() {
		return currentChar == NULL_CHAR;
	}

	/**
	 * Consume a single Long from the character array
	 * 
	 * @return The Long read from the array.
	 * @throws ParseException 
	 */
	private long consumeLong() throws ParseException {
		int digits = 0;
		// consume all digits
		long value = 0;
		while (currentChar >= '0' && currentChar <= '9') {
			value *= 10L;
			value += ((long)currentChar - 48L);
			consumeCharacter();
			digits++;
			if(digits > MAX_LONG_DIGITS) {
				throw new ParseException(index);
			}
		}
		if(digits < 1) {
			throw new ParseException(index);
		}
		return value;
	}

	/**
	 * Consume case insensitive 'fh' if present.
	 *
	 */
	private void consumeFh() throws ParseException {
		if (currentChar == 'f' || currentChar == 'F') {
			consumeCharacter();
			if (currentChar == 'h' || currentChar == 'H') {
				consumeCharacter();
			} else {
				throw new ParseException(index);
			}
		} else {
			throw new ParseException(index);
		}
	}

	/**
	 * Checks if case insensitive 'fh' if present.
	 *
	 */
	private boolean checkForFh() {
		if (currentChar == 'f' || currentChar == 'F') {
			consumeCharacter();
			if (currentChar == 'h' || currentChar == 'H') {
				consumeCharacter();
				return true;
			}
		}
		return false;
	}

	/**
	 * Skip over all whitespace.
	 */
	private void consumeWhiteSpace() {
		while (Character.isWhitespace(currentChar)) {
			consumeCharacter();
		}
	}

	/**
	 * Exception that indicates where the error occurred.
	 *
	 */
	public static class ParseException extends Exception {
		int errorIndex;

		public ParseException(int index) {
			super("Unexpected character at index: " + index);
			this.errorIndex = index;
		}

		/**
		 * The index of the error encountered.
		 * 
		 * @return
		 */
		public int getErrorIndex() {
			return errorIndex;
		}
	}

	/**
	 * Parse the given String into a file handle ID.
	 * 
	 * @param toParse
	 * @return
	 */
	public static String parseFileHandleId(String toParse) {
		FileHandleIdParser parser = new FileHandleIdParser(toParse);
		return parser.parse();
	}

	/**
	 * Check that the given String starts with the case-insensitive prefix 'fh'.
	 *
	 * @param toParse
	 * @return
	 */
	public static boolean startsWithFh(String toParse) {
		FileHandleIdParser parser = new FileHandleIdParser(toParse);
		parser.consumeWhiteSpace();
		return parser.checkForFh();
	}
}
