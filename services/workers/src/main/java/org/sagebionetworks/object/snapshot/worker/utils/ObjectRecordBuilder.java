package org.sagebionetworks.object.snapshot.worker.utils;

import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;

public interface ObjectRecordBuilder {
	
	/**
	 * builds an ObjectRecord from the change message
	 * 
	 * @param message
	 * @return the record that was built
	 */
	public ObjectRecord build(ChangeMessage message);
}
