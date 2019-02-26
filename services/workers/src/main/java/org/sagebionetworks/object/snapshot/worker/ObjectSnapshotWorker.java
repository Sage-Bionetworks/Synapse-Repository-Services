package org.sagebionetworks.object.snapshot.worker;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.object.snapshot.worker.utils.ObjectRecordWriter;
import org.sagebionetworks.object.snapshot.worker.utils.ObjectRecordWriterFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker listens to object change messages, takes a snapshot of the objects, 
 * writes them to files, and put the files to S3.
 */
public class ObjectSnapshotWorker implements BatchChangeMessageDrivenRunner {

	@Autowired
	private ObjectRecordWriterFactory writerFactory;
	
	ObjectSnapshotWorker(){
	}

	@Override
	public void run(ProgressCallback progressCallback, List<ChangeMessage> changeMessages) throws IOException {
		// Keep this message invisible
		ObjectRecordWriter objectRecordWriter = writerFactory.getObjectRecordWriter(changeMessages.get(0).getObjectType());
		objectRecordWriter.buildAndWriteRecords(progressCallback, changeMessages);
	}

}
