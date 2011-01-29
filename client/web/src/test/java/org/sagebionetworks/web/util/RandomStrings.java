package org.sagebionetworks.web.util;

import java.util.Random;

public class RandomStrings {
	
	private static final Random rand = new Random(12345);
	
	// this is the array of valid characters.
	private static char[] validChars = null;
	static{
		int length = 'z'-'a'+1;
		validChars = new char[length];
		for(int i=0; i<length;i++){
			validChars[i] = (char) (i+'a');
		}
	}
	
	/**
	 * Generate a random word from the of a given length
	 * @param builder
	 * @param length
	 */
	public static void generateRandomWord(StringBuilder builder, int length){
		int maxChar = validChars.length-1;
//		builder.append(RandomStringUtils.random(length, validChars));
		for(int i=0; i<length; i++){
			int charIndex = rand.nextInt(maxChar);
			builder.append(validChars[charIndex]);
		}
	}
	
	/**
	 * Generate a random string with words separated by the given delimiter.
	 * @param builder
	 * @param numberWords
	 * @param maxWordSize
	 * @param delimiter
	 */
	public static void generateRandomString(StringBuilder builder, int numberWords, int maxWordSize, String delimiter){
		for(int i=0; i<numberWords; i++){
			// Add a word
			int wordSize = rand.nextInt(maxWordSize)+1;
			if(i > 0){
				builder.append(delimiter);
			}
			generateRandomWord(builder, wordSize);
		}
	}
	
	/**
	 * Generate a random string composed of random words.
	 * @param builder
	 * @param numberWords
	 */
	public static void generateRandomString(StringBuilder builder, int numberWords, int maxWordSize){
		// Space delimiter.
		generateRandomString(builder, numberWords, maxWordSize, " ");
	}
	
	/**
	 * Another way to expose random strings.
	 * @param numberWords
	 * @param maxWordSize
	 * @return
	 */
	public static String generateRandomString(int numberWords, int maxWordSize){
		StringBuilder builder = new StringBuilder();
		generateRandomString(builder, numberWords, maxWordSize);
		return builder.toString();
	}
	
	/**
	 * Generates a random url from parts.
	 * @param builder
	 * @param numberWords
	 */
	public static void generateRandomUrl(StringBuilder builder, int numParts, int maxWordSize){
		builder.append("http://");
		generateRandomString(builder, numParts, maxWordSize, "/");
	}
	/**
	 * Another way to generate random urls.
	 * @param numParts
	 * @param maxWordSize
	 * @return
	 */
	public static String generateRandomUrl(int numParts, int maxWordSize){
		StringBuilder builder = new StringBuilder();
		generateRandomUrl(builder, numParts, maxWordSize);
		return builder.toString();
	}

}
