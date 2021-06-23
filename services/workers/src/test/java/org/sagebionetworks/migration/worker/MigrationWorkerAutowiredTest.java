package org.sagebionetworks.migration.worker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationWorkerAutowiredTest {

	public static final int MAX_WAIT_MS = 1000 * 60;

	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	SemaphoreManager semphoreManager;
	@Autowired
	UserManager userManager;
	@Autowired
	StackStatusDao stackStatusDao;

	private UserInfo adminUserInfo;

	@BeforeEach
	public void before() throws NotFoundException {
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		asynchJobStatusManager.emptyAllQueues();
		// Get the admin user
		adminUserInfo = userManager
				.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER
						.getPrincipalId());
		
		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_ONLY);
		stackStatusDao.updateStatus(status);
	}
	
	@AfterEach
	public void after() throws Exception {
		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_WRITE);
		stackStatusDao.updateStatus(status);
	}

	@Test
	public void testRoundtrip() throws Exception {
		AsyncMigrationRangeChecksumRequest req = new AsyncMigrationRangeChecksumRequest();
		req.setMinId(0L);
		req.setMaxId(Long.MAX_VALUE);
		req.setSalt("salt");
		req.setMigrationType(MigrationType.NODE);
		AsyncMigrationRequest request = new AsyncMigrationRequest();
		request.setAdminRequest(req);		
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		status = waitForStatus(adminUserInfo, status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		AsynchronousResponseBody resp = status.getResponseBody();
		assertTrue(resp instanceof AsyncMigrationResponse);
		AsyncMigrationResponse aResp = (AsyncMigrationResponse)resp;
		assertNotNull(aResp.getAdminResponse());
		assertTrue(aResp.getAdminResponse() instanceof MigrationRangeChecksum);
		MigrationRangeChecksum checksum = (MigrationRangeChecksum)aResp.getAdminResponse();
		assertTrue(0 == checksum.getMinid());
		assertTrue(Long.MAX_VALUE == checksum.getMaxid());
		assertNotNull(checksum.getChecksum());
	}

	private AsynchronousJobStatus waitForStatus(UserInfo user,
			AsynchronousJobStatus status) throws InterruptedException,
			DatastoreException, NotFoundException {
		long start = System.currentTimeMillis();
		while (!AsynchJobState.COMPLETE.equals(status.getJobState())) {
			assertFalse("Job Failed: " + status.getErrorDetails(),
					AsynchJobState.FAILED.equals(status.getJobState()));
			assertTrue("Timed out waiting for table status",
					(System.currentTimeMillis() - start) < MAX_WAIT_MS);
			Thread.sleep(1000);
			// Get the status again
			status = this.asynchJobStatusManager.getJobStatus(user,
					status.getJobId());
		}
		return status;
	}

}
