package org.sagebionetworks.util;

import java.lang.reflect.Field;

import org.springframework.util.ReflectionUtils;

public class ReflectionStaticTestUtils {

	public static void setStaticField(Class<?> clazz, String name, Object value) {
		Field field;
		try {
			field = clazz.getDeclaredField(name);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, null, value);
	}
}
