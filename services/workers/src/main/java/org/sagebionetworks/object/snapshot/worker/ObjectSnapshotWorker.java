package org.sagebionetworks.object.snapshot.worker;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.object.snapshot.worker.utils.ObjectRecordBuilder;
import org.sagebionetworks.object.snapshot.worker.utils.ObjectRecordBuilderFactory;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker listens to object change messages, takes a snapshot of the object, 
 * writes it to a file, and put the file to S3.
 */
public class ObjectSnapshotWorker implements ChangeMessageDrivenRunner {

	static private Logger log = LogManager.getLogger(ObjectSnapshotWorker.class);
	@Autowired
	private ObjectRecordDAO objectRecordDAO;
	@Autowired
	private ObjectRecordBuilderFactory builderFactory;
	
	ObjectSnapshotWorker(){
	}

	// for unit test only
	ObjectSnapshotWorker(ObjectRecordDAO objectRecordDao) {
		this.objectRecordDAO = objectRecordDao;
	}

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback, ChangeMessage changeMessage) throws IOException {
		// Keep this message invisible
		progressCallback.progressMade(changeMessage);
		if (changeMessage.getChangeType() == ChangeType.DELETE) {
			// TODO: capture the deleted objects
			return;
		}
		ObjectRecordBuilder objectRecordBuilder = builderFactory.getObjectRecordBuilder(changeMessage.getObjectType());
		List<ObjectRecord> records = objectRecordBuilder.build(changeMessage);
		if (records == null) {
			return;
		}
		for (ObjectRecord record : records) {
			if (records != null) {
				objectRecordDAO.saveBatch(Arrays.asList(record), record.getJsonClassName());
			}
		}
	}
}
