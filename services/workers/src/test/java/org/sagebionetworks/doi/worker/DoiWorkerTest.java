package org.sagebionetworks.doi.worker;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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

	@InjectMocks
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
	String jobId;

	private static final Message message = new Message();

	@Before
	public void before(){
		request = new DoiRequest();
		requestDoi = new Doi();
		request.setDoi(requestDoi);
		adminUser = new UserInfo(true);
		adminUser.setId(adminUserId);
		message.setMessageId("messageID");
		jobId="jobId";

		when(mockAsyncMgr.lookupJobStatus(message.getBody())).thenReturn(mockStatus);
		when(mockStatus.getRequestBody()).thenReturn(request);
		when(mockStatus.getStartedByUserId()).thenReturn(adminUserId);
		when(mockStatus.getJobId()).thenReturn(jobId);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(adminUser);
	}

	@Test
	public void testSuccess() throws Exception {
		when(mockDoiManager.createOrUpdateDoi(adminUser, request.getDoi())).thenReturn(responseDoi);
		doiWorker.run(null, message);
		// We can't use Mockito to verify that the correct job status was called because we cannot access the DoiResponse.
		verify(mockAsyncMgr).setComplete(eq(jobId), any(DoiResponse.class));
	}


	@Test
	public void testRecoverable() throws Exception {
		when(mockDoiManager.createOrUpdateDoi(adminUser, request.getDoi())).thenThrow(new RecoverableMessageException());
		try { // Call under test
			doiWorker.run(null, message);
			fail();
		} catch(RecoverableMessageException e) {
			// As expected.
		}
		verify(mockAsyncMgr, never()).setJobFailed(any(), any());
		verify(mockAsyncMgr, never()).setComplete(any(), any());
	}

	@Test
	public void testFailure() throws Exception {
		when(mockDoiManager.createOrUpdateDoi(adminUser, request.getDoi())).thenThrow(new IllegalArgumentException());
		doiWorker.run(null, message);
		verify(mockAsyncMgr).setJobFailed(eq(jobId), any(IllegalArgumentException.class));
	}
}
