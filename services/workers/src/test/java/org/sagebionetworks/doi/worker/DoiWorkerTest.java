package org.sagebionetworks.doi.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;

@ExtendWith(MockitoExtension.class)
public class DoiWorkerTest {

	@Mock
	private DoiManager mockDoiManager;
	@InjectMocks
	private DoiWorker doiWorker;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private DoiRequest mockRequest;
	@Mock
	private Doi mockRequestDoi;
	@Mock
	private Doi mockResponseDoi;
	@Mock
	private UserInfo mockUser;
	private String jobId;

	@BeforeEach
	public void before() {
		jobId = "jobId";
	}

	@Test
	public void testSuccess() throws Exception {
		when(mockRequest.getDoi()).thenReturn(mockRequestDoi);
		when(mockDoiManager.createOrUpdateDoi(any(), any())).thenReturn(mockResponseDoi);

		DoiResponse expected = new DoiResponse().setDoi(mockResponseDoi);

		DoiResponse result = doiWorker.run(jobId, mockUser, mockRequest, mockJobCallback);

		assertEquals(expected, result);

		verify(mockDoiManager).createOrUpdateDoi(mockUser, mockRequestDoi);
	}
}
