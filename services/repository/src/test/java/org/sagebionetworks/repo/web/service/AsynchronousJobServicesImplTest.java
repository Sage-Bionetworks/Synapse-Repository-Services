package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousAdminRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;

@ExtendWith(MockitoExtension.class)
public class AsynchronousJobServicesImplTest {
	
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private AsynchronousAdminRequestBody mockAdminRequest;
	@Mock
	private AsynchronousRequestBody mockRequest;
	
	@InjectMocks
	private AsynchronousJobServicesImpl svc;

	@Test
	public void testStartAdminJobAsRegular() {
		Long userId = 123L;
		UserInfo expectedUser = new UserInfo(false);
		expectedUser.setId(123L);
		when(mockUserManager.getUserInfo(eq(userId))).thenReturn(expectedUser);
		
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {			
			svc.startJob(userId, mockAdminRequest);
		});
		
		assertEquals("Only an administrator may start this job.", ex.getMessage());
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
