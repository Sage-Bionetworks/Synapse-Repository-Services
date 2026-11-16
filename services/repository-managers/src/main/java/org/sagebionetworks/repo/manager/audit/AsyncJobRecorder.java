package org.sagebionetworks.repo.manager.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseConstants;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.kinesis.AwsKinesisLogRecordSerializer;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.audit.AsyncJobRecord;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
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
	 * Leaves room under the Firehose record limit for the stack and instance the logger adds, and for the
	 * newline that separates the records packed into a single Firehose record.
	 */
	static final int MAX_RECORD_BYTES = AwsKinesisFirehoseConstants.RECORD_SIZE_LIMIT - AwsKinesisFirehoseConstants.ONE_KiB;

	private final AwsKinesisFirehoseLogger firehoseLogger;
	private final AwsKinesisLogRecordSerializer recordSerializer;
	private final TransactionSynchronizationProxy transactionSynchronization;
	private final AsynchronousJobStatusDAO asynchJobStatusDao;

	/**
	 * Records are queued on the threads that end jobs and sent together from the timer thread, so a job
	 * never waits on Firehose.
	 */
	private final LinkedBlockingQueue<AsyncJobRecord> recordQueue = new LinkedBlockingQueue<>();

	public AsyncJobRecorder(AwsKinesisFirehoseLogger firehoseLogger, AwsKinesisLogRecordSerializer recordSerializer,
			TransactionSynchronizationProxy transactionSynchronization, AsynchronousJobStatusDAO asynchJobStatusDao) {
		this.firehoseLogger = firehoseLogger;
		this.recordSerializer = recordSerializer;
		this.transactionSynchronization = transactionSynchronization;
		this.asynchJobStatusDao = asynchJobStatusDao;
	}

	/**
	 * Record a job that has completed or failed. Called inside a transaction, the job is read, and its
	 * record created and queued, only once that transaction commits.
	 *
	 * @param jobId the job that ended.
	 */
	public void record(String jobId) {
		// Read without the stack mode check of AsynchJobStatusManager.lookupJobStatus, so a job is recorded
		// in any mode. A failure to read the job is a failure to record, so it is only logged.
		save(() -> createRecord(asynchJobStatusDao.getJobStatus(jobId)));
	}

	/**
	 * When the timer fires, send the current batch to the delivery stream.
	 */
	public void timerFired() {
		List<AsyncJobRecord> batch = new ArrayList<>();
		recordQueue.drainTo(batch);
		List<KinesisJsonEntityRecord<AsyncJobRecord>> kinesisRecords = batch.stream().map(this::toKinesisRecord)
				.filter(this::isUnderSizeLimit).toList();
		if (kinesisRecords.isEmpty()) {
			return;
		}
		try {
			// Delivery is best effort: a batch that fails to send is logged rather than retried.
			firehoseLogger.logBatch(ASYNC_JOB_RECORD_STREAM, kinesisRecords);
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

	AsyncJobRecord createRecord(AsynchronousJobStatus status) {
		long startedOn = status.getStartedOn().getTime();
		long changedOn = status.getChangedOn().getTime();
		return new AsyncJobRecord()
				.setJobId(status.getJobId())
				.setJobType(AsynchJobType.findTypeFromRequestClass(status.getRequestBody().getClass()).name())
				.setJobState(status.getJobState().name())
				.setStartedBy(status.getStartedByUserId())
				.setStartedOn(startedOn)
				.setChangedOn(changedOn)
				// Failing a job does not update its stored runtime, so the runtime is taken from when it ended.
				.setRuntimeMs(changedOn - startedOn)
				.setException(status.getException())
				.setErrorMessage(status.getErrorMessage())
				.setSessionId(status.getCallersContext().getSessionId())
				.setRequestBody(toJson(status.getRequestBody()))
				.setResponseBody(toJson(status.getResponseBody()));
	}

	/**
	 * The bodies are recorded as JSON strings because their shape differs for every job type.
	 */
	private static String toJson(JSONEntity body) {
		if (body == null) {
			return null;
		}
		try {
			return EntityFactory.createJSONStringForEntity(body);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}

	KinesisJsonEntityRecord<AsyncJobRecord> toKinesisRecord(AsyncJobRecord record) {
		// The logger fills in the stack and instance.
		return new KinesisJsonEntityRecord<>(record.getChangedOn(), record, null, null);
	}

	/**
	 * A single record over the Firehose limit fails the delivery of its entire batch, so an oversized record
	 * is left out. Its job is logged to show which jobs outgrow the limit.
	 */
	boolean isUnderSizeLimit(KinesisJsonEntityRecord<AsyncJobRecord> kinesisRecord) {
		int bytes = recordSerializer.toBytes(kinesisRecord).length;
		if (bytes <= MAX_RECORD_BYTES) {
			return true;
		}
		AsyncJobRecord record = kinesisRecord.getPayload();
		log.error("Asynchronous job record of job {} ({}) is {} bytes, over the limit of {} bytes, so it was not sent",
				record.getJobId(), record.getJobType(), bytes, MAX_RECORD_BYTES);
		return false;
	}

}
