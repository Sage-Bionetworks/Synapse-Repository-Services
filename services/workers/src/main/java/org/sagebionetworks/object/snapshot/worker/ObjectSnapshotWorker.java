package org.sagebionetworks.object.snapshot.worker;

import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.object.snapshot.worker.utils.ObjectRecordBuilder;
import org.sagebionetworks.object.snapshot.worker.utils.ObjectRecordBuilderFactory;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker listens to object change messages, takes a snapshot of the object, 
 * writes it to a file, and put the file to S3.
 */
public class ObjectSnapshotWorker implements MessageDrivenRunner {

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
	public void run(ProgressCallback<Message> progressCallback, Message message) throws IOException, JSONObjectAdapterException{
		// Keep this message invisible
		progressCallback.progressMade(message);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		if (changeMessage.getChangeType() == ChangeType.DELETE) {
			// TODO: capture the deleted objects
			return;
		}
		ObjectRecordBuilder objectRecordBuilder = builderFactory.getObjectRecordBuilder(changeMessage.getObjectType());
		ObjectRecord record = objectRecordBuilder.build(changeMessage);
		if (record != null) {
			objectRecordDAO.saveBatch(Arrays.asList(record), record.getJsonClassName());
		}
	}
}
