package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.dao.files.DBOFilesScannerStatus;
import org.sagebionetworks.repo.model.dbo.dao.files.FilesScannerStatusDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleScannerWorkerIntegrationTest {
	
	private static final long TIMEOUT = 3 * 60 * 1000;
	
	@Autowired
	private FilesScannerStatusDao dao;
	
	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;

	@Autowired
	private DaoObjectHelper<ManagedACTAccessRequirement> managedHelper;
	
	@Autowired
	private AccessRequirementDAO arDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private Scheduler scheduler;
	
	private UserInfo user;
	
	private Trigger dispatcherTrigger;
	
	@BeforeEach
	public void before() throws SchedulerException {
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		arDao.clear();
		fileHandleDao.truncateTable();
		dao.truncateAll();
		
		FileHandle handle = TestUtils.createS3FileHandle(user.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		fileHandleDao.createFile(handle).getId();
		
		// We create at least one association so that we are sure one job need to run at least
		managedHelper.create(ar-> {
			ar.setCreatedBy(user.getId().toString());
			ar.setSubjectIds(Collections.emptyList());
			ar.setDucTemplateFileHandleId(handle.getId());
		});
		
		dispatcherTrigger = scheduler.getTrigger(new TriggerKey("fileHandleAssociationScanDispatcherWorkerTrigger"));
		
		assertNotNull(dispatcherTrigger);
		
	}
	
	@AfterEach
	public void after() {
		arDao.clear();
		fileHandleDao.truncateTable();
		dao.truncateAll();
	}
	
	@Test
	public void testScanning() throws Exception {
		
		// Manually trigger the job since the start time is very long
		scheduler.triggerJob(dispatcherTrigger.getJobKey(), dispatcherTrigger.getJobDataMap());
		
		// First wait for the dispatcher job to trigger
		DBOFilesScannerStatus currentJob = TimeUtils.waitFor(TIMEOUT, 1000L, () -> {
			Optional<DBOFilesScannerStatus> status = dao.getLatest();
			
			return new Pair<>(status.isPresent() && status.get().getJobsStartedCount() > 0, status.orElse(null));
		});
		
		// Now wait for all the dispatched requests to finish
		TimeUtils.waitFor(TIMEOUT, 1000L, () -> {
			DBOFilesScannerStatus status = dao.get(currentJob.getId());
			
			return new Pair<>(status.getJobsCompletedCount() >= status.getJobsStartedCount(), null);
		});
	}

}
