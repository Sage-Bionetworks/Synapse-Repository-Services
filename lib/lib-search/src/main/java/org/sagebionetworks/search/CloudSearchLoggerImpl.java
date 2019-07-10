package org.sagebionetworks.search;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.ThreadLocalProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudSearchLoggerImpl implements CloudSearchLogger {

	static ThreadLocal<List<CloudSearchDocumentLogRecord>> threadLocalRecordList = ThreadLocalProvider
			.getInstanceWithInitial(CloudSearchDocumentLogRecord.KINESIS_DATA_STREAM_NAME_SUFFIX, ArrayList::new);

	@Autowired
	AwsKinesisFirehoseLogger firehoseLogger;

	@Override
	public CloudSearchDocumentLogRecord startRecordForChangeMessage(ChangeMessage change) {
		CloudSearchDocumentLogRecord record = new CloudSearchDocumentLogRecord().withObjectId(change.getObjectId())
				.withChangeNumber(change.getChangeNumber()).withChangeType(change.getChangeType())
				.withObjectType(change.getObjectType()).withTimestamp(System.currentTimeMillis());
		threadLocalRecordList.get().add(record);
		return record;
	}

	@Override
	public void currentBatchFinshed(final String status) {
		final String batchUUID = UUID.randomUUID().toString();

		// get
		List<CloudSearchDocumentLogRecord> recordList = threadLocalRecordList.get();

		for (CloudSearchDocumentLogRecord record : recordList) {
			// only creates, updates and deletes are part of the batch.
			if (DocumentAction.CREATE_OR_UPDATE.equals(record.getAction())
					|| DocumentAction.DELETE.equals(record.getAction())) {
				// documents that are not ignored will be part of this batch.
				record.withDocumentBatchUpdateStatus(status).withDocumentBatchUUID(batchUUID);
			}
		}
	}

	@Override
	public void pushAllRecordsAndReset() {
		List<CloudSearchDocumentLogRecord> recordList = threadLocalRecordList.get();
		if (!recordList.isEmpty()) {
			// pass a copy of the list
			firehoseLogger.logBatch(CloudSearchDocumentLogRecord.KINESIS_DATA_STREAM_NAME_SUFFIX,
					new ArrayList<CloudSearchDocumentLogRecord>(recordList));
		}
		// reset all local records.
		threadLocalRecordList.get().clear();
	}

}
