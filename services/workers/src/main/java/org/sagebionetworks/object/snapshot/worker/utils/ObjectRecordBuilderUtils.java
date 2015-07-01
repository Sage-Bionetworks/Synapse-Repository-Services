package org.sagebionetworks.object.snapshot.worker.utils;

import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class ObjectRecordBuilderUtils {
	public static ObjectRecord buildObjectRecord(JSONEntity entity, ChangeMessage changeMessage) {
		ObjectRecord record = new ObjectRecord();
		record.setChangeNumber(changeMessage.getChangeNumber());
		record.setTimestamp(changeMessage.getTimestamp().getTime());
		try {
			record.setObjectType(entity.getClass().getSimpleName().toLowerCase());
			record.setJsonString(EntityFactory.createJSONStringForEntity(entity));
		} catch (JSONObjectAdapterException e) {
			e.printStackTrace();
		}
		return record;
	}
}
