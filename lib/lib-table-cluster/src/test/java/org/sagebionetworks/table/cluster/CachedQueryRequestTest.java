package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.SelectColumn;

public class CachedQueryRequestTest {

	@Test
	public void testClone() {
		CachedQueryRequest request = new CachedQueryRequest().setExpiresInSec(60).setIncludeEntityEtag(true)
				.setIncludesRowIdAndVersion(false).setOutputSQL("select * from syn123")
				.setParameters(Map.of("key", "value")).setSingleTableId("syn123")
				.setSelectColumns(List.of(new SelectColumn().setName("foo")));

		// call under test
		CachedQueryRequest clone = CachedQueryRequest.clone(request);
		assertEquals(request, clone);

	}
	
	@Test
	public void testCloneEmpty() {
		CachedQueryRequest request = new CachedQueryRequest();

		// call under test
		CachedQueryRequest clone = CachedQueryRequest.clone(request);
		assertEquals(request, clone);

	}

}
