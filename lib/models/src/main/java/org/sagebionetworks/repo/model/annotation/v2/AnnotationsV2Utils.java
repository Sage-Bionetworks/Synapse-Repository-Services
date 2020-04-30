package org.sagebionetworks.repo.model.annotation.v2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator.AnnotationsV2TypeToValidator;
import org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator.AnnotationsV2ValueListValidator;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
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
	 * @throws JSONObjectAdapterException
	 */
	public static String toJSONStringForStorage(Annotations annotationsV2) throws JSONObjectAdapterException {
		if(annotationsV2 == null || annotationsV2.getAnnotations() == null || annotationsV2.getAnnotations().isEmpty()){
			return null;
		}

		//use a shallow copy which does not contain the id and etag info
		Annotations annotationsV2ShallowCopy = new Annotations();
		annotationsV2ShallowCopy.setAnnotations(annotationsV2.getAnnotations());

		return EntityFactory.createJSONStringForEntity(annotationsV2ShallowCopy);
	}

	/**
	 * Translate from NamedAnnotations to a list of AnnotationDTO.
	 * @param annos
	 * @param maxAnnotationChars the maximum number of characters for any annotation value.
	 * @return
	 */
	public static List<ObjectAnnotationDTO> translate(Long entityId, Annotations annos, int maxAnnotationChars) {
		LinkedHashMap<String, ObjectAnnotationDTO> map = new LinkedHashMap<>();
		if(annos != null){
			// add additional
			addAnnotations(entityId, maxAnnotationChars, map, annos);
		}
		// build the results from the map
		List<ObjectAnnotationDTO> results = new LinkedList<ObjectAnnotationDTO>();
		for(String key: map.keySet()){
			results.add(map.get(key));
		}
		return results;
	}

	private static void addAnnotations(Long entityId, int maxAnnotationChars,
									   LinkedHashMap<String, ObjectAnnotationDTO> map, Annotations additional) {
		if (additional.getAnnotations() == null){
			return;
		}
		for(Map.Entry<String, AnnotationsValue> entry: additional.getAnnotations().entrySet()){
			String key = entry.getKey();
			AnnotationsValue annotationsV2Value = entry.getValue();

			//ignore empty list or null list for values
			if(annotationsV2Value.getValue() == null || annotationsV2Value.getValue().isEmpty()){
				continue;
			}

			//enforce value max character limit
			List<String> transferredValues = new ArrayList<>();
			for (String value : annotationsV2Value.getValue()) {
				if(value == null){
					continue;
				}
				//make sure values are under the maxAnnotationChars limit
				String shortenedString = value.substring(0, Math.min(value.length(), maxAnnotationChars));
				transferredValues.add(shortenedString);
			}
			if(!transferredValues.isEmpty()) {
				map.put(key, new ObjectAnnotationDTO(entityId, key, AnnotationType.forAnnotationV2Type(annotationsV2Value.getType()), transferredValues));
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
}
