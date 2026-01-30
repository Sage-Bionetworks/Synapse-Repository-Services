package org.sagebionetworks.repo.model.grid.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class ConValueTest {

    @Test
    public void testIntegerConvertedToLongInConstructor() {
        ConValue cv = new ConValue(ConType.LONG, Integer.valueOf(123));
        assertEquals(ConType.LONG, cv.getType());
        // the constructor converts Integer -> Long for LONG type
        assertEquals(Long.valueOf(123), cv.getValue());
    }

    @Test
    public void testIsUndefined() {
        ConValue undefined = new ConValue(ConType.UNDEFINED, null);
        assertTrue(undefined.isUndefined());

        ConValue n = new ConValue(ConType.NULL, null);
        assertFalse(n.isUndefined());

        ConValue str = new ConValue(ConType.STRING, "foo");
        assertFalse(str.isUndefined());
    }

    @Test
    public void testFromCompactInvalidThrows() {
        // length 2 but second element not timestamp array
        JSONArray bad = new JSONArray();
        bad.put(0, 0);
        bad.put(1, 1); // not an array
        assertThrows(IllegalArgumentException.class, () -> ConValue.fromCompact(bad));

        // length other than 1 or 2
        assertThrows(IllegalArgumentException.class, () -> ConValue.fromCompact(new JSONArray("[]")));
        assertThrows(IllegalArgumentException.class, () -> ConValue.fromCompact(new JSONArray("[1,2,3]")));
    }
    
	@Test
	public void testFromString() {
	    // Existing cases - null and strings
	    assertEquals(new ConValue(ConType.NULL, null), ConValue.fromString("null"));
	    assertEquals(new ConValue(ConType.NULL, null), ConValue.fromString(null));
	    assertEquals(new ConValue(ConType.STRING, ""), ConValue.fromString(""));
	    assertEquals(new ConValue(ConType.STRING, "basic"), ConValue.fromString("basic"));
	    assertEquals(new ConValue(ConType.STRING, "basic/complex"), ConValue.fromString("basic/complex"));
	    assertEquals(new ConValue(ConType.STRING, "slash/"), ConValue.fromString("slash/"));
	    assertEquals(new ConValue(ConType.STRING, "First, Last"), ConValue.fromString("First, Last"));
	    assertEquals(new ConValue(ConType.STRING, "123, 456"), ConValue.fromString("123, 456"));
	    assertEquals(new ConValue(ConType.STRING, "true:false"), ConValue.fromString("true:false"));
	    
	    // Numbers - positive, negative, zero, scientific notation
	    assertEquals(new ConValue(ConType.LONG, 1234L), ConValue.fromString("1234"));
	    assertEquals(new ConValue(ConType.LONG, -123L), ConValue.fromString("-123"));
	    assertEquals(new ConValue(ConType.LONG, 0L), ConValue.fromString("0"));
	    assertEquals(new ConValue(ConType.DOUBLE, 3.14), ConValue.fromString("3.14"));
	    assertEquals(new ConValue(ConType.DOUBLE, -3.14), ConValue.fromString("-3.14"));
	    assertEquals(new ConValue(ConType.DOUBLE, 0.0), ConValue.fromString("0.0"));
	    assertEquals(new ConValue(ConType.DOUBLE, 1.23e10), ConValue.fromString("1.23e10"));
	    assertEquals(new ConValue(ConType.DOUBLE, 1E-5), ConValue.fromString("1E-5"));
	    
	    // Booleans
	    assertEquals(new ConValue(ConType.BOOLEAN, true), ConValue.fromString("true"));
	    assertEquals(new ConValue(ConType.BOOLEAN, false), ConValue.fromString("false"));
	    
	    // Whitespace handling - should trim and parse correctly
	    assertEquals(new ConValue(ConType.LONG, 123L), ConValue.fromString("  123  "));
	    assertEquals(new ConValue(ConType.BOOLEAN, true), ConValue.fromString("  true  "));
	    assertEquals(new ConValue(ConType.NULL, null), ConValue.fromString("  null  "));
	    
	    // Arrays and Objects
	    assertEquals(new ConValue(ConType.JSON_ARRAY, new JSONArray("[1,2,3]")), ConValue.fromString("[1,2,3]"));
	    assertEquals(new ConValue(ConType.JSON_ARRAY, new JSONArray("[]")), ConValue.fromString("[]"));
	    assertEquals(new ConValue(ConType.JSON_OBJECT, new JSONObject("{\"a\":true}")), ConValue.fromString("{\"a\":true}"));
	    assertEquals(new ConValue(ConType.JSON_OBJECT, new JSONObject("{}")), ConValue.fromString("{}"));
	    
	    // Nested structures
	    assertEquals(new ConValue(ConType.JSON_OBJECT, new JSONObject("{\"a\":{\"b\":1}}")), ConValue.fromString("{\"a\":{\"b\":1}}"));
	    assertEquals(new ConValue(ConType.JSON_ARRAY, new JSONArray("[[1,2],[3,4]]")), ConValue.fromString("[[1,2],[3,4]]"));
	    assertEquals(new ConValue(ConType.JSON_ARRAY, new JSONArray("[{\"a\":1},2,\"three\"]")), ConValue.fromString("[{\"a\":1},2,\"three\"]"));
	    
	    // Strings that look like JSON but have extra content (not valid JSON)
	    assertEquals(new ConValue(ConType.STRING, "123 extra"), ConValue.fromString("123 extra"));
	    assertEquals(new ConValue(ConType.STRING, "true extra"), ConValue.fromString("true extra"));
	    assertEquals(new ConValue(ConType.STRING, "[1,2] extra"), ConValue.fromString("[1,2] extra"));
	    
	    // Strings containing JSON-like characters (but are actual strings)
	    // Note: For this case the JSONTokener we use will accept this as a valid JSONArray
	    assertEquals(new ConValue(ConType.JSON_ARRAY, new JSONArray("[\"hello\"]")), ConValue.fromString("[hello]"));
	    assertEquals(new ConValue(ConType.STRING, "{world}"), ConValue.fromString("{world}"));
	    
	    // Numbers as strings (quoted)
	    assertEquals(new ConValue(ConType.STRING, "123"), ConValue.fromString("\"123\""));
	    
	    // Unicode and escaped characters
	    assertEquals(new ConValue(ConType.STRING, "hello world"), ConValue.fromString("\"hello\\u0020world\""));
	    assertEquals(new ConValue(ConType.STRING, "line\nbreak"), ConValue.fromString("\"line\\nbreak\""));
	    
	    // Invalid/incomplete JSON - should be treated as strings
	    assertEquals(new ConValue(ConType.STRING, "[1,2"), ConValue.fromString("[1,2"));
	    assertEquals(new ConValue(ConType.STRING, "{\"a\":"), ConValue.fromString("{\"a\":"));
	    assertEquals(new ConValue(ConType.STRING, "["), ConValue.fromString("["));
	}

    @Test
    public void testEqualsWithJsonStructures() {
        JSONObject o1 = new JSONObject().put("a", 1);
        JSONObject o2 = new JSONObject().put("a", 1);
        ConValue c1 = new ConValue(ConType.JSON_OBJECT, o1);
        ConValue c2 = new ConValue(ConType.JSON_OBJECT, o2);
        assertEquals(c1, c2);

        JSONArray a1 = new JSONArray().put(0, 1).put(1, 2);
        JSONArray a2 = new JSONArray().put(0, 1).put(1, 2);
        ConValue ca1 = new ConValue(ConType.JSON_ARRAY, a1);
        ConValue ca2 = new ConValue(ConType.JSON_ARRAY, a2);
        assertEquals(ca1, ca2);

        // different values
        ConValue s1 = new ConValue(ConType.STRING, "x");
        ConValue s2 = new ConValue(ConType.STRING, "y");
        assertNotEquals(s1, s2);
    }

    @Test
    @Disabled("org.json does not properly implement hashCode for JSONObject and JSONArray")
    public void testHashCodeWithJsonStructures() {
        JSONObject o1 = new JSONObject().put("a", 1);
        JSONObject o2 = new JSONObject().put("a", 1);
        ConValue c1 = new ConValue(ConType.JSON_OBJECT, o1);
        ConValue c2 = new ConValue(ConType.JSON_OBJECT, o2);
        assertEquals(c1.hashCode(), c2.hashCode());

        JSONArray a1 = new JSONArray().put(0, 1).put(1, 2);
        JSONArray a2 = new JSONArray().put(0, 1).put(1, 2);
        ConValue ca1 = new ConValue(ConType.JSON_ARRAY, a1);
        ConValue ca2 = new ConValue(ConType.JSON_ARRAY, a2);
        assertEquals(ca1.hashCode(), ca2.hashCode());

        // different values
        ConValue s1 = new ConValue(ConType.STRING, "x");
        ConValue s2 = new ConValue(ConType.STRING, "y");
        assertNotEquals(s1.hashCode(), s2.hashCode());
    }

    enum ConValueTestCase {
        NULL(ConType.NULL, null, "[null]"),
        NULL_WITH_JSON_NULL(ConType.NULL, JSONObject.NULL, "[null]"),
        UNDEFINED(ConType.UNDEFINED, null, "[0,0]"),
        TIMESTAMP(ConType.TIMESTAMP, new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(456L), "[0,[123,456]]"),
        STRING_WITH_NULL_VALUE(ConType.STRING, null, "[null]", new ConValue(ConType.NULL, null)), // null value for STRING becomes NULL type after deserialization
        EMPTY_STRING(ConType.STRING, "", "[\"\"]"),
        STRING(ConType.STRING, "hello", "[\"hello\"]"),
        LONG_ZERO(ConType.LONG, 0L, "[0]"),
        LONG(ConType.LONG, 123L, "[123]"),
        LONG_WITH_INT(ConType.LONG, 4, "[4]"),
        DOUBLE(ConType.DOUBLE, 1.25, "[1.25]"),
        DOUBLE_ZERO(ConType.DOUBLE, 0.0, "[0]"),
        BOOLEAN_TRUE(ConType.BOOLEAN, true, "[true]"),
        BOOLEAN_FALSE(ConType.BOOLEAN, false, "[false]"),
        ARRAY(ConType.JSON_ARRAY, new JSONArray("[1,2,3]"),"[[1,2,3]]"),
        ARRAY_EMPTU(ConType.JSON_ARRAY, new JSONArray("[]"),"[[]]"),
        OBJECT(ConType.JSON_OBJECT, new JSONObject("{\"key\":99}"),"[{\"key\":99}]"),
        OBJECT_EMPTY(ConType.JSON_OBJECT, new JSONObject("{}"),"[{}]");


        ConType type;
        Object value;
        String expectedSerializationValue;
        // Optionally define the expected ConValue after deserialization, if different from the original
        ConValue expectedConValueAfterDeserialize = null;

        ConValueTestCase(ConType type, Object value, String expectedSerializationValue) {
            this.type = type;
            this.value = value;
            this.expectedSerializationValue = expectedSerializationValue;
        }

        ConValueTestCase(ConType type, Object value, String expectedSerializationValue, ConValue expectedConValueAfterDeserialize) {
            this.type = type;
            this.value = value;
            this.expectedSerializationValue = expectedSerializationValue;
            this.expectedConValueAfterDeserialize = expectedConValueAfterDeserialize;
        }
    }

    @ParameterizedTest
    @EnumSource(ConValueTestCase.class)
    public void testGetTypeAndValueFromConTypeRoundTrip(ConValueTestCase testCase) {
        // create a representative value for each supported class
        ConValue v = new ConValue(testCase.type, testCase.value);

        JSONArray compact = v.toCompact();
        assertEquals(testCase.expectedSerializationValue, compact.toString());

        ConValue reconstructed = ConValue.fromCompact(compact);
        ConValue expected = v;
        if (testCase.expectedConValueAfterDeserialize != null) {
            expected = testCase.expectedConValueAfterDeserialize;
        }
        assertEquals(expected, reconstructed);
    }


    @ParameterizedTest
    @EnumSource(value = ConType.class, mode = EnumSource.Mode.EXCLUDE, names = { "NULL", "UNDEFINED" })
    public void testGetTypeAndValueFromJavaClassRoundtrip(ConType expectedType) {
        // create a representative value for each supported class
        Object testVal = getTestValue(expectedType);
        ConValue v = new ConValue(expectedType, testVal);
        assertEquals(expectedType, v.getType());
        assertEquals(testVal instanceof Integer && expectedType == ConType.LONG ? Long.valueOf((Integer) testVal) : testVal,
                v.getValue());

        // Test compact serialization round trip is not lossy
        assertEquals(ConValue.fromCompact(v.toCompact()) ,v);
    }


    private Object getTestValue(ConType type) {
        Class<?>[] classes = type.getSupportedClasses();
        if (classes == null || classes.length == 0) {
            return null;
        }
        Class<?> clazz = classes[0];
        if (Boolean.class.isAssignableFrom(clazz)) {
            return Boolean.TRUE;
        }
        if (Long.class.isAssignableFrom(clazz)) {
            return 123L;
        }
        if (Integer.class.isAssignableFrom(clazz)) {
            return 123; // will be converted to Long for LONG type
        }
        if (Short.class.isAssignableFrom(clazz)) {
            return Short.valueOf((short) 1);
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
            return new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
        }
        throw new IllegalStateException("Unsupported class " + clazz + ".");
    }

}
