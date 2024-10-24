package org.sagebionetworks.util.json;

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
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.json.translator.ByteArrayTranslator;
import org.sagebionetworks.util.json.translator.DateTranslator;
import org.sagebionetworks.util.json.translator.EnumTranslator;
import org.sagebionetworks.util.json.translator.IdentityTranslator;
import org.sagebionetworks.util.json.translator.JSONEntityTranslator;
import org.sagebionetworks.util.json.translator.JSONType;
import org.sagebionetworks.util.json.translator.TimestampTranslator;
import org.sagebionetworks.util.json.translator.Translator;

/**
 * A utility to write/read simple Java objects to/from JSON.
 * 
 */
public class JavaJSONUtil {

	public static final List<Translator<?, ?>> TRANSLATORS = Collections.unmodifiableList(Arrays.asList(
			new IdentityTranslator<>(Long.class, long.class), new IdentityTranslator<>(Integer.class, int.class),
			new IdentityTranslator<>(String.class), new IdentityTranslator<>(Boolean.class, boolean.class),
			new IdentityTranslator<>(Double.class, double.class), new ByteArrayTranslator(), new DateTranslator(),
			new TimestampTranslator(), new JSONEntityTranslator(), new EnumTranslator()));

	/**
	 * Write the provided list of simple Java objects to a JSONArray. Each object
	 * will be a single JSONObject within the resulting JSONArray.
	 * 
	 * @param objects
	 * @return Returns {@link Optional#empty()} if no data was written, else a new
	 *         JSONArray that contains the data of the provide list of Java objects.
	 */
	public static Optional<JSONArray> writeToJSON(List<?> objects) {
		ValidateArgument.required(objects, "objects");
		JSONArray array = new JSONArray();
		for (Object object : objects) {
			writeToJSON(object).ifPresent(o -> array.put(o));
		}
		return array.length() > 0 ? Optional.of(array) : Optional.empty();
	}

	/**
	 * Write a single simple Java object to a JSONObject.
	 * 
	 * @param object
	 * @return Returns {@link Optional#empty()} if no data was written, else a new
	 *         JSONObject that contains the data of the provided Java object.
	 * 
	 */
	public static Optional<JSONObject> writeToJSON(Object object) {
		return writeToJSON(TRANSLATORS, object);
	}

	/**
	 * Read a list of simple Java objects from the provided JSONArray.
	 * 
	 * @param <T>
	 * @param clazz The class of the resulting Java objects.
	 * @param array The JSONArray containing the data to read.
	 * @return
	 */
	public static <T> List<T> readFromJSON(Class<? extends T> clazz, JSONArray array) {
		ValidateArgument.required(array, "array");
		ValidateArgument.required(clazz, "clazz");

		List<T> list = new ArrayList<>(array.length());
		array.forEach(o -> {
			if (!(o instanceof JSONObject)) {
				throw new IllegalArgumentException("Expected JSONObjects but found: " + o.getClass().getName());
			}
			list.add(readFromJSON(clazz, (JSONObject) o));
		});
		return list;
	}

	/**
	 * Read a single simple Java object from the provide JSONObject.
	 * 
	 * @param <T>
	 * @param clazz  The class of the resulting Java object.
	 * @param object
	 * @return
	 */
	public static <T> T readFromJSON(Class<? extends T> clazz, JSONObject object) {
		return readFromJSON(TRANSLATORS, clazz, object);
	}

	@SuppressWarnings("unchecked")
	static <F, J> Optional<JSONObject> writeToJSON(List<Translator<?, ?>> translators, Object object) {
		ValidateArgument.required(translators, "translators");
		ValidateArgument.required(object, "object");

		JSONObject json = new JSONObject();
		boolean wasWritten = false;
		Class<? extends Object> clazz = object.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers())) {
				try {
					field.setAccessible(true);
					Object value = field.get(object);
					if (value != null) {
						Translator<F, J> transaltor = findTranslator(translators, field.getType());
						json.put(field.getName(), transaltor.translateFromJavaToJSON((F) value));
						wasWritten = true;
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return wasWritten ? Optional.of(json) : Optional.empty();
	}

	/**
	 * Helper to find a translator for the provided type.
	 * 
	 * @param <F>
	 * @param <J>
	 * @param translators The is of possible translator.
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	static <F, J> Translator<F, J> findTranslator(List<Translator<?, ?>> translators, Class<?> type) {
		ValidateArgument.required(translators, "translators");
		ValidateArgument.required(type, "type");

		Translator<F, J> transaltor = (Translator<F, J>) translators.stream().filter(t -> t.canTranslate(type))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No translator found for: " + type.getName()));
		return transaltor;
	}

	@SuppressWarnings("unchecked")
	static <T, F, J> T readFromJSON(List<Translator<?, ?>> translators, Class<? extends T> clazz,
			JSONObject jsonObject) {
		ValidateArgument.required(translators, "translators");
		ValidateArgument.required(clazz, "type");
		ValidateArgument.required(jsonObject, "jsonObject");

		try {
			T newObject = (T) createNewInstance(clazz);
			for (Field field : clazz.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers())) {
					if (jsonObject.has(field.getName())) {
						field.setAccessible(true);
						Translator<F, J> transaltor = findTranslator(translators, field.getType());
						J jsonValue = (J) JSONType.lookupType(transaltor.getJSONClass()).getFromJSON(field.getName(),
								jsonObject);
						field.set(newObject, transaltor.translateFromJSONToJava((Class<F>) field.getType(), jsonValue));
					}
				}
			}
			return newObject;
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Create a new Java object from the given class. The calls must provide a zero
	 * argument constructor.
	 * 
	 * @param type
	 * @return
	 */
	public static Object createNewInstance(Class<?> type) {
		try {
			return type.getConstructor((Class<?>[]) null).newInstance((Object[]) null);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("A zero argument constructor could not be found for: " + type.getName());
		}
	}

}
