package org.sagebionetworks.repo.model.drs;

import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

/**
 * AccessId::= fileHandleAssociationType_synIdWithVersion_fileHandleId
 * <p>
 * This instance will parse the string in a single pass.
 */
public class AccessIdParser {

    // Null char is used to indicate parser termination.
    private static final int NULL_CHAR = 0x0;

    private static int MAX_LONG_DIGITS = 19;

    private int index;
    private char[] chars;
    private char currentChar;

    AccessIdParser(String toParse) {
        if (toParse == null) {
            throw new IllegalArgumentException("AccessId string cannot be null.");
        }
        index = 0;
        chars = toParse.trim().toCharArray();
        if (chars.length < 1) {
            throw new IllegalArgumentException("AccessId must contain at least one character.");
        }
        currentChar = chars[index];
    }

    /**
     * Parser the string in a single pass.
     *
     * @return
     */
    AccessId parse() {
        try {
            AccessIdBuilder builder = new AccessIdBuilder();
            // first parameter is the FileAssociationType
            builder.setAssociateType(consumeFileAssociationType());

            // Not at the end so the next char must be underScore
            consumeUnderscore();

            // Second parameter is the synapseIdWithVersion
            builder.setSynapseIdWithVersion(consumeSyn() + consumeSynapseId() + consumeDot() + consumeVersion());

            // Not at the end so the next char must be underScore
            consumeUnderscore();

            //  Third parameter is the FileHandleId
            builder.setFileHandleId(consumeFileHandleId());

            // Must be at the end
            if (!isEnd()) {
                throw new AccessIdParser.ParseException(index, "must be an end.");
            }
            return builder.build();
        } catch (AccessIdParser.ParseException e) {
            throw new IllegalArgumentException("Invalid Access ID: " + new String(chars), e);
        }
    }

    /**
     * Consume a single char from the character array
     *
     * @return The FileHandleAssociateType read from the array.
     * @throws AccessIdParser.ParseException
     */
    private FileHandleAssociateType consumeFileAssociationType() throws AccessIdParser.ParseException {
        final StringBuilder stringBuilder = new StringBuilder();
        return getAssociateType(stringBuilder);
    }

    private FileHandleAssociateType getAssociateType(StringBuilder stringBuilder) throws AccessIdParser.ParseException {
        try {
            while (Character.isLetter(currentChar)) {
                stringBuilder.append(currentChar);
                consumeCharacter();
            }

            return FileHandleAssociateType.valueOf(stringBuilder.toString());
        } catch (Exception exception) {
            throw new AccessIdParser.ParseException(index, exception.getMessage());
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
     * Consume the '_' character.
     *
     * @throws AccessIdParser.ParseException Thrown if the current character is not underscore.
     */
    private void consumeUnderscore() throws AccessIdParser.ParseException {
        if (currentChar == '_') {
            consumeCharacter();
        } else {
            throw new AccessIdParser.ParseException(index, "Expected character underscore is missing.");
        }
    }

    /**
     * Consume case insensitive 'syn' if present.
     *
     * @throws AccessIdParser.ParseException
     */
    private String consumeSyn() throws AccessIdParser.ParseException {
        final StringBuilder stringBuilder = new StringBuilder();

        if (currentChar == 's' || currentChar == 'S') {
            stringBuilder.append('s');
            consumeCharacter();
            if (currentChar == 'y' || currentChar == 'Y') {
                stringBuilder.append('y');
                consumeCharacter();
            } else {
                throw new AccessIdParser.ParseException(index, "AccessId must contains syn.");
            }
            if (currentChar == 'n' || currentChar == 'N') {
                stringBuilder.append('n');
                consumeCharacter();
            } else {
                throw new AccessIdParser.ParseException(index, "AccessId must contains syn.");
            }
        } else {
            throw new AccessIdParser.ParseException(index, "AccessId must contains syn.");
        }
        return stringBuilder.toString();
    }

    /**
     * Consume a single Long from the character array
     *
     * @return The Long read from the array.
     * @throws AccessIdParser.ParseException
     */
    private long consumeSynapseId() throws AccessIdParser.ParseException {
        return consumeLong("Not a valid synapse Id.");
    }

    private String consumeFileHandleId() throws AccessIdParser.ParseException {
        return consumeLong("Not a valid file handle Id.").toString();
    }

    private Long consumeLong(final String errorMessage) throws AccessIdParser.ParseException {
        int digits = 0;
        // consume all digits
        long value = 0;
        while (currentChar >= '0' && currentChar <= '9') {
            value *= 10L;
            value += ((long) currentChar - 48L);
            consumeCharacter();
            digits++;
            if (digits > MAX_LONG_DIGITS) {
                throw new AccessIdParser.ParseException(index, errorMessage);
            }
        }
        if (digits < 1) {
            throw new AccessIdParser.ParseException(index, errorMessage);
        }

        return value;
    }

    /**
     * Consume the 'dot' character.
     *
     * @throws AccessIdParser.ParseException Thrown if the current character is not dot.
     */
    private String consumeDot() throws AccessIdParser.ParseException {
        if (currentChar == '.') {
            consumeCharacter();
            return ".";
        } else {
            throw new AccessIdParser.ParseException(index, "dot was expected.");
        }
    }

    /**
     * Consume a single Long from the character array
     *
     * @return The Long read from the array.
     * @throws AccessIdParser.ParseException
     */
    private long consumeVersion() throws AccessIdParser.ParseException {
        return consumeLong("Not a valid synapse Version.");
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
     * Exception that indicates where the error occurred.
     */
    public static class ParseException extends Exception {

        private static final long serialVersionUID = 1L;

        int errorIndex;
        String message;

        public ParseException(int index, String message) {
            super("Unexpected character at index: " + index);
            this.errorIndex = index;
            this.message = message;
        }

        /**
         * The index of the error encountered.
         *
         * @return
         */
        public int getErrorIndex() {
            return errorIndex;
        }

        /**
         * The error message.
         *
         * @return
         */
        public String getMessage() {
            return message;
        }

    }

    /**
     * Parse the given String into an AccessId.
     *
     * @param toParse
     * @return
     */
    public static AccessId parseAccessId(String toParse) {
        AccessIdParser parser = new AccessIdParser(toParse);
        return parser.parse();
    }
}


