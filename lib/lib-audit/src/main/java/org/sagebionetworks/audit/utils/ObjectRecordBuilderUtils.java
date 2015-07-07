package org.sagebionetworks.audit.utils;

import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class ObjectRecordBuilderUtils {
	
	/**
	 * Build an ObjectRecord from the entity and the changeMessage
	 * 
	 * @param entity
	 * @param timestamp
	 * @return the ObjectRecord that is being built
	 * @throws JSONObjectAdapterException
	 */
	public static ObjectRecord buildObjectRecord(JSONEntity entity, long timestamp) {
		ObjectRecord record = new ObjectRecord();
		record.setTimestamp(timestamp);
		record.setJsonClassName(entity.getClass().getSimpleName().toLowerCase());
		try {
			record.setJsonString(EntityFactory.createJSONStringForEntity(entity));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException();
		}
		return record;
	}
}
