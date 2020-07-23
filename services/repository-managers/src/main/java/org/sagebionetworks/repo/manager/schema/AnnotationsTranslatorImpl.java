package org.sagebionetworks.repo.manager.schema;

import java.util.ArrayList;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class AnnotationsTranslatorImpl implements AnnotationsTranslator {

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
		Annotations annotations = new Annotations();
		annotations.setId(jsonObject.getString("id"));
		annotations.setEtag(jsonObject.getString("etag"));
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		annotations.setAnnotations(map);
		jsonObject.keySet().stream().filter(key -> canUseKey(entityClass, key)).forEach((key) -> {
			AnnotationsValue annValue = getAnnotationValueFromJsonObject(key, jsonObject);
			map.put(key, annValue);
		});
		return annotations;
	}

	/**
	 * Read a single AnnotationValue for the given key from the given JSONObject.
	 * 
	 * @param key
	 * @param jsonObject
	 * @return
	 */
	public AnnotationsValue getAnnotationValueFromJsonObject(String key, JSONObject jsonObject) {
		return Stream
				.of(attemptToReadAsJSONArray(key, jsonObject), attemptToReadAsDouble(key, jsonObject),
						attemptToReadAsLong(key, jsonObject), attemptToReadAsString(key, jsonObject))
				.filter(Optional::isPresent).findFirst().get().get();
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
			annValue.setValue(java.util.Collections.singletonList(value));
			return Optional.of(annValue);
		} catch (JSONException e) {
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
			AnnotationsValue annValue = new AnnotationsValue();
			annValue.setType(AnnotationsValueType.DOUBLE);
			annValue.setValue(java.util.Collections.singletonList(value.toString()));
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
			annValue.setValue(java.util.Collections.singletonList(value.toString()));
			return Optional.of(annValue);
		} catch (JSONException e) {
			return Optional.empty();
		}
	}

	/**
	 * Attempt to read the value at the given index as a long.  If the value is not a long
	 * an empty optional will be returned.
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
	 * Attempt to read the value for the given key as a JSONArray.  If the value is not a JSONArray
	 * an empty optional will be returned.
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
				ListValue listValue = Stream.of(attemptToReadAsDouble(i, array), attemptToReadAsLong(i, array),
						attemptToReadAsString(i, array)).filter(Optional::isPresent).findFirst().get().get();
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
			clazz.getDeclaredField(key);
			return false;
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
		ValidateArgument.required(toWrite, "Annotations");
		ValidateArgument.required(jsonObject, "JSONObject");
		for (Entry<String, AnnotationsValue> entry : toWrite.getAnnotations().entrySet()) {
			writeAnnotationValue(entry.getKey(), entry.getValue(), jsonObject);
		}
	}

	void writeAnnotationValue(String key, AnnotationsValue value, JSONObject jsonObject) {
		if (value.getValue() != null) {
			if (value.getValue().size() > 1) {
				writeAnnotationValueList(key, value.getType(), value.getValue(), jsonObject);
			} else {
				writeAnnotationSingle(key, value.getType(), value.getValue().get(0), jsonObject);
			}
		}
	}

	void writeAnnotationSingle(String key, AnnotationsValueType type, String value, JSONObject jsonObject) {
		switch (type) {
		case STRING:
			jsonObject.put(key, value);
			break;
		case DOUBLE:
			jsonObject.put(key, Double.parseDouble(value));
			break;
		case LONG:
		case TIMESTAMP_MS:
			jsonObject.put(key, Long.parseLong(value));
			break;
		default:
			throw new IllegalArgumentException("Unknown annotation type: " + type);
		}
	}

	void writeAnnotationValueList(String key, AnnotationsValueType type, List<String> values, JSONObject jsonObject) {
		JSONArray array = new JSONArray();
		switch (type) {
		case STRING:
			for (String value : values) {
				array.put(value);
			}
			break;
		case DOUBLE:
			for (String value : values) {
				array.put(Double.parseDouble(value));
			}
			break;
		case LONG:
		case TIMESTAMP_MS:
			for (String value : values) {
				array.put(Long.parseLong(value));
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown annotation type: " + type);
		}
		jsonObject.put(key, array);
	}
}
