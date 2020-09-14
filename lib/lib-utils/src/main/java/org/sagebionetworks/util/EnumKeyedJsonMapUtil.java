package org.sagebionetworks.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;

/**
 * Utils for converting between maps that should be enum-keyed, but are string-keyed due to limitations in schema-to-pojo. See PLFM-6254.
 *
 * Note: you should only use this class if your schema property MUST be a JSON map (e.g. to adhere to a formal specification).
 * If you need a non-string keyed POJO, but don't care about the JSON format, use `tuplearraymap` in your schema-to-pojo schema
 */
public class EnumKeyedJsonMapUtil {
	/**
	 * Converts a map with {@link String} keys to a map with {@link Enum} keys, using a supplied enumeration class.
	 *
	 * This method provides a workaround for a limitation in schema-to-pojo where a schema cannot represent a JSON
	 * map unless the key is a string.
	 *
	 * If a given key does not map to an enumeration value, it will be omitted and no exception will be thrown.
	 * @param map
	 * @param enumClazz
	 * @return a map containing entries with keys that mapped to valid enums.
	 */
	public static <E extends Enum<E>, V> Map<E, V> convertKeysToEnums(Map<String, V> map, Class<E> enumClazz) {
		Map<E, V> enumMap = new EnumMap<>(enumClazz);
		for (Map.Entry<String, V> entry : map.entrySet()) {
			if (EnumUtils.isValidEnum(enumClazz, entry.getKey())) {
				enumMap.put(E.valueOf(enumClazz, entry.getKey()), entry.getValue());
			}
		}
		return enumMap;
	}

	/**
	 * Converts a map with enum keys to a map with String keys.
	 * @param map
	 * @param <V>
	 * @return
	 */
	public static <V> Map<String, V> convertKeysToStrings(Map<? extends Enum<?>, V> map) {
		Map<String, V> stringMap = new HashMap<>();
		for (Map.Entry<? extends Enum<?>, V> entry : map.entrySet()) {
			stringMap.put(entry.getKey().name(), entry.getValue());
		}
		return stringMap;
	}

}
