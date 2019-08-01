package org.sagebionetworks.repo.model.jdo;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;

public class AnnotationUtils {

	// match one or more whitespace characters
	private static final Pattern ALLOWABLE_CHARS = Pattern
			.compile("^[a-z,A-Z,0-9,_,.]+");

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.omitField(Annotations.class, "id")
			.omitField(Annotations.class, "etag")
			.omitField(NamedAnnotations.class, "id")
			.omitField(NamedAnnotations.class, "etag")
			.allowTypes(NamedAnnotations.class, Annotations.class)
			.alias("annotations", Annotations.class)
			.alias("name-space", NamedAnnotations.class)
			.build();


	/**
	 * Convert the passed annotations to a compressed (zip) byte array
	 * @param dto
	 * @return compressed annotations
	 * @throws IOException
	 */
	public static byte[] compressAnnotations(NamedAnnotations dto) throws IOException{
		return JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto == null || dto.isEmpty() ? null : dto);
	}

	/**
	 * Read the compressed (zip) byte array into the Annotations.
	 * @param zippedBytes
	 * @return the resurrected Annotations
	 * @throws IOException
	 */
	public static NamedAnnotations decompressedAnnotations(byte[] zippedBytes) throws IOException{
		Object o = JDOSecondaryPropertyUtils.decompressObject(X_STREAM, zippedBytes);
		if (o==null) return new NamedAnnotations();
		return (NamedAnnotations) o;
	}

	/**
	 * Convert the passed annotations to a compressed (zip) byte array
	 * @param dto
	 * @return compressed annotations
	 * @throws IOException
	 */
	public static byte[] compressAnnotationsV1(Annotations dto) throws IOException{
		return JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto == null || dto.isEmpty() ? null : dto);
	}

	/**
	 * Read the compressed (zip) byte array into the Annotations.
	 * @param zippedBytes
	 * @return the resurrected Annotations
	 * @throws IOException
	 */
	public static Annotations decompressedAnnotationsV1(byte[] zippedBytes) throws IOException{
		Object o = JDOSecondaryPropertyUtils.decompressObject(X_STREAM, zippedBytes);
		if (o==null) return new Annotations();
		return (Annotations) o;
	}


	/**
	 * Get a single string value from a list of objects.
	 * @param values
	 * @param maxAnnotationChars the maximum number of characters for any annotation value.
	 * @return
	 */
	public static String getSingleString(List values, int maxAnnotationChars){
		if(values == null){
			return null;
		}
		if(values.isEmpty()){
			return null;
		}
		Object value = values.get(0);
		if(value == null){
			return null;
		}
		String stringValue = null;
		if(value instanceof Date){
			stringValue = ""+((Date)value).getTime();
		}else{
			stringValue = value.toString();
		}
		if(stringValue.length() > maxAnnotationChars){
			stringValue = stringValue.substring(0, maxAnnotationChars);
		}
		return stringValue;
	}

	/**
	 * Translate from NamedAnnotations to a list of AnnotationDTO.
	 * @param annos
	 * @param maxAnnotationChars the maximum number of characters for any annotation value.
	 * @return
	 */
	public static List<AnnotationDTO> translate(Long entityId, Annotations annos, int maxAnnotationChars) {
		LinkedHashMap<String, AnnotationDTO> map = new LinkedHashMap<>();
		if(annos != null){
			// add additional
			addAnnotations(entityId, maxAnnotationChars, map, annos);
		}
		// build the results from the map
		List<AnnotationDTO> results = new LinkedList<AnnotationDTO>();
		for(String key: map.keySet()){
			results.add(map.get(key));
		}
		return results;
	}

	private static void addAnnotations(Long entityId, int maxAnnotationChars,
									   LinkedHashMap<String, AnnotationDTO> map, Annotations additional) {
		for(String key: additional.getStringAnnotations().keySet()){
			List values = additional.getStringAnnotations().get(key);
			String value = getSingleString(values, maxAnnotationChars);
			if(value != null){
				map.put(key, new AnnotationDTO(entityId, key, AnnotationType.STRING, value));
			}
		}
		// longs
		for(String key: additional.getLongAnnotations().keySet()){
			List values = additional.getLongAnnotations().get(key);
			String value = getSingleString(values, maxAnnotationChars);
			if(value != null){
				map.put(key, new AnnotationDTO(entityId, key, AnnotationType.LONG, value));
			}
		}
		// doubles
		for(String key: additional.getDoubleAnnotations().keySet()){
			List values = additional.getDoubleAnnotations().get(key);
			String value = getSingleString(values, maxAnnotationChars);
			if(value != null){
				map.put(key, new AnnotationDTO(entityId, key, AnnotationType.DOUBLE, value));
			}
		}
		// dates
		for(String key: additional.getDateAnnotations().keySet()){
			List values = additional.getDateAnnotations().get(key);
			String value = getSingleString(values, maxAnnotationChars);
			if(value != null){
				map.put(key, new AnnotationDTO(entityId, key, AnnotationType.DATE, value));
			}
		}
	}

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
