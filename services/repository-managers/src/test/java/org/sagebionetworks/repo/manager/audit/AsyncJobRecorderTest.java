package org.sagebionetworks.repo.manager.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.kinesis.AwsKinesisDeliveryException;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.kinesis.AwsKinesisLogRecordSerializer;
import org.sagebionetworks.kinesis.AwsKinesisLogRecordSerializerImpl;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.audit.AsyncJobRecord;
import org.sagebionetworks.repo.model.auth.CallersContext;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.web.BelowThresholdException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.transaction.support.TransactionSynchronization;

@ExtendWith(MockitoExtension.class)
public class AsyncJobRecorderTest {

	@Mock
	private AwsKinesisFirehoseLogger mockFirehoseLogger;
	@Mock
	private TransactionSynchronizationProxy mockTransactionSynchronization;
	@Mock
	private AsynchronousJobStatusDAO mockJobStatusDao;

	@Captor
	private ArgumentCaptor<List<KinesisJsonEntityRecord<AsyncJobRecord>>> recordsCaptor;
	@Captor
	private ArgumentCaptor<TransactionSynchronization> synchronizationCaptor;

	private AwsKinesisLogRecordSerializer serializer;

	private AsyncJobRecorder recorder;

	private QueryBundleRequest request;
	private QueryResultBundle response;

	@BeforeEach
	public void before() {
		serializer = new AwsKinesisLogRecordSerializerImpl();
		recorder = new AsyncJobRecorder(mockFirehoseLogger, serializer, mockTransactionSynchronization, mockJobStatusDao);
		request = new QueryBundleRequest().setEntityId("syn123").setPartMask(2L)
				.setQuery(new Query().setSql("select * from syn123").setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter()
						.setColumnName("a").setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("x")))));
		response = new QueryResultBundle().setQueryCount(25L).setFacetPostProcessingApplied(true);
	}

	@Test
	public void testRecordWithCompletedJob() throws Exception {
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(createCompletedStatus("8888", response));

		// call under test
		recorder.record("8888");

		AsyncJobRecord expected = new AsyncJobRecord().setJobId("8888").setJobType(AsynchJobType.QUERY.name())
				.setJobState(AsynchJobState.COMPLETE.name()).setStartedBy(7L).setStartedOn(1_000L).setChangedOn(1_567L)
				.setRuntimeMs(567L).setSessionId("session-1").setRequestBody(EntityFactory.createJSONStringForEntity(request))
				.setResponseBody(EntityFactory.createJSONStringForEntity(response));
		List<AsyncJobRecord> sent = sendAndCapture();
		assertEquals(List.of(expected), sent);
		// The bodies are recorded whole, with the concrete type of each polymorphic part.
		assertEquals(request, EntityFactory.createEntityFromJSONString(sent.get(0).getRequestBody(), AsynchronousRequestBody.class));
		assertEquals(response, EntityFactory.createEntityFromJSONString(sent.get(0).getResponseBody(), AsynchronousResponseBody.class));
	}

	@Test
	public void testRecordWithFailedJob() throws Exception {
		// Failing a job leaves its stored runtime as it was, so the record takes the runtime from when it ended.
		AsynchronousJobStatus status = createStatus("8888").setJobState(AsynchJobState.FAILED).setChangedOn(new Date(4_000L))
				.setRuntimeMS(0L).setException(BelowThresholdException.class.getName())
				.setErrorMessage("The query matched fewer rows than the minimum")
				.setErrorDetails("org.sagebionetworks.repo.web.BelowThresholdException: at ...");
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(status);

		// call under test
		recorder.record("8888");

		// The error message is recorded, but not the stack trace in the error details.
		AsyncJobRecord expected = new AsyncJobRecord().setJobId("8888").setJobType(AsynchJobType.QUERY.name())
				.setJobState(AsynchJobState.FAILED.name()).setStartedBy(7L).setStartedOn(1_000L).setChangedOn(4_000L)
				.setRuntimeMs(3_000L).setException(BelowThresholdException.class.getName())
				.setErrorMessage("The query matched fewer rows than the minimum").setSessionId("session-1")
				.setRequestBody(EntityFactory.createJSONStringForEntity(request));
		assertEquals(List.of(expected), sendAndCapture());
	}

	@Test
	public void testRecordWithTransaction() throws Exception {
		when(mockTransactionSynchronization.isActualTransactionActive()).thenReturn(true);
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(createCompletedStatus("8888", response));

		// call under test
		recorder.record("8888");

		// The job is read only after the commit, so the read sees the committed outcome.
		recorder.timerFired();
		verifyNoInteractions(mockFirehoseLogger);
		verify(mockJobStatusDao, never()).getJobStatus(any());
		verify(mockTransactionSynchronization).registerSynchronization(synchronizationCaptor.capture());

		synchronizationCaptor.getValue().afterCommit();

		verify(mockJobStatusDao).getJobStatus("8888");
		assertEquals(List.of("8888"), sendAndCapture().stream().map(AsyncJobRecord::getJobId).toList());
	}

	@Test
	public void testRecordWithRolledBackTransaction() {
		when(mockTransactionSynchronization.isActualTransactionActive()).thenReturn(true);

		// call under test
		recorder.record("8888");

		// The transaction never commits, so afterCommit never runs and nothing is read or sent.
		verify(mockTransactionSynchronization).registerSynchronization(synchronizationCaptor.capture());
		recorder.timerFired();
		verifyNoInteractions(mockFirehoseLogger, mockJobStatusDao);
	}

	@Test
	public void testRecordWithStatusReadFailure() {
		when(mockJobStatusDao.getJobStatus("8888")).thenThrow(new NotFoundException("Job does not exist"));

		// call under test, a job that cannot be read is not recorded, and the read failure is not thrown
		recorder.record("8888");

		recorder.timerFired();
		verifyNoInteractions(mockFirehoseLogger);
	}

	@Test
	public void testRecordWithRecordFailure() {
		// A job with no request body cannot be mapped to a record.
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(createCompletedStatus("8888", response).setRequestBody(null));

		// call under test, a failure to record never changes the outcome of the job
		recorder.record("8888");

		recorder.timerFired();
		verifyNoInteractions(mockFirehoseLogger);
	}

	@Test
	public void testTimerFiredWithRecord() {
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(createCompletedStatus("8888", response));
		recorder.record("8888");

		// call under test
		recorder.timerFired();

		verify(mockFirehoseLogger).logBatch(eq(AsyncJobRecorder.ASYNC_JOB_RECORD_STREAM), recordsCaptor.capture());
		// The envelope is checked by its parts because its equals() is not null-safe, and the stack and
		// instance stay null until the logger fills them in.
		KinesisJsonEntityRecord<AsyncJobRecord> sent = recordsCaptor.getValue().get(0);
		assertEquals("8888", sent.getPayload().getJobId());
		// The record is dated by when the job ended.
		assertEquals(1_567L, sent.getTimestamp());
		assertNull(sent.getStack());
		assertNull(sent.getInstance());
	}

	@Test
	public void testTimerFiredWithNoRecords() {
		// call under test
		recorder.timerFired();

		verifyNoInteractions(mockFirehoseLogger);
	}

	@Test
	public void testTimerFiredWithRecordsAlreadySent() {
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(createCompletedStatus("8888", response));
		when(mockJobStatusDao.getJobStatus("9999")).thenReturn(createCompletedStatus("9999", response));
		recorder.record("8888");
		recorder.timerFired();
		recorder.record("9999");

		// call under test
		recorder.timerFired();

		// Each record is sent once, with the batch it was queued for.
		verify(mockFirehoseLogger, times(2)).logBatch(eq(AsyncJobRecorder.ASYNC_JOB_RECORD_STREAM), recordsCaptor.capture());
		assertEquals(List.of("9999"), recordsCaptor.getValue().stream().map(record -> record.getPayload().getJobId()).toList());
	}

	@Test
	public void testTimerFiredWithDeliveryFailure() {
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(createCompletedStatus("8888", response));
		recorder.record("8888");
		doThrow(new AwsKinesisDeliveryException("failed")).when(mockFirehoseLogger).logBatch(any(), anyList());

		// call under test, delivery is best effort so the failure is not thrown
		recorder.timerFired();

		// The failed batch is not retried on the next timer.
		recorder.timerFired();
		verify(mockFirehoseLogger, times(1)).logBatch(any(), anyList());
	}

	@Test
	public void testTimerFiredWithOversizedRecord() {
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(createCompletedStatus("8888", createOversizedResponse()));
		when(mockJobStatusDao.getJobStatus("9999")).thenReturn(createCompletedStatus("9999", response));
		recorder.record("8888");
		recorder.record("9999");

		// call under test
		recorder.timerFired();

		// The oversized record is left out, so it cannot fail the delivery of the other records in its batch.
		verify(mockFirehoseLogger).logBatch(eq(AsyncJobRecorder.ASYNC_JOB_RECORD_STREAM), recordsCaptor.capture());
		assertEquals(List.of("9999"), recordsCaptor.getValue().stream().map(record -> record.getPayload().getJobId()).toList());
	}

	@Test
	public void testTimerFiredWithOnlyOversizedRecords() {
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(createCompletedStatus("8888", createOversizedResponse()));
		recorder.record("8888");

		// call under test
		recorder.timerFired();

		verifyNoInteractions(mockFirehoseLogger);
	}

	/**
	 * Fire the timer and return the records sent to the delivery stream.
	 */
	private List<AsyncJobRecord> sendAndCapture() {
		recorder.timerFired();
		verify(mockFirehoseLogger).logBatch(eq(AsyncJobRecorder.ASYNC_JOB_RECORD_STREAM), recordsCaptor.capture());
		return recordsCaptor.getValue().stream().map(KinesisJsonEntityRecord::getPayload).toList();
	}

	private AsynchronousJobStatus createCompletedStatus(String jobId, AsynchronousResponseBody responseBody) {
		return createStatus(jobId).setJobState(AsynchJobState.COMPLETE).setChangedOn(new Date(1_567L)).setRuntimeMS(567L)
				.setResponseBody(responseBody);
	}

	private AsynchronousJobStatus createStatus(String jobId) {
		return new AsynchronousJobStatus().setJobId(jobId).setEtag("etag").setStartedByUserId(7L).setStartedOn(new Date(1_000L))
				.setProgressMessage("Complete").setRequestBody(request)
				.setCallersContext(new CallersContext().setSessionId("session-1"));
	}

	private static QueryResultBundle createOversizedResponse() {
		return new QueryResultBundle().setQueryCount(25L).setCombinedSql("x".repeat(AsyncJobRecorder.MAX_RECORD_BYTES));
	}
}
