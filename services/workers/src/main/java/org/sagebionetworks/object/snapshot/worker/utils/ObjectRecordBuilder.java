package org.sagebionetworks.object.snapshot.worker.utils;

import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface ObjectRecordBuilder {
	
	/**
	 * builds an ObjectRecord from the change message
	 * 
	 * @param message
	 * @return the record that was built
	 * @throws JSONObjectAdapterException 
	 */
	public ObjectRecord build(ChangeMessage message);
}
