package au.com.bytecode.opencsv;

/**
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A very simple CSV parser released under a commercial-friendly license.
 * This just implements splitting a single line into fields.
 *
 * @author Glen Smith
 * @author Rainer Pruy
 */
public class CSVParser {

    private final char separator;

    private final char quotechar;

    private final char escape;

    private final boolean strictQuotes;

    private String pending;

    private final boolean ignoreLeadingWhiteSpace;

    public static final int INITIAL_READ_SIZE = 128;

    /**
     * The default strict quote behavior to use if none is supplied to the
     * constructor
     */
    public static final boolean DEFAULT_STRICT_QUOTES = false;

    /**
     * The default leading whitespace behavior to use if none is supplied to the
     * constructor
     */
    public static final boolean DEFAULT_IGNORE_LEADING_WHITESPACE = true;

    /**
     * Constructs CSVParser using a comma for the separator.
     */
    public CSVParser() {
        this(Constants.DEFAULT_SEPARATOR, Constants.DEFAULT_QUOTE_CHARACTER, Constants.DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVParser with supplied separator.
     *
     * @param separator the delimiter to use for separating entries.
     */
    public CSVParser(char separator) {
        this(separator, Constants.DEFAULT_QUOTE_CHARACTER, Constants.DEFAULT_ESCAPE_CHARACTER);
    }


    /**
     * Constructs CSVParser with supplied separator and quote char.
     *
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     */
    public CSVParser(char separator, char quotechar) {
        this(separator, quotechar, Constants.DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVReader with supplied separator and quote char.
     *
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     * @param escape    the character to use for escaping a separator or quote
     */
    public CSVParser(char separator, char quotechar, char escape) {
        this(separator, quotechar, escape, DEFAULT_STRICT_QUOTES);
    }

    /**
     * Constructs CSVReader with supplied separator and quote char.
     * Allows setting the "strict quotes" flag
     *
     * @param separator    the delimiter to use for separating entries
     * @param quotechar    the character to use for quoted elements
     * @param escape       the character to use for escaping a separator or quote
     * @param strictQuotes if true, characters outside the quotes are ignored
     */
    public CSVParser(char separator, char quotechar, char escape, boolean strictQuotes) {
        this(separator, quotechar, escape, strictQuotes, DEFAULT_IGNORE_LEADING_WHITESPACE);
    }

    /**
     * Constructs CSVReader with supplied separator and quote char.
     * Allows setting the "strict quotes" and "ignore leading whitespace" flags
     *
     * @param separator               the delimiter to use for separating entries
     * @param quotechar               the character to use for quoted elements
     * @param escape                  the character to use for escaping a separator or quote
     * @param strictQuotes            if true, characters outside the quotes are ignored
     * @param ignoreLeadingWhiteSpace if true, white space in front of a quote in a field is ignored
     */
    public CSVParser(char separator, char quotechar, char escape, boolean strictQuotes, boolean ignoreLeadingWhiteSpace) {
        if (anyCharactersAreTheSame(separator, quotechar, escape)) {
            throw new UnsupportedOperationException("The separator, quote, and escape characters must be different!");
        }
        if (separator == Constants.NULL_CHARACTER) {
            throw new UnsupportedOperationException("The separator character must be defined!");
        }
        this.separator = separator;
        this.quotechar = quotechar;
        this.escape = escape;
        this.strictQuotes = strictQuotes;
        this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
    }

    private boolean anyCharactersAreTheSame(char separator, char quotechar, char escape) {
        return isSameCharacter(separator, quotechar) || isSameCharacter(separator, escape) || isSameCharacter(quotechar, escape);
    }

    private boolean isSameCharacter(char c1, char c2) {
        return c1 != Constants.NULL_CHARACTER && c1 == c2;
    }

    /**
     * @return true if something was left over from last call(s)
     */
    public boolean isPending() {
        return pending != null;
    }

    public String[] parseLineMulti(String nextLine) throws IOException {
        return parseLine(nextLine, true);
    }

    public String[] parseLine(String nextLine) throws IOException {
        return parseLine(nextLine, false);
    }

    /**
     * Parses an incoming String and returns an array of elements.
     *
     * @param nextLine the string to parse
     * @param multi
     * @return the comma-tokenized list of elements, or null if nextLine is null
     * @throws IOException if bad things happen during the read
     */
    private String[] parseLine(String nextLine, boolean multi) throws IOException {
    	
    	boolean inField = false; // It is in a column field without quote or with quote
    	
        if (!multi && pending != null) {
            pending = null;
        }

        if (nextLine == null) {
            if (pending != null) {
                String s = pending;
                pending = null;
                return new String[]{s};
            } else {
                return null;
            }
        }

        List<String> tokensOnThisLine = new ArrayList<String>();
        StringBuilder sb = new StringBuilder(INITIAL_READ_SIZE);
        boolean inQuotes = false;
        boolean isNull = true;
        if (pending != null) {
            sb.append(pending);
            pending = null;
            inQuotes = true;
            isNull = false;
        }
        /*
         * If a line starts with the UTF-8 BOM, the marker must be completely ignored.
         */
        if(nextLine.startsWith(Constants.UTF_8_BYTE_ORDER_MARKER)) {
        	nextLine = nextLine.substring(1, nextLine.length());
        }

        for (int i = 0; i < nextLine.length(); i++) {

            char c = nextLine.charAt(i);
            
            if ( this.escape != Constants.NO_ESCAPE_CHARACTER && c == this.escape) {
                if (isNextCharacterEscapable(nextLine, inQuotes || inField, i)) {
                    sb.append(nextLine.charAt(i + 1));
                    isNull = false;
                    i++;
                }
            } else if (c == quotechar) {
                if (isNextCharacterEscapedQuote(nextLine, inQuotes, i)) {
                    sb.append(nextLine.charAt(i + 1));
                    isNull = false;
                    i++;
                } else {
                    // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                    if (!strictQuotes) {
                        if (i > 0 //not on the beginning of the line
                                && nextLine.charAt(i - 1) != this.separator //not at the beginning of an escape sequence
                                && !isNextCharacterCloseQuote(nextLine, inQuotes || inField, i) //not at the end of an escape sequence
                                ) {

                            if (ignoreLeadingWhiteSpace && sb.length() > 0 && isAllWhiteSpace(sb)) {
                                sb.setLength(0);  //discard white space leading up to quote
                            } else {
                                sb.append(c);
                                isNull = false;
                                continue;
                            }
                        }
                    }
                    inQuotes = !inQuotes;
                    inField = !inField;

                    if (inQuotes || inField) {
                    	isNull = false;
                    }
                }
            } else if (c == separator && !inQuotes) {
            	if (isNull) {
            		tokensOnThisLine.add(null);
            	} else {
            		tokensOnThisLine.add(sb.toString());
            	}
                sb.setLength(0); // start work on next token
                inField = false;
                isNull = true;
            } else {
                if (!strictQuotes || inQuotes) {
                    sb.append(c);
                    inField = true;
                    isNull = false;
                }
            }
        }
        // line is done - check status
        if (inQuotes) {
            if (multi) {
                // continuing a quoted section, re-append newline
                sb.append("\n");
                pending = sb.toString();
                sb = null; // this partial content is not to be added to field list yet
                isNull = false;
            } else {
                throw new IOException("Un-terminated quoted field at end of CSV line");
            }
        }
        if (isNull) {
    		tokensOnThisLine.add(null);
    	} else if (sb != null) {
    		tokensOnThisLine.add(sb.toString());
    	}
        return tokensOnThisLine.toArray(new String[tokensOnThisLine.size()]);

    }

    /**
     * precondition: the current character is a quote or an escape
     *
     * @param nextLine the current line
     * @param inQuotes true if the current context is quoted
     * @param i        current index in line
     * @return true if the following character is a quote
     */
    private boolean isNextCharacterEscapedQuote(String nextLine, boolean inQuotes, int i) {
    	i++;
        return inQuotes  // we are in quotes, therefore there can be escaped quotes in here.
                && nextLine.length() > i  // there is indeed another character to check.
                && nextLine.charAt(i) == quotechar;
    }
    
    /**
     * If the current character is a close quote
     *
     * @param nextLine the current line
     * @param inQuotes true if the current context is quoted
     * @param i        current index in line
     * @return true if the following character is a quote
     */
    private boolean isNextCharacterCloseQuote(String nextLine, boolean inQuotes, int i) {
    	i++;
        return inQuotes  // we are in quotes, therefore there can be escaped quotes in here.
                && (nextLine.length() <= i // end of nextline
                || nextLine.charAt(i) == this.separator || nextLine.charAt(i) == '\n');
    }

    /**
     * precondition: the current character is an escape
     *
     * @param nextLine the current line
     * @param inQuotes true if the current context is quoted
     * @param i        current index in line
     * @return true if the following character is a quote
     */
    protected boolean isNextCharacterEscapable(String nextLine, boolean inQuotes, int i) {
        return inQuotes  // we are in quotes, therefore there can be escaped quotes in here.
                && nextLine.length() > (i + 1)  // there is indeed another character to check.
                && (nextLine.charAt(i + 1) == quotechar || nextLine.charAt(i + 1) == this.escape);
    }

    /**
     * precondition: sb.length() > 0
     *
     * @param sb A sequence of characters to examine
     * @return true if every character in the sequence is whitespace
     */
    protected boolean isAllWhiteSpace(CharSequence sb) {
        boolean result = true;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);

            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return result;
    }

	public String getPending() {
		return pending;
	}

	public void setPending(String pending) {
		this.pending = pending;
	}

	public char getSeparator() {
		return separator;
	}

	public char getQuotechar() {
		return quotechar;
	}

	public char getEscape() {
		return escape;
	}

	public boolean isStrictQuotes() {
		return strictQuotes;
	}

	public boolean isIgnoreLeadingWhiteSpace() {
		return ignoreLeadingWhiteSpace;
	}
}
