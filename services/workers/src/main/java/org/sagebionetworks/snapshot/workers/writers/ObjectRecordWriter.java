package org.sagebionetworks.snapshot.workers.writers;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.progress.ProgressCallback;

public interface ObjectRecordWriter {
	
	/**
	 * @return The object type this writer supports
	 */
	ObjectType getObjectType();
	
	/**
	 * Builds a list of ObjectRecord from the change messages,
	 * then write them to a log file and push it to S3
	 * @param progressCallback 
	 * @param message
	 * @throws IOException 
	 */
	void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException;
}
