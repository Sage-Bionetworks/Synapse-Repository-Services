package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleArchivalWorkerIngegrationTest {
	
	public static final Long MAX_WAIT_MS = 1000L * 30L;

	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	private UserInfo adminUser;
	
	@BeforeEach
	public void setup() {
		fileHandleDao.truncateTable();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	@Test
	public void testArchivalRequest() throws AsynchJobFailedException {
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertNotNull(response);
			assertEquals(0L, response.getCount());
		}, MAX_WAIT_MS);
	}

}
