package org.sagebionetworks.repo.model.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class DataSourceContextHolderTest {
	
	@AfterEach
	public void after() {
		DataSourceContextHolder.clear();
	}

	@Test
	public void testGetSet() {
		assertNull(DataSourceContextHolder.get());
		// Call under test
		DataSourceContextHolder.set(DataSourceType.REPO_BATCHING);
		assertEquals(DataSourceType.REPO_BATCHING, DataSourceContextHolder.get());
	}
	
	@Test
	public void testSetClear() {
		DataSourceContextHolder.set(DataSourceType.REPO_BATCHING);
		assertEquals(DataSourceType.REPO_BATCHING, DataSourceContextHolder.get());
		// Call under test
		DataSourceContextHolder.clear();
		assertNull(DataSourceContextHolder.get());
	}
	
	@Test
	public void testSetMixed() {
		DataSourceContextHolder.set(DataSourceType.REPO_BATCHING);
		assertEquals(DataSourceType.REPO_BATCHING, DataSourceContextHolder.get());
		assertThrows(IllegalStateException.class, () -> {
			DataSourceContextHolder.set(DataSourceType.REPO);
		});
	}

}
