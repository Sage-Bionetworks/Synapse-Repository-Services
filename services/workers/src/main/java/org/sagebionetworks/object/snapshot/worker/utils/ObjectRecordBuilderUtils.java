package org.sagebionetworks.object.snapshot.worker.utils;

import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class ObjectRecordBuilderUtils {
	
	/**
	 * Build an ObjectRecord from the entity and the changeMessage
	 * 
	 * @param entity
	 * @param changeMessage
	 * @return the ObjectRecord that is being built
	 * @throws JSONObjectAdapterException
	 */
	public static ObjectRecord buildObjectRecord(JSONEntity entity, ChangeMessage changeMessage) throws JSONObjectAdapterException {
		ObjectRecord record = new ObjectRecord();
		record.setChangeNumber(changeMessage.getChangeNumber());
		record.setTimestamp(changeMessage.getTimestamp().getTime());
		record.setChangeMessageObjectType(changeMessage.getObjectType().toString().toLowerCase());
		record.setJsonClassName(entity.getClass().getSimpleName().toLowerCase());
		record.setJsonString(EntityFactory.createJSONStringForEntity(entity));
		return record;
	}
}
