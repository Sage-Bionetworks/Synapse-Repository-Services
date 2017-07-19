package org.sagebionetworks.repo.model.jdo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.InvalidModelException;

public class AnnotationUtils {

	// match one or more whitespace characters
	private static final Pattern ALLOWABLE_CHARS = Pattern
			.compile("^[a-z,A-Z,0-9,_,.]+");

	/**
	 * Validate the name
	 * 
	 * @param key
	 * @throws InvalidModelException
	 */
	public static String checkKeyName(String key, Set<String> names) throws InvalidModelException {
		if (key == null)
			throw new InvalidModelException("Annotation names cannot be null");
		key = key.trim();
		if ("".equals(key))
			throw new InvalidModelException(
					"Annotation names cannot be empty strings");
		Matcher matcher = ALLOWABLE_CHARS.matcher(key);
		if (!matcher.matches()) {
			throw new InvalidModelException(
					"Invalid annotation name: '"
							+ key
							+ "'. Annotation names may only contain; letters, numbers, '_' and '.'");
		}
		if(!names.add(key)){
			throw new IllegalArgumentException("Duplicate annotation name: '"+key+"'");
		}
		return key;
	}

	/**
	 * Check all of the annotation names.
	 * @param updated
	 * @throws InvalidModelException
	 */
	public static void validateAnnotations(Annotations updated)	throws InvalidModelException {
		if (updated == null)
			throw new IllegalArgumentException("Annotations cannot be null");
		HashSet<String> uniqueNames = new HashSet<String>();

		// Validate the strings
		if (updated.getStringAnnotations() != null) {
			Iterator<String> it = updated.getStringAnnotations().keySet().iterator();
			while (it.hasNext()) {
				checkKeyName(it.next(), uniqueNames);
			}
		}
		// Validate the longs
		if (updated.getLongAnnotations() != null) {
			Iterator<String> it = updated.getLongAnnotations().keySet()
					.iterator();
			while (it.hasNext()) {
				checkKeyName(it.next(), uniqueNames);
			}
		}
		// Validate the dates
		if (updated.getDateAnnotations() != null) {
			Iterator<String> it = updated.getDateAnnotations().keySet()
					.iterator();
			while (it.hasNext()) {
				checkKeyName(it.next(), uniqueNames);
			}
		}
		// Validate the Doubles
		if (updated.getDoubleAnnotations() != null) {
			Iterator<String> it = updated.getDoubleAnnotations().keySet()
					.iterator();
			while (it.hasNext()) {
				checkKeyName(it.next(), uniqueNames);
			}
		}
		// Validate the Doubles
		if (updated.getBlobAnnotations() != null) {
			Iterator<String> it = updated.getBlobAnnotations().keySet()
					.iterator();
			while (it.hasNext()) {
				checkKeyName(it.next(), uniqueNames);
			}
		}

	}

}
