package org.sagebionetworks.repo.model.annotation.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator.AnnotationsV2TypeToValidator;
import org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator.AnnotationsV2ValueListValidator;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
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
	public static String getSingleValue(Annotations annotationsV2, String key){
		ValidateArgument.required(annotationsV2, "annotationsV2");
		ValidateArgument.required(key, "key");
		Map<String, AnnotationsValue> map = annotationsV2.getAnnotations();
		if(map == null){
			return null;
		}

		return getSingleValue(map.get(key));
	}

	public static String getSingleValue(AnnotationsValue value){
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
	 * @throws DatastoreException If the annotations could not be serialized to json
	 */
	public static String toJSONStringForStorage(Annotations annotationsV2) {
		if (isEmpty(annotationsV2)) {
			return null;
		}

		//use a shallow copy which does not contain the id and etag info
		Annotations annotationsV2ShallowCopy = new Annotations();
		annotationsV2ShallowCopy.setAnnotations(annotationsV2.getAnnotations());

		try {
			return EntityFactory.createJSONStringForEntity(annotationsV2ShallowCopy);
		} catch (JSONObjectAdapterException e) {
			throw new DatastoreException("Could not serialize annotations: " + e.getMessage(), e);
		}
	}

	/**
	 * Deserialize the given json string to an instance of an {@link Annotations}. 
	 * If the input is null or empty return null
	 * 
	 * @param jsonAnnotations The serialized annotations
	 * @return An instance of {@link Annotations} deserialized from the given json string
	 */
	public static Annotations fromJSONString(String jsonAnnotations) {
		if (StringUtils.isEmpty(jsonAnnotations)) {
			return null;
		}
		try {
			return EntityFactory.createEntityFromJSONString(jsonAnnotations, Annotations.class);
		} catch (JSONObjectAdapterException e) {
			throw new DatastoreException(e);
		}
	}

	/**
	 * Translate from NamedAnnotations to a list of AnnotationDTO.
	 * @param annos
	 * @param maxAnnotationChars the maximum number of characters for any annotation value.
	 * @return
	 */
	public static List<ObjectAnnotationDTO> translate(Long entityId, Annotations annos, int maxAnnotationChars) {
		Map<String, ObjectAnnotationDTO> map;
		
		if (annos == null || annos.getAnnotations() == null) {
			map = Collections.emptyMap();
		} else {
			map = new LinkedHashMap<>(annos.getAnnotations().size());
			// add additional
			addAnnotations(entityId, maxAnnotationChars, map, annos.getAnnotations());
		}
		// build the results from the map
		return map.values().stream().collect(Collectors.toList());
	}

	private static void addAnnotations(Long entityId, int maxAnnotationChars, Map<String, ObjectAnnotationDTO> map, Map<String, AnnotationsValue> additional) {
		
		additional.forEach((String key, AnnotationsValue annotationsV2Value) -> {
			//ignore empty list or null list for values
			if (annotationsV2Value.getValue() == null || annotationsV2Value.getValue().isEmpty()){
				return;
			}

			List<String> transferredValues = new ArrayList<>(annotationsV2Value.getValue().size());

			//enforce value max character limit			
			for (String value : annotationsV2Value.getValue()) {
				if (value == null) {
					continue;
				}
				//make sure values are under the maxAnnotationChars limit
				String shortenedString = StringUtils.truncate(value, maxAnnotationChars);
				transferredValues.add(shortenedString);
			}
			
			if(!transferredValues.isEmpty()) {
				AnnotationType annotationType = AnnotationType.forAnnotationV2Type(annotationsV2Value.getType());
				map.put(key, new ObjectAnnotationDTO(entityId, key, annotationType, transferredValues));
			}
		});
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

	static void checkValue(String key, AnnotationsValue annotationsV2Value){
		List<String> valueList = annotationsV2Value.getValue();
		if(valueList == null){
			throw new IllegalArgumentException("value list for key=" + key + " can not be null");
		}
		if(valueList.size() > MAX_VALUES_PER_KEY){
			throw new IllegalArgumentException("key=" + key + " has exceeded the maximum number of values allowed: " + MAX_VALUES_PER_KEY);
		}

		AnnotationsValueType type = annotationsV2Value.getType();
		if(type == null){
			throw new IllegalArgumentException("a value type must be set for values associated with key=" + key);
		}

		AnnotationsV2ValueListValidator valueValidator = AnnotationsV2TypeToValidator.validatorFor(type);
		valueValidator.validate(key, valueList);
	}

	/**
	 * Check all of the annotation names.
	 * @param annotation
	 * @throws InvalidModelException
	 */
	public static void validateAnnotations(Annotations annotation)	throws InvalidModelException {
		ValidateArgument.required(annotation, "annotation");

		Map<String, AnnotationsValue> annotationsMap = annotation.getAnnotations();

		//no map being set is still valid for emptying the annotation.
		if(annotationsMap == null){
			return;
		}

		if(annotationsMap.size() > MAX_ANNOTATION_KEYS){
			throw new IllegalArgumentException("Exceeded maximum number of annotation keys: " + MAX_ANNOTATION_KEYS);
		}

		for (Map.Entry<String, AnnotationsValue> entry: annotationsMap.entrySet()) {
			String key = entry.getKey();
			checkKeyName(key);
			checkValue(key, entry.getValue());
		}
	}
	
	public static Annotations emptyAnnotations() {
		Annotations annotations = new Annotations();
		annotations.setAnnotations(new HashMap<>());
		return annotations;
	}
	
	public static boolean isEmpty(Annotations annotations) {
		if (annotations == null || annotations.getAnnotations() == null || annotations.getAnnotations().isEmpty()) {
			return true;
		}
		return false;
	}
}
