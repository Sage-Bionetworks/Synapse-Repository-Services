package org.sagebionetworks.repo.model.entity;

/**
 * EntityId::= [syn]<id>[.<version>]
 *
 * This parser will parse the given string with a single pass (O(n)).
 */
public class EntityIdParser {

	// Null char is used to indicate parser termination.
	private static final int NULL_CHAR = 0x0;

	private int index;
	private char[] chars;
	private char currentChar;
	EntityIdBuilder builder;

	EntityIdParser(String toParse) {
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
	 * Parser the string in a single pass.
	 * 
	 * @return
	 */
	EntityId parse() {
		try {
			EntityIdBuilder builder = new EntityIdBuilder();
			// ignore starting white space
			consumeWhiteSpace();
			// skip 'syn' if present.
			consumeSyn();
			// first long is the ID
			builder.setId(consumeLong());
			// version is optional so might be at the end.
			if (!isEnd()) {
				// Not at the end so the next char must be dot
				consumeDot();
				// second long is the version
				builder.setVersion(consumeLong());
				// ignore trailing whitespace.
				consumeWhiteSpace();
			}
			// Must be at the end
			if (!isEnd()) {
				throw new ParseException(index);
			}
			return builder.build();
		} catch (ParseException e) {
			throw new IllegalArgumentException("Invalid Entity ID: " + new String(chars), e);
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
	 * Consume the 'dot' character.
	 * 
	 * @throws ParseException Thrown if the current character is not dot.
	 */
	private void consumeDot() throws ParseException {
		if (currentChar == '.') {
			consumeCharacter();
		} else {
			throw new ParseException(index);
		}
	}

	/**
	 * Consume a single Long from the character array
	 * 
	 * @return The Long read from the array.
	 */
	private Long consumeLong() {
		int start = index;
		// consume all digits
		while (currentChar >= '0' && currentChar <= '9') {
			consumeCharacter();
		}
		return Long.parseLong(new String(chars, start, index - start));
	}

	/**
	 * Consume case insensitive 'syn' if present.
	 * 
	 * @throws ParseExcpetion
	 */
	private void consumeSyn() throws ParseException {
		if (currentChar == 's' || currentChar == 'S') {
			consumeCharacter();
			if (currentChar == 'y' || currentChar == 'Y') {
				consumeCharacter();
			} else {
				throw new ParseException(index);
			}
			if (currentChar == 'n' || currentChar == 'N') {
				consumeCharacter();
			} else {
				throw new ParseException(index);
			}
		}
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

		private static final long serialVersionUID = 1L;

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
	 * Parse the given String into an EntityId.
	 * 
	 * @param toParse
	 * @return
	 */
	public static EntityId parseEntityId(String toParse) {
		EntityIdParser parser = new EntityIdParser(toParse);
		return parser.parse();
	}
}
