package org.sagebionetworks.repo.manager.grid.row.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class ArrayTranslatorTest {

	@Test
	public void testTranslateWithJsonArray() {
		// call under test
		ConValue con = new ArrayTranslator().translate("[1,2,3]");
		assertNotNull(con);
		assertEquals(ConType.JSON_ARRAY, con.getType());
		assertEquals(new JSONArray("[1,2,3]").toString(), con.getValue().toString());
	}

	@Test
	public void testTranslateWithPlainString() {
		// call under test
		ConValue con = new ArrayTranslator().translate("foo");
		assertNotNull(con);
		assertEquals(ConType.JSON_ARRAY, con.getType());
		assertEquals(new JSONArray().put("foo").toString(), con.getValue().toString());
	}

	@Test
	public void testTranslateWithCommaSeparatedValues() {
		// call under test
		ConValue con = new ArrayTranslator().translate("one, two, three");
		assertNotNull(con);
		assertEquals(ConType.JSON_ARRAY, con.getType());
		assertEquals(new JSONArray().put("one").put("two").put("three").toString(), con.getValue().toString());
	}

	@Test
	public void testTranslateWithCommaSeparatedValuesAndExtraWhitespace() {
		// call under test
		ConValue con = new ArrayTranslator().translate(" a , b , c ");
		assertNotNull(con);
		assertEquals(ConType.JSON_ARRAY, con.getType());
		assertEquals(new JSONArray().put("a").put("b").put("c").toString(), con.getValue().toString());
	}

	@Test
	public void testTranslateWithNumericCommaSeparatedValues() {
		// call under test
		ConValue con = new ArrayTranslator().translate("1, 2, 3");
		assertNotNull(con);
		assertEquals(ConType.JSON_ARRAY, con.getType());
		assertEquals(new JSONArray().put("1").put("2").put("3").toString(), con.getValue().toString());
	}
}
