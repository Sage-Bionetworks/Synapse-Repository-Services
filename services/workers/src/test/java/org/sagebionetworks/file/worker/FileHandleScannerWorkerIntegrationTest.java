package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleScannerWorkerIntegrationTest {
	
	private static final long TIMEOUT = 3 * 60 * 1000;
	
	@Autowired
	private FileHandleAssociationScanDispatcherWorker worker;
	
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
	
	private UserInfo user;
	
	@BeforeEach
	public void before() {
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
		
	}
	
	@AfterEach
	public void after() {
		arDao.clear();
		fileHandleDao.truncateTable();
		//dao.truncateAll();
	}
	
	@Test
	public void testScanning() throws Exception {
		// Manually start the job since the start time is very long
		worker.run(null);
		
		DBOFilesScannerStatus status = dao.getLatest().orElseThrow(IllegalStateException::new);
		
		assertTrue(status.getJobsStartedCount() > 0);
		
		long start = System.currentTimeMillis();
		
		// Wait until the number of completed jobs is the same or greater than the started jobs 
		while (status.getJobsCompletedCount() < status.getJobsStartedCount()) {
			Thread.sleep(1000);
			
			status = dao.get(status.getId());
			
			if (System.currentTimeMillis() - start >= TIMEOUT) {
				throw new IllegalStateException("Timed out while waiting for job");
			}
			
		}
				
	}

}
