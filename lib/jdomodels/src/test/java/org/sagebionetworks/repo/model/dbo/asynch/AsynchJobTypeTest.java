package org.sagebionetworks.repo.model.dbo.asynch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;

public class AsynchJobTypeTest {

	@Test
	public void testFindTypeFromRequestClass() throws Exception {
		for (AsynchJobType t: AsynchJobType.values()) {
			AsynchJobType type = AsynchJobType.findTypeFromRequestClass(t.getRequestClass());
			assertEquals(t, type);
		}
	}

}
