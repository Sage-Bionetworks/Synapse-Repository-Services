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
	private static  BitSet dontNeedEncoding;
	static{
		dontNeedEncoding = new BitSet(256);
		int i;
		for (i = 'a'; i <= 'z'; i++) {
		    dontNeedEncoding.set(i);
		}
		for (i = 'A'; i <= 'Z'; i++) {
		    dontNeedEncoding.set(i);
		}
		for (i = '0'; i <= '9'; i++) {
		    dontNeedEncoding.set(i);
		}
		dontNeedEncoding.set('-');
		dontNeedEncoding.set('_');
		dontNeedEncoding.set('.');
		dontNeedEncoding.set('*');
	}
	
	/**
	 * Replace all invalid URL chars with '_'
	 * @param toEncode
	 * @return
	 */
	public static String replaceUrlChars(String toEncode){
		if(toEncode == null) throw new IllegalArgumentException("String to encode cannot be null");
		char[] chars = toEncode.toCharArray();
		for(int i=0; i<chars.length; i++){
			int intChar = chars[i];
			if(!dontNeedEncoding.get(intChar)){
				chars[i] = REPLACE_CHAR;
			}
		}
		return new String(chars);
	}
	
}
