package org.sagebionetworks.repo.model.dbo.limits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.helper.NodeDaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.limits.ProjectStorageData;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ProjectStorageLimitsDaoImplTest {

	@Autowired
	private ProjectStorageLimitsDaoImpl dao;
	
	@Autowired
	private NodeDaoObjectHelper nodeHelper;
	
	@BeforeEach
	public void before() {
		nodeHelper.truncateAll();
		dao.truncateAll();
	}

	@AfterEach
	public void after() {
		dao.truncateAll();
		nodeHelper.truncateAll();
	}
	
	@Test
	public void testSetAndGetStorageData() throws InterruptedException {
		Long projectOneId = 123L;
		Long projectTwoId = 456L;
		Long projectThreeId = 789L;
				
		// Call under test
		assertEquals(Optional.empty(), dao.getStorageData(projectOneId));
		assertEquals(Optional.empty(), dao.getStorageData(projectTwoId));
		assertEquals(Optional.empty(), dao.getStorageData(projectThreeId));

		List<ProjectStorageData> projectsData = List.of(new ProjectStorageData()
				.setProjectId(projectOneId)
				.setRuntimeMs(1000L)
				.setStorageLocationData(Map.of("1", 1024L, "2", 3072L)),
			new ProjectStorageData()
				.setProjectId(projectTwoId)
				.setRuntimeMs(1024L)
				.setStorageLocationData(Map.of("2", 2048L, "3", 4096L)),
			new ProjectStorageData()
				.setProjectId(projectThreeId)
				.setRuntimeMs(2048L)
				.setStorageLocationData(Collections.emptyMap())
		);
		
		// Call under test
		dao.setStorageData(projectsData);
		
		Thread.sleep(1000);
				
		for (ProjectStorageData expectedData : projectsData) {
			ProjectStorageData fetchedProjectData = dao.getStorageData(expectedData.getProjectId()).orElseThrow();
			
			// Etag and modifiedOn are generated
			assertNotNull(fetchedProjectData.getEtag());
			assertNotNull(fetchedProjectData.getModifiedOn());
			
			expectedData.setEtag(fetchedProjectData.getEtag()).setModifiedOn(fetchedProjectData.getModifiedOn());
			assertEquals(expectedData, fetchedProjectData);
			
			// Storing again should update the etag and updatedOn
			dao.setStorageData(List.of(expectedData));
			
			fetchedProjectData = dao.getStorageData(expectedData.getProjectId()).orElseThrow();
			
			assertNotEquals(expectedData.getEtag(), fetchedProjectData.getEtag());
			assertNotEquals(expectedData.getModifiedOn(), fetchedProjectData.getModifiedOn());
		}
	}
	
	@Test
	public void testIsStorageDataModifiedOnAfter() {
		Long projectId = 123L;
		
		Instant instant = Instant.now().minusSeconds(1);
		
		assertFalse(dao.isStorageDataModifiedOnAfter(projectId, instant));
		
		dao.setStorageData(List.of(new ProjectStorageData().setProjectId(projectId).setRuntimeMs(1024L)));
		
		assertTrue(dao.isStorageDataModifiedOnAfter(projectId, instant));
		
		assertFalse(dao.isStorageDataModifiedOnAfter(projectId, instant.plusSeconds(60)));
	}
	
	@Test
	public void testGetAndSetStorageLimits() {
		Long userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		Long projectOneId = 123L;
		Long projectTwoId = 456L;
		
		// Call under test
		assertEquals(Collections.emptyList(), dao.getStorageLocationLimits(projectOneId));
		assertEquals(Collections.emptyList(), dao.getStorageLocationLimits(projectTwoId));
		
		Long storageLocationIdOne = 1L;
		Long storageLocationIdTwo = 2L;
		
		// Call under test
		assertEquals(Optional.empty(), dao.getStorageLocationLimit(projectOneId, storageLocationIdOne));
		assertEquals(Optional.empty(), dao.getStorageLocationLimit(projectOneId, storageLocationIdTwo));
		
		List<ProjectStorageLocationLimit> limits = List.of(
			new ProjectStorageLocationLimit()
				.setProjectId(KeyFactory.keyToString(projectOneId))
				.setStorageLocationId(storageLocationIdOne.toString())
				.setMaxAllowedFileBytes(1024L),
			new ProjectStorageLocationLimit()
				.setProjectId(KeyFactory.keyToString(projectOneId))
				.setStorageLocationId(storageLocationIdTwo.toString())
				.setMaxAllowedFileBytes(2048L),
			new ProjectStorageLocationLimit()
				.setProjectId(KeyFactory.keyToString(projectTwoId))
				.setStorageLocationId(storageLocationIdTwo.toString())
				.setMaxAllowedFileBytes(3072L)
		);

		limits.forEach(limit -> {

			// Call under test
			ProjectStorageLocationLimit stored = dao.setStorageLocationLimit(userId, limit);
			
			assertEquals(limit, stored);
			
			// Call under test
			assertEquals(Optional.of(limit), dao.getStorageLocationLimit(KeyFactory.stringToKey(limit.getProjectId()), Long.valueOf(limit.getStorageLocationId())));
		});
		
		// Call under test
		assertEquals(Optional.empty(), dao.getStorageLocationLimit(projectTwoId, storageLocationIdOne));
		
		// Call under test
		assertEquals(limits.subList(0, 2), dao.getStorageLocationLimits(projectOneId));
		assertEquals(limits.subList(2, 3), dao.getStorageLocationLimits(projectTwoId));
		
	}
}
