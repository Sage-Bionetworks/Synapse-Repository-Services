package org.sagebionetworks.repo.model.grid.node;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class ConstantUtilsTest {
	
	@Test
	public void testConstantValueToJson() {
		// call under test
		assertEquals("[{\"key\":99}]", ConstantUtils.constantValueToJson(new JSONObject("{\"key\":99}")));
		assertEquals("[123]", ConstantUtils.constantValueToJson(123L));
		assertEquals("[true]", ConstantUtils.constantValueToJson(true));
		assertEquals("[[3,2,1]]", ConstantUtils.constantValueToJson(new JSONArray("[3,2,1]")));
		assertEquals("[]", ConstantUtils.constantValueToJson(null));
	}

}
