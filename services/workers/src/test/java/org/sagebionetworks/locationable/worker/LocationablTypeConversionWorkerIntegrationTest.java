package org.sagebionetworks.locationable.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AsyncLocationableTypeConversionRequest;
import org.sagebionetworks.repo.model.AsyncLocationableTypeConversionResults;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LocationableTypeConversionResult;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class LocationablTypeConversionWorkerIntegrationTest {

	public static final int MAX_WAIT_MS = 1000 * 80;
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	StackConfiguration config;
	@Autowired
	EntityManager entityManager;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	UserManager userManager;
	@Autowired
	SemaphoreManager semphoreManager;
	private UserInfo adminUserInfo;
	private List<String> toConvert;
	private List<String> toDelete = Lists.newArrayList();

	@Before
	public void before() throws Exception {
		toConvert = Lists.newArrayList();
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		asynchJobStatusManager.emptyAllQueues();
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// Create a project
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);
		toDelete.add(project.getId());
		
		int toCreate = 5;

		for(int i=0; i<toCreate; i++){
			Data data = new Data();
			data.setParentId(project.getId());
			data.setName("data"+i);
			data.setNumSamples(new Long(i));
			String dataId = entityManager.createEntity(adminUserInfo, data, null);
			toConvert.add(dataId);
		}
	}

	@After
	public void after() {
		if (config.getTableEnabled()) {
			if (adminUserInfo != null) {
				for (String id : toDelete) {
					try {
						entityManager.deleteEntity(adminUserInfo, id);
					} catch (Exception e) {
					}
				}
			}
		}
	}

	@Test
	public void testConverTypes() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, IOException,
			InterruptedException {
		// We are now ready to start the job
		AsyncLocationableTypeConversionRequest request = new AsyncLocationableTypeConversionRequest();
		request.setLocationableIdsToConvert(toConvert);
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		AsyncLocationableTypeConversionResults results = (AsyncLocationableTypeConversionResults) status.getResponseBody();
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(toConvert.size(), results.getResults().size());
		// should match the order
		for(int i=0; i<toConvert.size(); i++){
			String entityId =toConvert.get(i);
			LocationableTypeConversionResult ltcr = results.getResults().get(i);
			assertNotNull(ltcr);
			assertEquals(entityId, ltcr.getEntityId());
			assertTrue(ltcr.getSuccess());
			assertEquals(Data.class.getName(), ltcr.getOriginalType());
			assertEquals(Folder.class.getName(), ltcr.getNewType());
			assertEquals(""+adminUserInfo.getId(), ltcr.getCreatedBy());
		}
	}
	
	private AsynchronousJobStatus waitForStatus(UserInfo user, AsynchronousJobStatus status) throws InterruptedException, DatastoreException,
			NotFoundException {
		long start = System.currentTimeMillis();
		while (!AsynchJobState.COMPLETE.equals(status.getJobState())) {
			assertFalse("Job Failed: " + status.getErrorDetails(), AsynchJobState.FAILED.equals(status.getJobState()));
			assertTrue("Timed out waiting for table status", (System.currentTimeMillis() - start) < MAX_WAIT_MS);
			Thread.sleep(1000);
			// Get the status again
			status = this.asynchJobStatusManager.getJobStatus(user, status.getJobId());
		}
		return status;
	}
}
