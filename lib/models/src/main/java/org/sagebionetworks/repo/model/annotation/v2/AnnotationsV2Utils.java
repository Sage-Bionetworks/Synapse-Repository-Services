package org.sagebionetworks.repo.model.annotation.v2;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator.AnnotationsV2TypeToValidator;
import org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator.AnnotationsV2ValueValidator;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;

public class AnnotationsV2Utils {

	static final int MAX_ANNOTATION_KEYS = 100;
	static final int MAX_VALUES_PER_KEY = 100;
	// match one or more whitespace characters
	private static final Pattern ALLOWABLE_CHARS = Pattern
			.compile("^[a-zA-Z0-9,_.]+");

	/**
	 *
	 * @return first value in the AnnotationV2 for the given key if it exists. null otherwise
	 */
	public static String getSingleValue(AnnotationsV2 annotationsV2, String key){
		ValidateArgument.required(annotationsV2, "annotationsV2");
		ValidateArgument.required(key, "key");
		Map<String, AnnotationsV2Value> map = annotationsV2.getAnnotations();
		if(map == null){
			return null;
		}

		return getSingleValue(map.get(key));
	}

	public static String getSingleValue(AnnotationsV2Value value){
		if(value == null){
			return null;
		}

		List<String> stringValues = value.getValue();
		if(stringValues == null || stringValues.isEmpty()){
			return null;
		}
		return stringValues.get(0);
	}

	/**
	 * Serializes ONLY the annotations, ignoring ID and Etag.
	 * @param annotationsV2
	 * @return JSON String of annotationsV2 if any annotations exist. Null otherwise
	 * @throws JSONObjectAdapterException
	 */
	public static String toJSONStringForStorage(AnnotationsV2 annotationsV2) throws JSONObjectAdapterException {
		if(annotationsV2 == null || annotationsV2.getAnnotations() == null || annotationsV2.getAnnotations().isEmpty()){
			return null;
		}

		//use a shallow copy which does not contain the id and etag info
		AnnotationsV2 annotationsV2ShallowCopy = new AnnotationsV2();
		annotationsV2ShallowCopy.setAnnotations(annotationsV2.getAnnotations());

		return EntityFactory.createJSONStringForEntity(annotationsV2ShallowCopy);
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
		if (additional.getAnnotations() == null){
			return;
		}
		for(Map.Entry<String, AnnotationsV2Value> entry: additional.getAnnotations().entrySet()){
			String key = entry.getKey();
			AnnotationsV2Value annotationsV2Value = entry.getValue();

			String value = getSingleValue(annotationsV2Value);
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
	public static void checkKeyName(String key) throws InvalidModelException {
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
			throw new IllegalArgumentException("value list for key=" + key + " can not be null");
		}
		if(valueList.size() > MAX_VALUES_PER_KEY){
			throw new IllegalArgumentException("key=" + key + " has exceeded the maximum number of values allowed: " + MAX_VALUES_PER_KEY);
		}

		AnnotationsV2ValueType type = annotationsV2Value.getType();
		if(type == null){
			throw new IllegalArgumentException("a value type must be set for values associated with key=" + key);
		}

		AnnotationsV2ValueValidator valueValidator = AnnotationsV2TypeToValidator.validatorFor(type);
		for(String value: valueList){
			if(value == null){
				throw new IllegalArgumentException("null is not allowed. To indicate no values, use an empty list.");
			}
			valueValidator.validate(key, value, type);
		}
	}

	/**
	 * Check all of the annotation names.
	 * @param annotation
	 * @throws InvalidModelException
	 */
	public static void validateAnnotations(AnnotationsV2 annotation)	throws InvalidModelException {
		ValidateArgument.required(annotation, "annotation");

		Map<String, AnnotationsV2Value> annotationsMap = annotation.getAnnotations();

		//no map being set is still valid for emptying the annotation.
		if(annotationsMap == null){
			return;
		}

		if(annotationsMap.size() > MAX_ANNOTATION_KEYS){
			throw new IllegalArgumentException("Exceeded maximum number of annotation keys: " + MAX_ANNOTATION_KEYS);
		}

		for (Map.Entry<String, AnnotationsV2Value> entry: annotationsMap.entrySet()) {
			String key = entry.getKey();
			checkKeyName(key);
			checkValue(key, entry.getValue());
		}
	}
}
