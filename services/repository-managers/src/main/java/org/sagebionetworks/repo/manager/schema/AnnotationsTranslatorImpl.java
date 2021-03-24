package org.sagebionetworks.repo.manager.schema;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JsonDateUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

@Service
public class AnnotationsTranslatorImpl implements AnnotationsTranslator {

	private static final String ID = "id";
	private static final String ETAG = "etag";
	public static final String CONCRETE_TYPE = "concreteType";

	@Override
	public JSONObject writeToJsonObject(Entity entity, Annotations annotations) {
		ValidateArgument.required(entity, "entity");
		ValidateArgument.required(annotations, "annotations");
		JSONObject jsonObject = new JSONObject();
		writeAnnotationsToJSONObject(annotations, jsonObject);
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(jsonObject);
		try {
			// write the entity second to override any conflicts.
			entity.writeToJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
		return jsonObject;
	}

	@Override
	public Annotations readFromJsonObject(Class<? extends Entity> entityClass, JSONObject jsonObject) {
		ValidateArgument.required(entityClass, "entity");
		ValidateArgument.required(jsonObject, "jsonObject");
		validateHasKeys(Lists.newArrayList(ID, ETAG, CONCRETE_TYPE), jsonObject);
		String entityId = jsonObject.getString(ID);
		if (!entityClass.getName().equals(jsonObject.get(CONCRETE_TYPE))) {
			throw new IllegalArgumentException(
					"The value of 'concreteType' does not match the type of Entity: '" + entityId + "'");
		}
		Annotations annotations = new Annotations();
		annotations.setId(entityId);
		annotations.setEtag(jsonObject.getString(ETAG));
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		annotations.setAnnotations(map);
		jsonObject.keySet().stream().filter(key -> canUseKey(entityClass, key)).forEach((key) -> {
			AnnotationsValue annValue = getAnnotationValueFromJsonObject(key, jsonObject);
			map.put(key, annValue);
		});
		return annotations;
	}

	void validateHasKeys(List<String> expectedKeys, JSONObject jsonObject) {
		for (String expectedKey : expectedKeys) {
			if (!jsonObject.has(expectedKey)) {
				throw new IllegalArgumentException("Expected JSON to include key: '" + expectedKey + "'");
			}
		}
	}

	/**
	 * Read a single AnnotationValue for the given key from the given JSONObject.
	 * 
	 * @param key
	 * @param jsonObject
	 * @return
	 */
	AnnotationsValue getAnnotationValueFromJsonObject(String key, JSONObject jsonObject) {
		// @formatter:off
		return Stream
				.of(
						attemptToReadAsJSONArray(key, jsonObject),
						attemptToReadAsDouble(key, jsonObject),
						attemptToReadAsTimestamp(key, jsonObject),
						attemptToReadAsLong(key, jsonObject),
						attemptToReadAsBoolean(key, jsonObject),
						attemptToReadAsString(key, jsonObject))
				.filter(Optional::isPresent).findFirst().get().orElseThrow(
						() -> new IllegalArgumentException("Cannot translate value at '" + key + "' to an Annotation"));
		// @formatter:on
	}

	/**
	 * Attempt to read the value for the given key as a string. Note: This should
	 * always work as any type can be read as a string.
	 * 
	 * @param key
	 * @param jsonObject
	 * @return
	 */
	Optional<AnnotationsValue> attemptToReadAsString(String key, JSONObject jsonObject) {
		try {
			String value = jsonObject.getString(key);
			AnnotationsValue annValue = new AnnotationsValue();
			annValue.setType(AnnotationsValueType.STRING);
			annValue.setValue(Collections.singletonList(value));
			return Optional.of(annValue);
		} catch (JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Attempt to read the value from the given key as a DATE_TIME formated string.
	 * An empty Optional will be returned if the value is not a DATE_TIME formated
	 * string.
	 * 
	 * @param key
	 * @param jsonObject
	 * @return
	 */
	Optional<AnnotationsValue> attemptToReadAsTimestamp(String key, JSONObject jsonObject) {
		try {
			String value = jsonObject.getString(key);
			long timeMS = JsonDateUtils.convertStringToDate(FORMAT.DATE_TIME, value).getTime();
			AnnotationsValue annValue = new AnnotationsValue();
			annValue.setType(AnnotationsValueType.TIMESTAMP_MS);
			annValue.setValue(Collections.singletonList(Long.toString(timeMS)));
			return Optional.of(annValue);
		} catch (IllegalArgumentException | JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Attempt to read the value at the given index as a string. Note: This should
	 * always work as any type can be read as a string.
	 * 
	 * @param index
	 * @param array
	 * @return
	 */
	Optional<ListValue> attemptToReadAsString(int index, JSONArray array) {
		try {
			String value = array.getString(index);
			return Optional.of(new ListValue(AnnotationsValueType.STRING, value));
		} catch (JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Attempt to read the value at the given index as a DATE_TIME formated string.
	 * If the value is not a DATE_TIME formated string an empty optional will be
	 * returned.
	 * 
	 * @param index
	 * @param array
	 * @return
	 */
	Optional<ListValue> attemptToReadAsTimestamp(int index, JSONArray array) {
		try {
			String value = array.getString(index);
			long timeMS = JsonDateUtils.convertStringToDate(FORMAT.DATE_TIME, value).getTime();
			return Optional.of(new ListValue(AnnotationsValueType.TIMESTAMP_MS, Long.toString(timeMS)));
		} catch (IllegalArgumentException | JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Attempt to read the value for the given key as a double. If the value is not
	 * a double an empty optional will be returned.
	 * 
	 * @param key
	 * @param jsonObject
	 * @return
	 */
	Optional<AnnotationsValue> attemptToReadAsDouble(String key, JSONObject jsonObject) {
		try {
			Double value = jsonObject.getDouble(key);
			String testString = jsonObject.getString(key);
			if (!testString.equals(value.toString())) {
				// data loss
				return Optional.empty();
			}
			AnnotationsValue annValue = new AnnotationsValue();
			annValue.setType(AnnotationsValueType.DOUBLE);
			annValue.setValue(Collections.singletonList(value.toString()));
			return Optional.of(annValue);
		} catch (JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Attempt to read the value for the given index as a double. If the value is
	 * not a double an empty optional will be returned.
	 * 
	 * @param index
	 * @param array
	 * @return
	 */
	Optional<ListValue> attemptToReadAsDouble(int index, JSONArray array) {
		try {
			Double value = array.getDouble(index);
			String testString = array.getString(index);
			if (!testString.equals(value.toString())) {
				// data loss
				return Optional.empty();
			}
			return Optional.of(new ListValue(AnnotationsValueType.DOUBLE, value.toString()));
		} catch (JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Attempt to read the value for the given key as a long. If the value is not a
	 * long an empty optional will be returned.
	 * 
	 * @param key
	 * @param jsonObject
	 * @return
	 */
	Optional<AnnotationsValue> attemptToReadAsLong(String key, JSONObject jsonObject) {
		try {
			Long value = jsonObject.getLong(key);
			AnnotationsValue annValue = new AnnotationsValue();
			annValue.setType(AnnotationsValueType.LONG);
			annValue.setValue(Collections.singletonList(value.toString()));
			return Optional.of(annValue);
		} catch (JSONException e) {
			return Optional.empty();
		}
	}
	
	/**
	 * Attempt to read the value for the given key as a boolean. If the value is not a
	 * boolean an empty optional will be returned.
	 * 
	 * @param key
	 * @param jsonObject
	 * @return
	 */
	Optional<AnnotationsValue> attemptToReadAsBoolean(String key, JSONObject jsonObject) {
		try {
			Boolean value = jsonObject.getBoolean(key);
			AnnotationsValue annValue = new AnnotationsValue();
			annValue.setType(AnnotationsValueType.BOOLEAN);
			annValue.setValue(Collections.singletonList(value.toString()));
			return Optional.of(annValue);
		} catch (JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Attempt to read the value at the given index as a long. If the value is not a
	 * long an empty optional will be returned.
	 * 
	 * @param index
	 * @param array
	 * @return
	 */
	Optional<ListValue> attemptToReadAsLong(int index, JSONArray array) {
		try {
			Long value = array.getLong(index);
			return Optional.of(new ListValue(AnnotationsValueType.LONG, value.toString()));
		} catch (JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Attempt to read the value for the given key as a JSONArray. If the value is
	 * not a JSONArray an empty optional will be returned.
	 * 
	 * @param key
	 * @param jsonObject
	 * @return
	 */
	Optional<AnnotationsValue> attemptToReadAsJSONArray(String key, JSONObject jsonObject) {
		try {
			JSONArray array = jsonObject.getJSONArray(key);
			List<String> valueList = new ArrayList<String>(array.length());
			AnnotationsValueType lastType = null;
			for (int i = 0; i < array.length(); i++) {
				ListValue listValue = Stream
						.of(attemptToReadAsDouble(i, array), attemptToReadAsTimestamp(i, array),
								attemptToReadAsLong(i, array), attemptToReadAsString(i, array))
						.filter(Optional::isPresent).findFirst().get().orElseThrow(() -> new IllegalArgumentException(
								"Cannot translate value at '" + key + "' to an Annotation"));
				if (lastType != null && !lastType.equals(listValue.getType())) {
					throw new IllegalArgumentException("List of mixed types found for key: '" + key + "'");
				}
				lastType = listValue.getType();
				valueList.add(listValue.getValue());
			}
			AnnotationsValue annValue = new AnnotationsValue();
			annValue.setType(lastType);
			annValue.setValue(valueList);
			return Optional.of(annValue);
		} catch (JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Field names of an Entity cannot be used as annotation keys. All other keys
	 * are valid.
	 * 
	 * @param clazz
	 * @param key
	 * @return
	 */
	public static boolean canUseKey(Class clazz, String key) {
		try {
			Field field = clazz.getDeclaredField(key);
			// static field names are allowed.
			return Modifier.isStatic(field.getModifiers());
		} catch (NoSuchFieldException e) {
			return true;
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Write the provided annotations to the provided JSONObject. Note: The
	 * resulting JSON format is not the same as the format used to write Annotations
	 * in API requests.
	 * 
	 * @param toWrite
	 * @param jsonObject
	 */
	void writeAnnotationsToJSONObject(Annotations toWrite, JSONObject jsonObject) {
		for (Entry<String, AnnotationsValue> entry : toWrite.getAnnotations().entrySet()) {
			writeAnnotationValue(entry.getKey(), entry.getValue(), jsonObject);
		}
	}

	void writeAnnotationValue(String key, AnnotationsValue value, JSONObject jsonObject) {
		if (value == null || value.getValue() == null || value.getType() == null) {
			return;
		}
		if (value.getValue().isEmpty()) {
			jsonObject.put(key, "");
		} else if (value.getValue().size() > 1) {
			JSONArray array = new JSONArray();
			jsonObject.put(key, array);
			value.getValue().forEach(s -> array.put(stringToObject(value.getType(), s)));
		} else {
			jsonObject.put(key, stringToObject(value.getType(), value.getValue().get(0)));
		}
	}

	Object stringToObject(AnnotationsValueType type, String value) {
		switch (type) {
		case STRING:
			return value;
		case DOUBLE:
			return Double.parseDouble(value);
		case LONG:
			return Long.parseLong(value);
		case TIMESTAMP_MS:
			return JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(Long.parseLong(value)));
		case BOOLEAN:
			return Boolean.parseBoolean(value);
		default:
			throw new IllegalArgumentException("Unknown annotation type: " + type);
		}
	}
}
