package org.sagebionetworks.doi.worker;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;


@RunWith(MockitoJUnitRunner.class)
public class DoiWorkerTest {
	
	public static final long MAX_WAIT_MS = 1000 * 60;

	private DoiWorker doiWorker;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private DoiManager mockDoiManager;
	@Mock
	private AsynchJobStatusManager mockAsyncMgr;
	@Mock
	private AsynchronousJobStatus mockStatus;


	DoiRequest request;
	Doi requestDoi;
	Doi responseDoi;
	UserInfo adminUser;
	Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	private static final Message message = new Message();

	@Before
	public void before(){
		request = new DoiRequest();
		requestDoi = new Doi();
		request.setDoi(requestDoi);
		adminUser = new UserInfo(true);
		adminUser.setId(adminUserId);
		doiWorker = new DoiWorker();
		ReflectionTestUtils.setField(doiWorker, "asynchJobStatusManager", mockAsyncMgr);
		ReflectionTestUtils.setField(doiWorker, "doiManager", mockDoiManager);
		ReflectionTestUtils.setField(doiWorker, "userManager", mockUserManager);
	}

	@Test
	public void testSuccess() throws Exception {
		when(mockAsyncMgr.lookupJobStatus(message.getBody())).thenReturn(mockStatus);
		when(mockStatus.getRequestBody()).thenReturn(request);
		when(mockStatus.getStartedByUserId()).thenReturn(adminUserId);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(adminUser);
		when(mockDoiManager.createOrUpdateDoi(adminUser, request.getDoi())).thenReturn(responseDoi);
		doiWorker.run(null, message);
		// We can't use Mockito to verify that the correct job status was called because we cannot access the DoiResponse.
		verify(mockAsyncMgr).setComplete(any(String.class), any(DoiResponse.class));
	}


	@Test
	public void testRecoverable() throws Exception {
		when(mockAsyncMgr.lookupJobStatus(message.getBody())).thenReturn(mockStatus);
		when(mockStatus.getRequestBody()).thenReturn(request);
		when(mockStatus.getStartedByUserId()).thenReturn(adminUserId);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(adminUser);
		when(mockDoiManager.createOrUpdateDoi(adminUser, request.getDoi())).thenThrow(new RecoverableMessageException());
		try { // Call under test
			doiWorker.run(null, message);
			fail();
		} catch(RecoverableMessageException e) {
			// As expected.
		}
		verify(mockAsyncMgr, never()).setJobFailed(any(String.class), any(Throwable.class));
		verify(mockAsyncMgr, never()).setComplete(any(String.class), any(DoiResponse.class));
	}

	@Test
	public void testFailure() throws Exception {
		when(mockAsyncMgr.lookupJobStatus(message.getBody())).thenReturn(mockStatus);
		when(mockStatus.getRequestBody()).thenReturn(request);
		when(mockStatus.getStartedByUserId()).thenReturn(adminUserId);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(adminUser);
		when(mockDoiManager.createOrUpdateDoi(adminUser, request.getDoi())).thenThrow(new IllegalArgumentException());
		doiWorker.run(null, message);
		verify(mockAsyncMgr).setJobFailed(any(String.class), any(Throwable.class));
	}
}
