package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.web.service.AsynchronousJobServices;
import org.sagebionetworks.repo.web.service.DoiServiceV2;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DoiControllerTest {

	DoiController doiController = new DoiController();

	@Mock
	ServiceProvider mockServiceProvider;

	@Mock
	DoiServiceV2 mockDoiService;

	@Mock
	AsynchronousJobServices mockAsyncServices;

	Long userId = 100L;
	String objectId = "syn123";
	ObjectType objectType = ObjectType.ENTITY;
	Long versionNumber = 3L;
	String url = "http://internet.cool/";
	String jobId = "somejob";
	HttpServletResponse response;


	@Before
	public void before() {
		ReflectionTestUtils.setField(doiController, "serviceProvider", mockServiceProvider);
		response = new MockHttpServletResponse();

		Mockito.when(mockServiceProvider.getAsynchronousJobServices()).thenReturn(mockAsyncServices);
		Mockito.when(mockServiceProvider.getDoiServiceV2()).thenReturn(mockDoiService);
		Mockito.when(mockDoiService.locate(userId, objectId, objectType, versionNumber)).thenReturn(url);
		Mockito.when(mockDoiService.locate(userId, objectId, objectType, null)).thenReturn(url);
	}

	@Test
	public void testGetDoi() throws Exception {
		doiController.getDoiV2(userId, objectId, objectType, versionNumber);
		Mockito.verify(mockServiceProvider).getDoiServiceV2();
		Mockito.verify(mockDoiService).getDoi(userId, objectId, objectType, versionNumber);
	}

	@Test
	public void testGetDoiNoVersion() throws Exception {
		doiController.getDoiV2(userId, objectId, objectType, null);
		Mockito.verify(mockServiceProvider).getDoiServiceV2();
		Mockito.verify(mockDoiService).getDoi(userId, objectId, objectType, null);
	}

	@Test
	public void testGetDoiAssociation() throws Exception {
		doiController.getDoiAssociation(userId, objectId, objectType, versionNumber);
		Mockito.verify(mockServiceProvider).getDoiServiceV2();
		Mockito.verify(mockDoiService).getDoiAssociation(userId, objectId, objectType, versionNumber);
	}

	@Test
	public void testGetDoiAssociationNoVersion() throws Exception {
		doiController.getDoiAssociation(userId, objectId, objectType, null);
		Mockito.verify(mockServiceProvider).getDoiServiceV2();
		Mockito.verify(mockDoiService).getDoiAssociation(userId, objectId, objectType, null);
	}

	@Test
	public void testCreateOrUpdateDoi() {
		DoiRequest req = new DoiRequest();
		AsynchronousJobStatus job = new AsynchronousJobStatus();
		job.setJobId(jobId);

		Mockito.when(mockAsyncServices.startJob(userId, req)).thenReturn(job);

		// Call under test
		AsyncJobId result = doiController.startCreateOrUpdateDoi(userId, req);
		Mockito.verify(mockServiceProvider).getAsynchronousJobServices();
		Mockito.verify(mockAsyncServices).startJob(userId, req);

		assertEquals(jobId, result.getToken());

	}

	@Test
	public void testGetResultsOfCreateOrUpdateDoi() throws Throwable {
		String token = "43210";
		AsynchronousJobStatus job = new AsynchronousJobStatus();
		job.setJobId(jobId);
		job.setResponseBody(new DoiResponse());
		Mockito.when(mockAsyncServices.getJobStatusAndThrow(userId, token)).thenReturn(job);

		// Call under test
		DoiResponse result = doiController.getCreateOrUpdateDoiResults(userId, token);
		Mockito.verify(mockServiceProvider).getAsynchronousJobServices();
		Mockito.verify(mockAsyncServices).getJobStatusAndThrow(userId, token);
		assertEquals(result, job.getResponseBody());
	}

	@Test
	public void testLocate() throws Exception {
		doiController.locate(userId, objectId, objectType, versionNumber, true, response);
		Mockito.verify(mockServiceProvider).getDoiServiceV2();
		Mockito.verify(mockDoiService).locate(userId, objectId, objectType, versionNumber);

		assertEquals(response.getStatus(), HttpStatus.TEMPORARY_REDIRECT.value());
	}

	@Test
	public void testLocateNoVersion() throws Exception {
		doiController.locate(userId, objectId, objectType,null, true ,response);
		Mockito.verify(mockServiceProvider).getDoiServiceV2();
		Mockito.verify(mockDoiService).locate(userId, objectId, objectType, null);

		assertEquals(response.getStatus(), HttpStatus.TEMPORARY_REDIRECT.value());
	}

	@Test
	public void testLocateNoRedirect()throws Exception {
		doiController.locate(userId, objectId, objectType, versionNumber, false, response);
		Mockito.verify(mockServiceProvider).getDoiServiceV2();
		Mockito.verify(mockDoiService).locate(userId, objectId, objectType, versionNumber);
		assertEquals(response.getStatus(), HttpStatus.OK.value());
	}

}
