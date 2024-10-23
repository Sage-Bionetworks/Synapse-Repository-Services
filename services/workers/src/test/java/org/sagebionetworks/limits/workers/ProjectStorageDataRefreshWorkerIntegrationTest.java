package org.sagebionetworks.limits.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.limits.ProjectStorageLimitManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.limits.ProjectStorageLimitsDao;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationUsage;
import org.sagebionetworks.repo.model.limits.ProjectStorageUsage;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ProjectStorageDataRefreshWorkerIntegrationTest {
	
	private static final long MAX_WAIT = 60 * 1000 * 2;
	
	@Autowired
	private ProjectStorageLimitsDao storageLimitsDao;
	
	@Autowired
	private ProjectStorageLimitManager manager;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private FileHandleObjectHelper fileHelper;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private TableIndexDAO replicationDao;
	
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	
	private UserInfo adminUser;
	
	@BeforeEach
	public void before() {
		storageLimitsDao.truncateAll();
		entityManager.truncateAll();
		fileHelper.truncateAll();
		
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@AfterEach
	public void after() {
		storageLimitsDao.truncateAll();
		entityManager.truncateAll();
		fileHelper.truncateAll();
	}

	@Test
	public void testRefreshProjectData() throws Exception {
		String projectId = entityManager.createEntity(adminUser, new Project().setName("TestProject"), null);
		
		String fileOneId = entityManager.createEntity(adminUser, new FileEntity().setName("fileOne").setParentId(projectId)
				.setDataFileHandleId(fileHelper.create(file -> file.setStorageLocationId(1L).setContentSize(1024L)).getId()), null);
		
		String fileTwoId = entityManager.createEntity(adminUser, new FileEntity().setName("fileTwo").setParentId(projectId)
				.setDataFileHandleId(fileHelper.create(file -> file.setStorageLocationId(1L).setContentSize(2048L)).getId()), null);
		
		// Wait for the data to be up-to-date in the replication index
		asyncHelper.waitForEntityReplication(adminUser, projectId, MAX_WAIT);
		asyncHelper.waitForEntityReplication(adminUser, fileOneId, MAX_WAIT);
		asyncHelper.waitForEntityReplication(adminUser, fileTwoId, MAX_WAIT);
		
		// The initial call returns an empty list of location, but triggers a notification to recompute the information
		assertEquals(Collections.emptyList(), manager.gerProjectStorageUsage(projectId).getLocations());
		
		TimeUtils.waitFor(MAX_WAIT, 1000, () -> {
			return Pair.create(new ProjectStorageUsage()
				.setProjectId(projectId)
				.setLocations(List.of(new ProjectStorageLocationUsage()
					.setStorageLocationId("1")
					.setSumFileBytes(3072L)
					.setIsOverLimit(false)
				)).equals(manager.gerProjectStorageUsage(projectId)), null);
		});
		
		entityManager.deleteEntity(adminUser, fileTwoId);
		
		// Wait fot the replication index to update with the delete
		TimeUtils.waitFor(MAX_WAIT, 1000, () ->{
			return Pair.create(replicationDao.getObjectDataForCurrentVersion(ReplicationType.ENTITY, KeyFactory.stringToKey(fileTwoId)) == null, null);
		});
		
		// Invalidate the cache
		storageLimitsDao.truncateAll();
		
		TimeUtils.waitFor(MAX_WAIT, 1000, () -> {
			return Pair.create(new ProjectStorageUsage()
				.setProjectId(projectId)
				.setLocations(List.of(new ProjectStorageLocationUsage()
					.setStorageLocationId("1")
					.setSumFileBytes(1024L)
					.setIsOverLimit(false)
				)).equals(manager.gerProjectStorageUsage(projectId)), null);
		});
		
	}

}
