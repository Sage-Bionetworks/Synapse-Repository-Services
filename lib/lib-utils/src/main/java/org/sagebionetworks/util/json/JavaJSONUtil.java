package org.sagebionetworks.util.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.util.json.translator.BooleanTranslator;
import org.sagebionetworks.util.json.translator.ByteArrayTranslator;
import org.sagebionetworks.util.json.translator.DateTranslator;
import org.sagebionetworks.util.json.translator.DoubleTranslator;
import org.sagebionetworks.util.json.translator.EnumTranslator;
import org.sagebionetworks.util.json.translator.JSONEntityTranslator;
import org.sagebionetworks.util.json.translator.LongTranslator;
import org.sagebionetworks.util.json.translator.StringTranslator;
import org.sagebionetworks.util.json.translator.TimestampTranslator;
import org.sagebionetworks.util.json.translator.Translator;

/**
 * A utility to write/read simple Java objects to/from JSON.
 * 
 */
public class JavaJSONUtil {

	public static final List<Translator<?, ?>> TRANSLATORS = Collections
			.unmodifiableList(Arrays.asList(new LongTranslator(), new StringTranslator(), new BooleanTranslator(),
					new ByteArrayTranslator(), new DoubleTranslator(), new DateTranslator(), new TimestampTranslator(),
					new JSONEntityTranslator(), new EnumTranslator()));

	public static JSONArray writeToJSON(List<?> objects) {
		JSONArray array = new JSONArray();
		for (Object object : objects) {
			array.put(writeToJSON(object));
		}
		return array;
	}

	public static <F, J> JSONObject writeToJSON(Object object) {
		JSONObject json = new JSONObject();
		Class clazz = object.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers())) {
				try {
					field.setAccessible(true);
					Object value = field.get(object);
					if (value != null) {
						Translator<F, J> transaltor = getTranslator(field.getType());
						json.put(field.getName(), transaltor.translateFieldValueToJSON(field.getType(), (F) value));
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		;
		return json;
	}

	static <F, J> Translator<F, J> getTranslator(Class type) {
		Translator<F, J> transaltor = (Translator<F, J>) TRANSLATORS.stream().filter(t -> t.canTranslate(type))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No translator found for: " + type.getName()));
		return transaltor;
	}

	public static <T> List<T> readFromJSON(Class<? extends T> clazz, JSONArray array) {
		List<T> list = new ArrayList<>(array.length());
		array.forEach(o -> {
			if (!(o instanceof JSONObject)) {
				throw new IllegalArgumentException("Expected JSONObjects but found: " + o.getClass().getName());
			}
			list.add(readFromJSON(clazz, (JSONObject) o));
		});
		return list;
	}

	public static <T, F, J> T readFromJSON(Class<? extends T> clazz, JSONObject o) {
		Optional<Constructor<?>> constructor = Arrays.stream(clazz.getDeclaredConstructors())
				.filter(c -> c.getParameterCount() == 0).findFirst();
		if (!constructor.isPresent()) {
			throw new IllegalArgumentException("No zero argument constructor found for:" + clazz.getName());
		}
		try {
			T object = (T) constructor.get().newInstance(null);
			for (Field field : clazz.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers())) {
					if (o.has(field.getName())) {
						field.setAccessible(true);
						Translator<F, J> transaltor = getTranslator(field.getType());
						J value = (J) getFromJSONObject(transaltor.getJSONClass(), field.getName(), o);
						field.set(object, transaltor.translateFromJSONToFieldValue(field.getType(), value));
					}
				}
			}
			return object;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}

	static Object getFromJSONObject(Class clazz, String key, JSONObject json) {
		if (String.class.equals(clazz)) {
			return json.getString(key);
		} else if (Long.class.equals(clazz)) {
			return json.getLong(key);
		} else if (Double.class.equals(clazz)) {
			return json.getDouble(key);
		} else if (Boolean.class.equals(clazz)) {
			return json.getBoolean(key);
		} else if(JSONObject.class.equals(clazz)){
			return json.getJSONObject(key);
		}
		throw new IllegalArgumentException("Unknown JSON type: "+clazz.getName());
	}

}
