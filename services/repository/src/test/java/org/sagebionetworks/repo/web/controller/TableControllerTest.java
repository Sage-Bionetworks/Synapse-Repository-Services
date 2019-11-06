package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.web.service.AsynchronousJobServices;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.repo.web.service.table.TableServices;
import org.springframework.test.util.ReflectionTestUtils;

public class TableControllerTest {
	
	@Mock
	ServiceProvider serviceProvider;
	@Mock
	TableServices tableService;
	@Mock
	AsynchronousJobServices asynchronousJobServices;
	
	TableController controller;
	
	String tableId;
	Long userId;
	AsynchronousJobStatus jobStatus;

	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		controller = new TableController();
		ReflectionTestUtils.setField(controller, "serviceProvider", serviceProvider);
		when(serviceProvider.getTableServices()).thenReturn(tableService);
		when(serviceProvider.getAsynchronousJobServices()).thenReturn(asynchronousJobServices);
		
		jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobId("999");
		when(asynchronousJobServices.startJob(anyLong(), any(AsynchronousRequestBody.class))).thenReturn(jobStatus);
		
		userId = 7L;
		tableId = "syn123";
	}
	
	@Test
	public void testStartTransaction() throws Exception {
		TableUpdateTransactionRequest request = new TableUpdateTransactionRequest();
		// call under test
		AsyncJobId result = controller.startTableTransactionJob(userId, tableId, request);
		assertNotNull(result);
		assertEquals(jobStatus.getJobId(), result.getToken());
		// the request should be assigned the id.
		ArgumentCaptor<AsynchronousRequestBody> capture = ArgumentCaptor.forClass(AsynchronousRequestBody.class);
		verify(asynchronousJobServices).startJob(anyLong(), capture.capture());
		AsynchronousRequestBody body = capture.getValue();
		assertNotNull(body);
		assertTrue(body instanceof TableUpdateTransactionRequest);
		TableUpdateTransactionRequest passedRequest = (TableUpdateTransactionRequest)body;
		assertEquals(tableId, passedRequest.getEntityId());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testStartTransactionNullEntityId() throws Exception {
		TableUpdateTransactionRequest request = new TableUpdateTransactionRequest();
		tableId = null;
		// call under test
		controller.startTableTransactionJob(userId, tableId, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testStartTransactionNullRequest() throws Exception {
		TableUpdateTransactionRequest request = null;
		// call under test
		controller.startTableTransactionJob(userId, tableId, request);
	}
	
	@Test
	public void testStartAppendRowsJob() throws Exception {
		AppendableRowSetRequest request = new AppendableRowSetRequest();
		// call under test
		AsyncJobId result = controller.startAppendRowsJob(userId, tableId, request);
		assertNotNull(result);
		assertEquals(jobStatus.getJobId(), result.getToken());

		ArgumentCaptor<AsynchronousRequestBody> capture = ArgumentCaptor.forClass(AsynchronousRequestBody.class);
		verify(asynchronousJobServices).startJob(anyLong(), capture.capture());
		AsynchronousRequestBody body = capture.getValue();
		assertNotNull(body);
		assertTrue(body instanceof TableUpdateTransactionRequest);
		TableUpdateTransactionRequest passedRequest = (TableUpdateTransactionRequest)body;
		assertEquals(tableId, passedRequest.getEntityId());
		// the original request should be wrapped in the transaction
		assertNotNull(passedRequest.getChanges());
		assertEquals(1, passedRequest.getChanges().size());
		assertEquals(request, passedRequest.getChanges().get(0));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testStartAppendRowsNullTableId() throws Exception {
		AppendableRowSetRequest request = new AppendableRowSetRequest();
		tableId = null;
		// call under test
		controller.startAppendRowsJob(userId, tableId, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testStartAppendRowsNullRequest() throws Exception {
		AppendableRowSetRequest request = null;
		// call under test
		controller.startAppendRowsJob(userId, tableId, request);
	}

	
	@Test
	public void testCsvUploadAsyncStart() throws Exception {
		UploadToTableRequest request = new UploadToTableRequest();
		// call under test
		AsyncJobId result = controller.csvUploadAsyncStart(userId, tableId, request);
		assertNotNull(result);
		assertEquals(jobStatus.getJobId(), result.getToken());

		ArgumentCaptor<AsynchronousRequestBody> capture = ArgumentCaptor.forClass(AsynchronousRequestBody.class);
		verify(asynchronousJobServices).startJob(anyLong(), capture.capture());
		AsynchronousRequestBody body = capture.getValue();
		assertNotNull(body);
		assertTrue(body instanceof TableUpdateTransactionRequest);
		TableUpdateTransactionRequest passedRequest = (TableUpdateTransactionRequest)body;
		assertEquals(tableId, passedRequest.getEntityId());
		// the original request should be wrapped in the transaction
		assertNotNull(passedRequest.getChanges());
		assertEquals(1, passedRequest.getChanges().size());
		assertEquals(request, passedRequest.getChanges().get(0));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCsvUploadAsyncStartNullTableId() throws Exception {
		UploadToTableRequest request = new UploadToTableRequest();
		tableId = null;
		// call under test
		controller.csvUploadAsyncStart(userId, tableId, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCsvUploadAsyncStartNullRequest() throws Exception {
		UploadToTableRequest request = null;
		// call under test
		controller.csvUploadAsyncStart(userId, tableId, request);
	}
}
