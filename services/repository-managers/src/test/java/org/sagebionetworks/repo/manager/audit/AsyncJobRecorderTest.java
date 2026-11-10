package org.sagebionetworks.repo.manager.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.audit.AsyncJobOutcome;
import org.sagebionetworks.repo.model.audit.AsyncJobRecord;
import org.sagebionetworks.repo.model.auth.CallersContext;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.web.BelowThresholdException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.springframework.transaction.support.TransactionSynchronization;

@ExtendWith(MockitoExtension.class)
public class AsyncJobRecorderTest {

	@Mock
	private AwsKinesisFirehoseLogger mockFirehoseLogger;
	@Mock
	private Clock mockClock;
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

	private AsynchronousJobStatus status;

	@BeforeEach
	public void before() {
		serializer = new AwsKinesisLogRecordSerializerImpl();
		recorder = new AsyncJobRecorder(mockFirehoseLogger, serializer, mockClock, mockTransactionSynchronization, mockJobStatusDao);
		status = new AsynchronousJobStatus().setJobId("8888").setStartedByUserId(7L)
				.setStartedOn(new Date(1_000L)).setRequestBody(new BulkFileDownloadRequest())
				.setCallersContext(new CallersContext().setSessionId("session-1"));
	}

	@Test
	public void testRecordCompleteWithNoQuery() {
		when(mockClock.currentTimeMillis()).thenReturn(5_000L);

		// call under test
		recorder.recordComplete(status, new UploadToTableResult(), 567L);

		AsyncJobRecord expected = new AsyncJobRecord().setTimestamp(5_000L).setJobId("8888")
				.setJobType(AsynchJobType.BULK_FILE_DOWNLOAD.name())
				.setRequestConcreteType(BulkFileDownloadRequest.class.getName()).setUserId(7L)
				.setSessionId("session-1").setStartedOn(1_000L).setRuntimeMs(567L)
				.setOutcome(AsyncJobOutcome.COMPLETE);
		// The request body of a job that carries no table query is never recorded.
		assertEquals(List.of(expected), sendAndCapture());
	}

	@Test
	public void testRecordCompleteWithQuery() {
		when(mockClock.currentTimeMillis()).thenReturn(5_000L);
		Query query = new Query().setSql("select * from syn123");
		status.setRequestBody(new QueryBundleRequest().setEntityId("syn123").setQuery(query));
		QueryResultBundle response = new QueryResultBundle().setQueryCount(25L).setFacetPostProcessingApplied(true);

		// call under test
		recorder.recordComplete(status, response, 567L);

		AsyncJobRecord expected = new AsyncJobRecord().setTimestamp(5_000L).setJobId("8888")
				.setJobType(AsynchJobType.QUERY.name())
				.setRequestConcreteType(QueryBundleRequest.class.getName()).setUserId(7L)
				.setSessionId("session-1").setEntityId("syn123").setStartedOn(1_000L).setRuntimeMs(567L)
				.setOutcome(AsyncJobOutcome.COMPLETE).setQuery(query).setResultCount(25L)
				.setFacetPostProcessingApplied(true);
		assertEquals(List.of(expected), sendAndCapture());
	}

	@Test
	public void testRecordFailedWithNoQuery() {
		when(mockClock.currentTimeMillis()).thenReturn(4_000L);

		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(status);

		// call under test
		recorder.recordFailed("8888", new BelowThresholdException(20L));

		AsyncJobRecord expected = new AsyncJobRecord().setTimestamp(4_000L).setJobId("8888")
				.setJobType(AsynchJobType.BULK_FILE_DOWNLOAD.name())
				.setRequestConcreteType(BulkFileDownloadRequest.class.getName()).setUserId(7L)
				.setSessionId("session-1").setStartedOn(1_000L).setRuntimeMs(3_000L)
				.setOutcome(AsyncJobOutcome.FAILED).setExceptionClass(BelowThresholdException.class.getName());
		assertEquals(List.of(expected), sendAndCapture());
	}

	@Test
	public void testRecordFailedWithRecordFailure() {
		// A job with no request body cannot be mapped to a record.
		status.setRequestBody(null);

		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(status);

		// call under test, a failure to record never changes the outcome of the job
		recorder.recordFailed("8888", new IllegalArgumentException("bad request"));

		recorder.timerFired();
		verifyNoInteractions(mockFirehoseLogger);
	}

	@Test
	public void testRecordFailedWithStatusReadFailure() {
		when(mockJobStatusDao.getJobStatus("8888")).thenThrow(new NotFoundException("Job does not exist"));

		// call under test, a job that cannot be read is not recorded, and the read failure is not thrown
		recorder.recordFailed("8888", new IllegalArgumentException("bad request"));

		recorder.timerFired();
		verifyNoInteractions(mockFirehoseLogger);
	}

	@Test
	public void testRecordFailedWithMalformedNextPageToken() {
		when(mockClock.currentTimeMillis()).thenReturn(4_000L);
		status.setRequestBody(new QueryNextPageToken().setEntityId("syn123").setToken("not a next page token"));

		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(status);

		// call under test
		recorder.recordFailed("8888", new IllegalArgumentException("Not a valid next page token"));

		// A query the job could not read is left out, but the failed job is still recorded.
		AsyncJobRecord expected = new AsyncJobRecord().setTimestamp(4_000L).setJobId("8888")
				.setJobType(AsynchJobType.QUERY_NEXT_PAGE.name())
				.setRequestConcreteType(QueryNextPageToken.class.getName()).setUserId(7L)
				.setSessionId("session-1").setEntityId("syn123").setStartedOn(1_000L).setRuntimeMs(3_000L)
				.setOutcome(AsyncJobOutcome.FAILED).setExceptionClass(IllegalArgumentException.class.getName());
		assertEquals(List.of(expected), sendAndCapture());
	}

	@Test
	public void testRecordCompleteWithTransaction() {
		when(mockTransactionSynchronization.isActualTransactionActive()).thenReturn(true);
		when(mockClock.currentTimeMillis()).thenReturn(5_000L);

		// call under test
		recorder.recordComplete(status, new UploadToTableResult(), 567L);

		// Nothing is queued until the job's transaction commits.
		recorder.timerFired();
		verifyNoInteractions(mockFirehoseLogger);
		verify(mockTransactionSynchronization).registerSynchronization(synchronizationCaptor.capture());

		synchronizationCaptor.getValue().afterCommit();

		AsyncJobRecord expected = new AsyncJobRecord().setTimestamp(5_000L).setJobId("8888")
				.setJobType(AsynchJobType.BULK_FILE_DOWNLOAD.name())
				.setRequestConcreteType(BulkFileDownloadRequest.class.getName()).setUserId(7L)
				.setSessionId("session-1").setStartedOn(1_000L).setRuntimeMs(567L)
				.setOutcome(AsyncJobOutcome.COMPLETE);
		assertEquals(List.of(expected), sendAndCapture());
	}

	@Test
	public void testRecordCompleteWithRolledBackTransaction() {
		when(mockTransactionSynchronization.isActualTransactionActive()).thenReturn(true);

		// call under test
		recorder.recordComplete(status, new UploadToTableResult(), 567L);

		// The transaction never commits, so afterCommit never runs and nothing is sent.
		verify(mockTransactionSynchronization).registerSynchronization(synchronizationCaptor.capture());
		recorder.timerFired();
		verifyNoInteractions(mockFirehoseLogger);
	}

	@Test
	public void testRecordFailedWithTransaction() {
		when(mockTransactionSynchronization.isActualTransactionActive()).thenReturn(true);
		when(mockClock.currentTimeMillis()).thenReturn(4_000L);
		when(mockJobStatusDao.getJobStatus("8888")).thenReturn(status);

		// call under test
		recorder.recordFailed("8888", new BelowThresholdException(20L));

		// The job is read only after the commit, so the read sees the committed failure.
		verify(mockJobStatusDao, never()).getJobStatus(any());
		verify(mockTransactionSynchronization).registerSynchronization(synchronizationCaptor.capture());

		synchronizationCaptor.getValue().afterCommit();

		verify(mockJobStatusDao).getJobStatus("8888");
		AsyncJobRecord expected = new AsyncJobRecord().setTimestamp(4_000L).setJobId("8888")
				.setJobType(AsynchJobType.BULK_FILE_DOWNLOAD.name())
				.setRequestConcreteType(BulkFileDownloadRequest.class.getName()).setUserId(7L)
				.setSessionId("session-1").setStartedOn(1_000L).setRuntimeMs(3_000L)
				.setOutcome(AsyncJobOutcome.FAILED).setExceptionClass(BelowThresholdException.class.getName());
		assertEquals(List.of(expected), sendAndCapture());
	}

	@Test
	public void testTimerFiredWithRecord() {
		when(mockClock.currentTimeMillis()).thenReturn(5_000L);
		recorder.recordComplete(status, new UploadToTableResult(), 567L);

		// call under test
		recorder.timerFired();

		AsyncJobRecord expected = new AsyncJobRecord().setTimestamp(5_000L).setJobId("8888")
				.setJobType(AsynchJobType.BULK_FILE_DOWNLOAD.name())
				.setRequestConcreteType(BulkFileDownloadRequest.class.getName()).setUserId(7L)
				.setSessionId("session-1").setStartedOn(1_000L).setRuntimeMs(567L)
				.setOutcome(AsyncJobOutcome.COMPLETE);
		verify(mockFirehoseLogger).logBatch(eq(AsyncJobRecorder.ASYNC_JOB_RECORD_STREAM), recordsCaptor.capture());
		assertEquals(List.of(expected), recordsCaptor.getValue().stream().map(KinesisJsonEntityRecord::getPayload).toList());
		// The envelope is checked by its parts because its equals() is not null-safe, and the stack and
		// instance stay null until the logger fills them in.
		KinesisJsonEntityRecord<AsyncJobRecord> sent = recordsCaptor.getValue().get(0);
		assertEquals(5_000L, sent.getTimestamp());
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
		recorder.recordComplete(status, new UploadToTableResult(), 567L);
		recorder.timerFired();
		status.setJobId("9999");
		recorder.recordComplete(status, new UploadToTableResult(), 567L);

		// call under test
		recorder.timerFired();

		// Each record is sent once, with the batch it was queued for.
		verify(mockFirehoseLogger, times(2)).logBatch(eq(AsyncJobRecorder.ASYNC_JOB_RECORD_STREAM), recordsCaptor.capture());
		assertEquals(List.of("9999"), recordsCaptor.getValue().stream().map(record -> record.getPayload().getJobId()).toList());
	}

	@Test
	public void testTimerFiredWithDeliveryFailure() {
		recorder.recordComplete(status, new UploadToTableResult(), 567L);
		doThrow(new AwsKinesisDeliveryException("failed")).when(mockFirehoseLogger).logBatch(any(), anyList());

		// call under test, delivery is best effort so the failure is not thrown
		recorder.timerFired();

		// The failed batch is not retried on the next timer.
		recorder.timerFired();
		verify(mockFirehoseLogger, times(1)).logBatch(any(), anyList());
	}

	@Test
	public void testToKinesisRecordWithPolymorphicFilter() {
		AsyncJobRecord record = createRecord("select * from syn1");

		// call under test
		KinesisJsonEntityRecord<AsyncJobRecord> result = recorder.toKinesisRecord(record);

		String json = new String(serializer.toBytes(result), StandardCharsets.UTF_8);
		// The delivered JSON identifies the type of each filter.
		assertTrue(json.contains("\"concreteType\":\"" + ColumnSingleValueQueryFilter.class.getName() + "\""), json);
		assertTrue(json.contains("\"outcome\":\"COMPLETE\""), json);
	}

	@Test
	public void testToKinesisRecordWithOversizedRecord() {
		String sql = "select * from syn1 where a in (" + "'x',".repeat(AsyncJobRecorder.MAX_RECORD_BYTES / 4) + "'x')";
		AsyncJobRecord record = createRecord(sql);

		// call under test
		KinesisJsonEntityRecord<AsyncJobRecord> result = recorder.toKinesisRecord(record);

		assertEquals(createTruncatedRecord(sql.substring(0, AsyncJobRecorder.MAX_TRUNCATED_SQL_CHARS)), result.getPayload());
		assertTrue(serializer.toBytes(result).length <= AsyncJobRecorder.MAX_RECORD_BYTES);
	}

	@Test
	public void testToKinesisRecordWithOversizedFilters() {
		AsyncJobRecord record = createRecordWithOversizedFilters("select * from syn1");

		// call under test
		KinesisJsonEntityRecord<AsyncJobRecord> result = recorder.toKinesisRecord(record);

		// The short SQL is kept whole; only the filters that made the record too large are dropped.
		assertEquals(createTruncatedRecord("select * from syn1"), result.getPayload());
		assertTrue(serializer.toBytes(result).length <= AsyncJobRecorder.MAX_RECORD_BYTES);
	}

	@Test
	public void testToKinesisRecordWithOversizedFiltersAndNoSql() {
		// A failed job is recorded too, including one whose request has no SQL.
		AsyncJobRecord record = createRecordWithOversizedFilters(null);

		// call under test
		KinesisJsonEntityRecord<AsyncJobRecord> result = recorder.toKinesisRecord(record);

		assertEquals(createTruncatedRecord(null), result.getPayload());
	}

	@Test
	public void testToKinesisRecordWithRecordUnderLimit() {
		AsyncJobRecord record = createRecord("select * from syn1");

		// call under test
		KinesisJsonEntityRecord<AsyncJobRecord> result = recorder.toKinesisRecord(record);

		assertEquals(createRecord("select * from syn1"), result.getPayload());
	}

	/**
	 * Fire the timer and return the records sent to the delivery stream.
	 */
	private List<AsyncJobRecord> sendAndCapture() {
		recorder.timerFired();
		verify(mockFirehoseLogger).logBatch(eq(AsyncJobRecorder.ASYNC_JOB_RECORD_STREAM), recordsCaptor.capture());
		return recordsCaptor.getValue().stream().map(KinesisJsonEntityRecord::getPayload).toList();
	}

	private static AsyncJobRecord createRecordWithOversizedFilters(String sql) {
		AsyncJobRecord record = createRecord(sql);
		List<String> values = Collections.nCopies(AsyncJobRecorder.MAX_RECORD_BYTES / 4, "x");
		record.getQuery().setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("a")
				.setOperator(ColumnSingleValueFilterOperator.IN).setValues(values)));
		return record;
	}

	/**
	 * The record of {@link #createRecord(String)} as it is sent once it was too large: the given start of the
	 * SQL is kept and the filters are dropped.
	 */
	private static AsyncJobRecord createTruncatedRecord(String keptSql) {
		AsyncJobRecord record = createRecord(keptSql).setTruncated(true);
		record.getQuery().setAdditionalFilters(null);
		return record;
	}

	private static AsyncJobRecord createRecord(String sql) {
		return new AsyncJobRecord().setTimestamp(1L).setJobId("42").setJobType("QUERY").setUserId(7L)
				.setOutcome(AsyncJobOutcome.COMPLETE).setRuntimeMs(5L)
				.setQuery(new Query().setSql(sql).setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter()
						.setColumnName("a").setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("x")))));
	}
}
