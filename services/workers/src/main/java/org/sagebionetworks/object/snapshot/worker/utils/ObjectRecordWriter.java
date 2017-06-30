package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.message.ChangeMessage;

public interface ObjectRecordWriter {
	
	/**
	 * Builds a list of ObjectRecord from the change messages,
	 * then write them to a log file and push it to S3
	 * @param progressCallback 
	 * @param message
	 * @throws IOException 
	 */
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException;
}
