package org.sagebionetworks.repo.model.dbo.asynch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AsynchJobTypeTest {

	@Test
	public void testFindTypeFromRequestClass() throws Exception {
		for (AsynchJobType t: AsynchJobType.values()) {
			AsynchJobType type = AsynchJobType.findTypeFromRequestClass(t.getRequestClass());
			assertEquals(t, type);
		}
	}

}
