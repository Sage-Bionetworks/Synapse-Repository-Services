package org.sagebionetworks.repo.manager.grid.row.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class ArrayTranslatorTest {

	@Test
	public void testTranslate() {
		// call under test
		ConValue con = new ArrayTranslator().translate("[1,2,3]");
		assertNotNull(con);
		assertEquals(ConType.JSON_ARRAY, con.getType());
		assertEquals(new JSONArray("[1,2,3]").toString(), con.getValue().toString());

	}
}
