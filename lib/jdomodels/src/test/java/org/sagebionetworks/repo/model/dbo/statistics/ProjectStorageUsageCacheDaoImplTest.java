package org.sagebionetworks.repo.model.dbo.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ProjectStorageUsageCacheDaoImplTest {

	@Autowired
	private ProjectStorageUsageCacheDaoImpl dao;
	
	@BeforeEach
	public void before() {
		dao.truncateAll();
	}

	@AfterEach
	public void after() {
		//dao.truncateAll();
	}
	
	@Test
	public void testSetAndGet() {
		long projectOneId = 123;
		long projectTwoId = 456;
				
		assertEquals(Collections.emptyMap(), dao.getStorageUsageMap(projectOneId));
		assertEquals(Collections.emptyMap(), dao.getStorageUsageMap(projectTwoId));

		Map<String, Long> storageOneLocationMap = Map.of("1", 1024L, "3", 2048L, "2", 4096L);
		Map<String, Long> storageTwoLocationMap = Map.of("1", 4096L);
		
		dao.setStorageUsageMap(projectOneId, storageOneLocationMap);
		dao.setStorageUsageMap(projectTwoId, storageTwoLocationMap);
		
		assertEquals(storageOneLocationMap, dao.getStorageUsageMap(projectOneId));
		assertEquals(storageTwoLocationMap, dao.getStorageUsageMap(projectTwoId));
		
	}
	
	@Test
	public void testIsUpdatedOnAfter() {
		long projectId = 123;
		
		Instant instant = Instant.now();
		
		assertFalse(dao.isUpdatedOnAfter(projectId, instant));
		
		dao.setStorageUsageMap(projectId, Map.of("1", 1024L));
		
		assertTrue(dao.isUpdatedOnAfter(projectId, instant));
		
		assertFalse(dao.isUpdatedOnAfter(projectId, instant.plusSeconds(60)));
	}
}
