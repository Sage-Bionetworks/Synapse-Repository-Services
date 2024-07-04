package org.sagebionetworks.util.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;

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

}
