package org.sagebionetworks.repo.manager.grid.row.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class JSONTranslatorTest {

	@Test
	public void testTranslateArray() {
		// call under test
		ConValue con = new JSONTranslator().translate("[1,2,3]");
		assertNotNull(con);
		assertEquals(ConType.JSON_ARRAY, con.getType());
		assertEquals(new JSONArray("[1,2,3]").toString(), con.getValue().toString());
	}

	@Test
	public void testTranslateObject() {
		// call under test
		ConValue con = new JSONTranslator().translate("{\"a\":true}");
		assertNotNull(con);
		assertEquals(ConType.JSON_OBJECT, con.getType());
		assertEquals(new JSONObject("{\"a\":true}").toString(), con.getValue().toString());
	}

	@Test
	public void testTranslateNotJSON() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new JSONTranslator().translate("notJSON");
		}).getMessage();
		assertEquals("Expected first char of: '{' or '[' for JSON value", message);
	}
}
