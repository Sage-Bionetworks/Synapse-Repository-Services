package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;

import org.sagebionetworks.repo.model.message.ChangeMessage;

public interface ObjectRecordWriter {
	
	/**
	 * builds a list of ObjectRecord from the change message
	 * then write them to a log file and push it to S3
	 * 
	 * @param message
	 * @throws IOException 
	 */
	public void buildAndWriteRecord(ChangeMessage message) throws IOException;
}
