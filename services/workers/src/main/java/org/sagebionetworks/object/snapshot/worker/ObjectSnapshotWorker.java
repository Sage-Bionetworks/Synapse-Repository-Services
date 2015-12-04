package org.sagebionetworks.object.snapshot.worker;

import java.io.IOException;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.object.snapshot.worker.utils.ObjectRecordWriterFactory;
import org.sagebionetworks.object.snapshot.worker.utils.ObjectRecordWriter;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker listens to object change messages, takes a snapshot of the object, 
 * writes it to a file, and put the file to S3.
 */
public class ObjectSnapshotWorker implements ChangeMessageDrivenRunner {

	@Autowired
	private ObjectRecordWriterFactory writerFactory;
	
	ObjectSnapshotWorker(){
	}

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback, ChangeMessage changeMessage) throws IOException {
		// Keep this message invisible
		progressCallback.progressMade(changeMessage);
		if (changeMessage.getChangeType() == ChangeType.DELETE) {
			// TODO: capture the deleted objects
			return;
		}
		ObjectRecordWriter objectRecordWriter = writerFactory.getObjectRecordWriter(changeMessage.getObjectType());
		objectRecordWriter.buildAndWriteRecord(changeMessage);
	}
}
