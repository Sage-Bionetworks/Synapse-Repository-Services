package org.sagebionetworks.snapshot.workers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.snapshot.workers.writers.ObjectRecordWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This worker listens to object change messages, takes a snapshot of the objects, 
 * writes them to files, and put the files to S3.
 */
@Service
public class ObjectSnapshotWorker implements BatchChangeMessageDrivenRunner {

	private Map<ObjectType, ObjectRecordWriter> objectSnapshotWriterMap;

	@Autowired
	public ObjectSnapshotWorker(Map<ObjectType, ObjectRecordWriter> objectSnapshotWriterMap){
		this.objectSnapshotWriterMap = objectSnapshotWriterMap;
	}

	@Override
	public void run(ProgressCallback progressCallback, List<ChangeMessage> changeMessages) throws IOException {
		if (changeMessages.isEmpty()) {
			return;
		}
		
		ObjectType objectType = changeMessages.get(0).getObjectType();
		
		// Keep this message invisible
		ObjectRecordWriter objectRecordWriter = objectSnapshotWriterMap.get(objectType);
		
		if (objectRecordWriter == null) {
			throw new IllegalStateException("Object type " + objectType + " not supported yet");
		}
		
		objectRecordWriter.buildAndWriteRecords(progressCallback, changeMessages);
	}

}
