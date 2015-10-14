package org.sagebionetworks.object.snapshot.worker.utils;

import java.util.List;

import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface ObjectRecordBuilder {
	
	/**
	 * builds a list of ObjectRecord from the change message
	 * 
	 * @param message
	 * @return the record that was built
	 * @throws JSONObjectAdapterException 
	 */
	public List<ObjectRecord> build(ChangeMessage message);
}
