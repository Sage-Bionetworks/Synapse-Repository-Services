package org.sagebionetworks.repo.model.jdo;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Value;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;
import org.sagebionetworks.repo.model.jdo.annotaitonvalidator.AnnotationsV2TypeToValidator;
import org.sagebionetworks.repo.model.jdo.annotaitonvalidator.AnnotationsV2ValueValidator;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.util.ValidateArgument;

public class AnnotationUtils {
	//TODO: sync with AnnotationV2Utils

	private static final int MAX_ANNOTATION_KEYS = 100;
	private static final int MAX_VALUES_PER_KEY = 100;

	// match one or more whitespace characters
	private static final Pattern ALLOWABLE_CHARS = Pattern
			.compile("^[a-zA-Z0-9,_.]+");

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.omitField(Annotations.class, "id")
			.omitField(Annotations.class, "etag")
			.alias("annotations", Annotations.class)
			.build();

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
	 * Translate from NamedAnnotations to a list of AnnotationDTO.
	 * @param annos
	 * @param maxAnnotationChars the maximum number of characters for any annotation value.
	 * @return
	 */
	public static List<AnnotationDTO> translate(Long entityId, AnnotationsV2 annos, int maxAnnotationChars) {
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
									   LinkedHashMap<String, AnnotationDTO> map, AnnotationsV2 additional) {
		for(Map.Entry<String, AnnotationsV2Value> entry: additional.getAnnotations().entrySet()){
			String key = entry.getKey();
			AnnotationsV2Value annotationsV2Value = entry.getValue();


			String value = AnnotationsV2Utils.getSingleValue(annotationsV2Value);
			if(value != null){
				//enforce value max character limit
				if(value.length() > maxAnnotationChars){
					value = value.substring(0, maxAnnotationChars);
				}

				map.put(key, new AnnotationDTO(entityId, key, AnnotationType.forAnnotationV2Type(annotationsV2Value.getType()), value));
			}
		}
	}

	/**
	 * Validate the name
	 * 
	 * @param key
	 * @throws InvalidModelException
	 */
	static void checkKeyName(String key) throws InvalidModelException {
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
	}

	static void checkValue(String key, AnnotationsV2Value annotationsV2Value){
		List<String> valueList = annotationsV2Value.getValue();
		if(valueList == null){
			throw new IllegalArgumentException("value for key: " + key + " can not be null");
		}
		if(valueList.size() > MAX_VALUES_PER_KEY){
			throw new IllegalArgumentException("key: " + key + " has exceeded the maximum number of values allowed: " + MAX_VALUES_PER_KEY);
		}

		AnnotationsV2ValueType type = annotationsV2Value.getType();
		AnnotationsV2ValueValidator valueValidator = AnnotationsV2TypeToValidator.validatorFor(type);
		for(String value: valueList){
			if(!valueValidator.isValidValue(value)){
				throw new IllegalArgumentException("Value is not a valid for type " + type.name() + ": " + value);
			}
		}
	}

	/**
	 * Check all of the annotation names.
	 * @param annotation
	 * @throws InvalidModelException
	 */
	public static void validateAnnotations(AnnotationsV2 annotation)	throws InvalidModelException {
		//TODO: test
		ValidateArgument.required(annotation, "annotation");
		ValidateArgument.requiredNotEmpty(annotation.getEtag(), "etag");

		// Validate the strings
		if (annotation.getAnnotations() != null) {
			if(annotation.getAnnotations().size() > MAX_ANNOTATION_KEYS){
				throw new IllegalArgumentException("Exceeded maximum number of annotation keys: " + MAX_ANNOTATION_KEYS);
			}

			for (Map.Entry<String, AnnotationsV2Value> entry: annotation.getAnnotations().entrySet()) {
				checkKeyName(entry.getKey());
				checkValue(entry.getKey(), entry.getValue());
			}
		}

	}

}
