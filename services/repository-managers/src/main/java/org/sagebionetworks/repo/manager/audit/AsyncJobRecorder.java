package org.sagebionetworks.repo.manager.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseConstants;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.kinesis.AwsKinesisLogRecordSerializer;
import org.sagebionetworks.repo.manager.table.TableQueryUtils;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.audit.AsyncJobOutcome;
import org.sagebionetworks.repo.model.audit.AsyncJobRecord;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.repo.model.table.HasEntityId;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.util.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Writes the asynchronous job audit log to S3 through a Kinesis Firehose delivery stream. Jobs end in the
 * workers application, so its timer is scheduled there.
 * <p>
 * Recording never changes the outcome of a job: a failure to record is logged rather than thrown.
 */
@Service
public class AsyncJobRecorder {

	private static final Logger log = LogManager.getLogger(AsyncJobRecorder.class);

	public static final String ASYNC_JOB_RECORD_STREAM = "asyncJobRecords";

	/**
	 * Leaves room under the Firehose record limit for the newline that separates the records packed into
	 * a single Firehose record.
	 */
	static final int MAX_RECORD_BYTES = AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT - AwsKinesisFirehoseConstants.ONE_KiB;
	static final int MAX_TRUNCATED_SQL_CHARS = 10_000;

	private final AwsKinesisFirehoseLogger firehoseLogger;
	private final AwsKinesisLogRecordSerializer recordSerializer;
	private final Clock clock;
	private final TransactionSynchronizationProxy transactionSynchronization;
	private final AsynchronousJobStatusDAO asynchJobStatusDao;

	/**
	 * Records are queued on the threads that end jobs and sent together from the timer thread, so a job
	 * never waits on Firehose.
	 */
	private final LinkedBlockingQueue<AsyncJobRecord> recordQueue = new LinkedBlockingQueue<>();

	public AsyncJobRecorder(AwsKinesisFirehoseLogger firehoseLogger, AwsKinesisLogRecordSerializer recordSerializer, Clock clock,
			TransactionSynchronizationProxy transactionSynchronization, AsynchronousJobStatusDAO asynchJobStatusDao) {
		this.firehoseLogger = firehoseLogger;
		this.recordSerializer = recordSerializer;
		this.clock = clock;
		this.transactionSynchronization = transactionSynchronization;
		this.asynchJobStatusDao = asynchJobStatusDao;
	}

	/**
	 * Called inside a transaction, the record is created and queued only once that transaction commits.
	 *
	 * @param status    the job that completed.
	 * @param response  the job's response.
	 * @param runtimeMs how long the job ran, in milliseconds.
	 */
	public void recordComplete(AsynchronousJobStatus status, AsynchronousResponseBody response, long runtimeMs) {
		save(() -> createRecord(status, AsyncJobOutcome.COMPLETE, response).setRuntimeMs(runtimeMs));
	}

	/**
	 * Called inside a transaction, the job is read, and the record created and queued, only once that
	 * transaction commits.
	 *
	 * @param jobId the job that failed.
	 * @param error the exception that failed the job.
	 */
	public void recordFailed(String jobId, Throwable error) {
		save(() -> {
			// Read without the stack mode check of AsynchJobStatusManager.lookupJobStatus, so a failure is
			// recorded in any mode. A failure to read the job is a failure to record, so it is only logged.
			AsyncJobRecord record = createRecord(asynchJobStatusDao.getJobStatus(jobId), AsyncJobOutcome.FAILED, null)
					.setExceptionClass(error.getClass().getName());
			return record.setRuntimeMs(record.getTimestamp() - record.getStartedOn());
		});
	}

	/**
	 * When the timer fires, send the current batch to the delivery stream.
	 */
	public void timerFired() {
		List<AsyncJobRecord> batch = new ArrayList<>();
		recordQueue.drainTo(batch);
		if (batch.isEmpty()) {
			return;
		}
		try {
			// Delivery is best effort: a batch that fails to send is logged rather than retried.
			firehoseLogger.logBatch(ASYNC_JOB_RECORD_STREAM, batch.stream().map(this::toKinesisRecord).toList());
		} catch (Exception e) {
			log.error("Failed to write a batch of asynchronous job records", e);
		}
	}

	private void save(Supplier<AsyncJobRecord> record) {
		if (transactionSynchronization.isActualTransactionActive()) {
			// A job's outcome is only real once its transaction commits. Queuing after the commit keeps a
			// rolled-back outcome out of the audit log.
			transactionSynchronization.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					queue(record);
				}
			});
		} else {
			queue(record);
		}
	}

	/**
	 * Spring rethrows an exception from afterCommit to the code that committed, so every failure is caught
	 * here to keep recording from affecting the job.
	 */
	private void queue(Supplier<AsyncJobRecord> record) {
		try {
			recordQueue.add(record.get());
		} catch (RuntimeException e) {
			log.error("Failed to record an asynchronous job", e);
		}
	}

	/**
	 * Only the table query of a request is recorded; the request bodies of other job types can hold
	 * large or sensitive content such as table rows, grid edits and agent prompts.
	 */
	AsyncJobRecord createRecord(AsynchronousJobStatus status, AsyncJobOutcome outcome, AsynchronousResponseBody response) {
		AsynchronousRequestBody request = status.getRequestBody();
		AsyncJobRecord record = new AsyncJobRecord()
				.setTimestamp(clock.currentTimeMillis())
				.setJobId(status.getJobId())
				.setJobType(AsynchJobType.findTypeFromRequestClass(request.getClass()).name())
				.setRequestConcreteType(request.getConcreteType())
				.setUserId(status.getStartedByUserId())
				.setSessionId(status.getCallersContext().getSessionId())
				.setEntityId(request instanceof HasEntityId hasEntityId ? hasEntityId.getEntityId() : null)
				.setStartedOn(status.getStartedOn().getTime())
				.setOutcome(outcome)
				.setQuery(getQuery(request));
		if (response instanceof QueryResultBundle bundle) {
			record.setResultCount(bundle.getQueryCount()).setFacetPostProcessingApplied(bundle.getFacetPostProcessingApplied());
		}
		return record;
	}

	/**
	 * The caller supplies the query, so it can be unreadable, e.g. a malformed next page token. A job that
	 * failed on such a query must still be recorded, so the record is kept and only the query is left out.
	 */
	private static Query getQuery(AsynchronousRequestBody request) {
		try {
			return TableQueryUtils.getQueryFromRequestBody(request).orElse(null);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * A single record over the Firehose limit fails the delivery of its entire batch, so the parts of a
	 * query that have no size limit are dropped from an oversized record and the start of its SQL is kept.
	 */
	KinesisJsonEntityRecord<AsyncJobRecord> toKinesisRecord(AsyncJobRecord record) {
		// The logger fills in the stack and instance.
		KinesisJsonEntityRecord<AsyncJobRecord> kinesisRecord = new KinesisJsonEntityRecord<>(record.getTimestamp(), record, null, null);
		if (record.getQuery() != null && recordSerializer.toBytes(kinesisRecord).length > MAX_RECORD_BYTES) {
			// The envelope holds the record itself, not a copy, so shrinking the record shrinks what is sent.
			Query query = record.getQuery();
			query.setSql(StringUtils.left(query.getSql(), MAX_TRUNCATED_SQL_CHARS))
					.setAdditionalFilters(null)
					.setSelectedFacets(null)
					.setSort(null)
					.setCohorts(null);
			record.setTruncated(true);
		}
		return kinesisRecord;
	}

}
