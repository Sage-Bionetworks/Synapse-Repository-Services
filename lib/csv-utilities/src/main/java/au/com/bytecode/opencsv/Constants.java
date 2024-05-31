package au.com.bytecode.opencsv;

import java.nio.charset.StandardCharsets;

public class Constants {
	/**
     * The default separator to use if none is supplied to the constructor.
     */
    public static final char DEFAULT_SEPARATOR = ',';

    /**
     * The default quote character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    /** BACK SLASH character. */
    public static final char BACKSLASH_CHARACTER = '\\';
    
    /**
     * The default escape character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_ESCAPE_CHARACTER = BACKSLASH_CHARACTER;

    /**
     * This is the "null" character - if a value is set to this then it is ignored.
     * I.E. if the quote character is set to null then there is no quote character.
     */
    public static final char NULL_CHARACTER = '\0';
    
    /** The quote constant to use when you wish to suppress all quoting. */
    public static final char NO_QUOTE_CHARACTER = '\u0000';
    
    /** The escape constant to use when you wish to suppress all escaping. */
    public static final char NO_ESCAPE_CHARACTER = '\u0000';
    
    /** Default line terminator uses platform encoding. */
    public static final String DEFAULT_LINE_END = "\n";
    
    /**
     * Excel's CSV export function can start a file with the UTF-8 BOM (byte order marker) which is a non-ASCII character
     * used to indicate the file's encoding is UTF-8.  See: https://en.wikipedia.org/wiki/Byte_order_mark
     * 
     */
	public static final String UTF_8_BYTE_ORDER_MARKER = new String(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF },
			StandardCharsets.UTF_8);
    
}
