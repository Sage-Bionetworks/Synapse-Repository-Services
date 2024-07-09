package org.sagebionetworks.util.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.util.json.translator.IdentityTranslator;
import org.sagebionetworks.util.json.translator.Translator;

public class JavaJSONUtilTest {

	@Test
	public void testRoundTripWithAllFields() {

		AllValidFields one = new AllValidFields().setaBoolean(true).setaByteArray(new byte[] { -1, -2, 3, 4 })
				.setaDate(new Date(12345L)).setaDouble(1.234).setaLong(8888L).setaString("some value")
				.setaTimeStamp(new Timestamp(9876)).setSomeEnum(SomeEnum.b).setJsonEntity(
						new ExampleJSONEntity().setAge(55L).setName("Bob").setIntegerArray(new Integer[] { 4, 5, 6 }));

		AllValidFields two = new AllValidFields().setaBoolean(false).setaByteArray(new byte[] { 123, -123, 5, 6 })
				.setaDate(new Date(4444L)).setaDouble(3.14).setaLong(123123123123L).setaString("again")
				.setaTimeStamp(new Timestamp(1234567890)).setSomeEnum(SomeEnum.c);

		AllValidFields three = new AllValidFields().setaString("all else is null");

		AllValidFields four = new AllValidFields();
		List<AllValidFields> list = Arrays.asList(one, two, three, four);

		// call under test
		JSONArray array = JavaJSONUtil.writeToJSON(list);
		assertNotNull(array);
		String json = array.toString(2);
		System.out.println(json);
		JSONArray clone = new JSONArray(json);
		// call under test
		List<AllValidFields> result = JavaJSONUtil.readFromJSON(AllValidFields.class, clone);
		assertEquals(list, result);

	}

	@Test
	public void testWriteToJSONWithNull() {
		List<AllValidFields> list = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			JavaJSONUtil.writeToJSON(list);
		}).getMessage();
		assertEquals("objects is required.", message);
	}

	@Test
	public void testWriteToJSONWithNullTranslators() {
		List<Translator<?, ?>> translators = null;
		AllValidFields toWrite = new AllValidFields();
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			JavaJSONUtil.writeToJSON(translators, toWrite);
		}).getMessage();
		assertEquals("translators is required.", message);
	}

	@Test
	public void testWriteToJSONWithNullToWrite() {
		List<Translator<?, ?>> translators = Arrays.asList(new IdentityTranslator<>(Double.class));
		AllValidFields toWrite = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			JavaJSONUtil.writeToJSON(translators, toWrite);
		}).getMessage();
		assertEquals("object is required.", message);
	}

	@Test
	public void testReadFromJSONWithNullType() {
		Class<?> type = null;
		JSONArray array = new JSONArray();
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			JavaJSONUtil.readFromJSON(type, array);
		}).getMessage();
		assertEquals("clazz is required.", message);
	}

	@Test
	public void testReadFromJSONWithNullArray() {
		Class<?> type = AllValidFields.class;
		JSONArray array = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			JavaJSONUtil.readFromJSON(type, array);
		}).getMessage();
		assertEquals("array is required.", message);
	}

	@Test
	public void testReadFromJSONArrayWithNonJSONObject() {
		JSONArray array = new JSONArray();
		array.put(false);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			JavaJSONUtil.readFromJSON(Boolean.class, array);
		}).getMessage();
		assertEquals("Expected JSONObjects but found: java.lang.Boolean", message);
	}
	
	@Test
	public void testFindTranslator() {
		List<Translator<?, ?>> translators = Arrays.asList(new IdentityTranslator<>(Double.class), new IdentityTranslator<>(Long.class));
		Class<?> type = Long.class;
		// call under test
		Translator translator = JavaJSONUtil.findTranslator(translators, type);
		assertNotNull(translator);
		assertEquals(Long.class, translator.getJSONClass());
	}
	
	@Test
	public void testFindTranslatorWithNotFound() {
		List<Translator<?, ?>> translators = Arrays.asList(new IdentityTranslator<>(Double.class), new IdentityTranslator<>(Long.class));
		Class<?> type = Boolean.class;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			JavaJSONUtil.findTranslator(translators, type);
		}).getMessage();
		assertEquals("No translator found for: java.lang.Boolean", message);
	}
	
	@Test
	public void testFindTranslatorNullTranslators() {
		List<Translator<?, ?>> translators = null;
		Class<?> type = Boolean.class;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			JavaJSONUtil.findTranslator(translators, type);
		}).getMessage();
		assertEquals("translators is required.", message);
	}
	
	@Test
	public void testFindTranslatorWithNullType() {
		List<Translator<?, ?>> translators = Arrays.asList(new IdentityTranslator<>(Double.class), new IdentityTranslator<>(Long.class));
		Class<?> type = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			JavaJSONUtil.findTranslator(translators, type);
		}).getMessage();
		assertEquals("type is required.", message);
	}
	
	@Test
	public void testCreateNewInstance() {
		// call under test
		AllValidFields result = (AllValidFields) JavaJSONUtil.createNewInstance(AllValidFields.class);
		assertEquals(new AllValidFields(), result);
	}
	
	@Test
	public void testCreateNewInstanceWithNoConsructor() {
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			JavaJSONUtil.createNewInstance(Double.class);
		}).getMessage();
		assertEquals("A zero argument constructor could not be found for: java.lang.Double", message);
	}

}
