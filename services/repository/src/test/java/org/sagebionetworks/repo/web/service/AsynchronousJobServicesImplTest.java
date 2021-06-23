package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.springframework.test.util.ReflectionTestUtils;


public class AsynchronousJobServicesImplTest {
	
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private AsyncMigrationRequest mockAdminRequest;
	@Mock
	private AsynchronousRequestBody mockRequest;
	
	AsynchronousJobServicesImpl svc;

	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		svc = new AsynchronousJobServicesImpl();
		ReflectionTestUtils.setField(svc, "userManager", mockUserManager);
		ReflectionTestUtils.setField(svc, "asynchJobStatusManager", mockAsynchJobStatusManager);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test(expected=UnauthorizedException.class)
	public void testStartAdminJobAsRegular() {
		Long userId = 123L;
		UserInfo expectedUser = new UserInfo(false);
		expectedUser.setId(123L);
		when(mockUserManager.getUserInfo(eq(userId))).thenReturn(expectedUser);
		svc.startJob(userId, mockAdminRequest);
	}
	
	@Test
	public void testStartAdminJobAsAdmin() {
		Long userId = 123L;
		UserInfo expectedUser = new UserInfo(true);
		expectedUser.setId(123L);
		when(mockUserManager.getUserInfo(eq(userId))).thenReturn(expectedUser);
		AsynchronousJobStatus expectedStatus = new AsynchronousJobStatus();
		expectedStatus.setJobId("jobId");
		when(mockAsynchJobStatusManager.startJob(eq(expectedUser), eq(mockAdminRequest))).thenReturn(expectedStatus);
		AsynchronousJobStatus status = svc.startJob(userId, mockAdminRequest);
		assertEquals(expectedStatus.getJobId(), status.getJobId());
	}
	
	@Test
	public void testStartRegularJobAsAdmin() {
		Long userId = 123L;
		UserInfo expectedUser = new UserInfo(true);
		expectedUser.setId(123L);
		when(mockUserManager.getUserInfo(eq(userId))).thenReturn(expectedUser);
		AsynchronousJobStatus expectedStatus = new AsynchronousJobStatus();
		expectedStatus.setJobId("jobId");
		when(mockAsynchJobStatusManager.startJob(eq(expectedUser), eq(mockRequest))).thenReturn(expectedStatus);
		AsynchronousJobStatus status = svc.startJob(userId, mockRequest);
		assertEquals(expectedStatus.getJobId(), status.getJobId());
	}
	
	@Test
	public void testStartRegularJobAsRegular() {
		Long userId = 123L;
		UserInfo expectedUser = new UserInfo(false);
		expectedUser.setId(123L);
		when(mockUserManager.getUserInfo(eq(userId))).thenReturn(expectedUser);
		AsynchronousJobStatus expectedStatus = new AsynchronousJobStatus();
		expectedStatus.setJobId("jobId");
		when(mockAsynchJobStatusManager.startJob(eq(expectedUser), eq(mockRequest))).thenReturn(expectedStatus);
		AsynchronousJobStatus status = svc.startJob(userId, mockRequest);
		assertEquals(expectedStatus.getJobId(), status.getJobId());
	}
	

}
