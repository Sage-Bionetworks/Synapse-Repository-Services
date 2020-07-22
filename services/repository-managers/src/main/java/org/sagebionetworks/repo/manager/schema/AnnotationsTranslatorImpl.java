package org.sagebionetworks.repo.manager.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.management.RuntimeErrorException;

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

import io.jsonwebtoken.lang.Collections;

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
		jsonObject.keySet().stream().filter( key -> canUseKey(entityClass, key)).forEach((key)->{
			AnnotationsValue annValue = getValueFromJsonObject(key, jsonObject);
			map.put(key, annValue);
		});
		return annotations;
	}
	
	public AnnotationsValue getValueFromJsonObject(String key, JSONObject jsonObject) {
		Optional<AnnotationsValue> result = tryStringValue(key, jsonObject).map(Optional::of)    
				  .orElseGet(() -> tryDouble(key, jsonObject)).map(Optional::of)    
				  .orElseGet(() -> tryLong(key, jsonObject)).map(Optional::of)    
				  .orElseGet(() -> tryArray(key, jsonObject));
		if(!result.isPresent()) {
			throw new IllegalArgumentException("Cannot translate from the provided JSON to an AnnotationValue");
		}
		return result.get();
	}
	
	Optional<AnnotationsValue> tryStringValue(String key, JSONObject jsonObject){
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
	
	Optional<AnnotationsValue> tryDouble(String key, JSONObject jsonObject){
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
	
	Optional<AnnotationsValue> tryLong(String key, JSONObject jsonObject){
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
	
	Optional<AnnotationsValue> tryArray(String key, JSONObject jsonObject){
		try {
			JSONArray value = jsonObject.getJSONArray(key);
			AnnotationsValue annValue = new AnnotationsValue();
			annValue.setType(AnnotationsValueType.LONG);
			annValue.setValue(java.util.Collections.singletonList(value.toString()));
			return Optional.of(annValue);
		} catch (JSONException e) {
			return Optional.empty();
		}
	}
	
	/**
	 * Field names of an Entity cannot be used as annotation keys. All other keys are valid.
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

	void writeAnnotationsToJSONObject(Annotations toWrite, JSONObject jsonObject) {
		ValidateArgument.required(toWrite, "Annotations");
		ValidateArgument.required(jsonObject, "JSONObject");
		for(Entry<String, AnnotationsValue> entry: toWrite.getAnnotations().entrySet()) {
			writeAnnotationValue(entry.getKey(), entry.getValue(), jsonObject);
		}
	}
	
	void writeAnnotationValue(String key, AnnotationsValue value, JSONObject jsonObject) {
		if(value.getValue() != null) {
			if(value.getValue().size() > 1) {
				writeAnnotationValueList(key, value.getType(), value.getValue(), jsonObject);
			}else {
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
			for(String value: values) {
				array.put(value);
			}
			break;
		case DOUBLE:
			for(String value: values) {
				array.put(Double.parseDouble(value));
			}
			break;
		case LONG:
		case TIMESTAMP_MS:
			for(String value: values) {
				array.put(Long.parseLong(value));
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown annotation type: " + type);
		}
		jsonObject.put(key, array);
	}
}
