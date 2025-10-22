package org.sagebionetworks.repo.model.grid.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

public class ConTypeTest {

	@ParameterizedTest
	@EnumSource(value = ConType.class, mode = Mode.EXCLUDE, names = {"NULL", "UNDEFINED"})
	public void testFromValue(ConType expectedType) {		
		for (Class<?> clazz : expectedType.getSupportedClasses()) {
			assertEquals(expectedType, ConType.fromValue(getTestValue(clazz)));
		}
	}
	
	private Object getTestValue(Class<?> clazz) {
		if (Boolean.class.isAssignableFrom(clazz)) {
			return true;
		}
		if (Long.class.isAssignableFrom(clazz)) {
			return 123L;
		}
		if (Integer.class.isAssignableFrom(clazz)) {
			return 123;
		}
		if (Short.class.isAssignableFrom(clazz)) {
			return Short.valueOf("1");
		}
		if (Double.class.isAssignableFrom(clazz)) {
			return 123.0D;
		}
		if (Float.class.isAssignableFrom(clazz)) {
			return 123f;
		}
		if (String.class.isAssignableFrom(clazz)) {
			return "123";
		}
		if (JSONArray.class.isAssignableFrom(clazz)) {
			return new JSONArray();
		}
		if (JSONObject.class.isAssignableFrom(clazz)) {
			return new JSONObject();
		}
		if (LogicalTimestamp.class.isAssignableFrom(clazz)) {
			return new LogicalTimestamp();
		}
		throw new IllegalStateException("Unsuported class " + clazz + ".");
	}
	
	@Test
	public void testFromValueWithNull() {
		assertEquals(ConType.NULL, ConType.fromValue(null));
	}
	
	@Test
	public void testFromValueWithUndefined() {
		assertEquals(ConType.UNDEFINED, ConType.fromValue(new String[] {}));
	}
}
