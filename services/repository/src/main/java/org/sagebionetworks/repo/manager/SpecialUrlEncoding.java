package org.sagebionetworks.repo.manager;

import java.util.BitSet;

/**
 * Unlike java.net.URLEncoder, we want to replace all invalid url chars with an underscore since they names are
 * part of an S3 singed URL.
 * @author John
 *
 */
public class SpecialUrlEncoding {
	
	/**
	 * All invalid chars are replaced with this.
	 * 
	 */
	public static char REPLACE_CHAR = '_';
	// Basically the same set as java.net.URLEncoder without the space char.
	private static BitSet validChars;
	private static BitSet validCharsIgnoreSlashes;
	static{
		validChars = new BitSet(256);
		int i;
		for (i = 'a'; i <= 'z'; i++) {
		    validChars.set(i);
		}
		for (i = 'A'; i <= 'Z'; i++) {
		    validChars.set(i);
		}
		for (i = '0'; i <= '9'; i++) {
		    validChars.set(i);
		}
		validChars.set('-');
		validChars.set('_');
		validChars.set('.');
		validChars.set('*');
		
		
		validCharsIgnoreSlashes = (BitSet) validChars.clone();
		validCharsIgnoreSlashes.set('\\');
		validCharsIgnoreSlashes.set('/');
	}
	
	/**
	 * Replace all invalid URL chars with '_'
	 * @param toEncode
	 * @return processed URL-valid string
	 */
	public static String replaceUrlChars(String toEncode) {
		return replaceHelper(toEncode, validChars);
	}
	
	/**
	 * Replace all invalid URL chars with '_'. Ignore slashes (as with a hierarchical path).
	 * @param toEncode
	 * @return processed URL-valid string
	 */
	public static String replaceUrlCharsIgnoreSlashes(String toEncode) {
		return replaceHelper(toEncode, validCharsIgnoreSlashes);
	}
	
	private static String replaceHelper(String toEncode, BitSet validChars){
		if(toEncode == null) throw new IllegalArgumentException("String to encode cannot be null");
		char[] chars = toEncode.toCharArray();
		for(int i=0; i<chars.length; i++){
			int intChar = chars[i];
			if(!validChars.get(intChar)){
				chars[i] = REPLACE_CHAR;
			}
		}
		return new String(chars);
	}
	
	
	
	
}
