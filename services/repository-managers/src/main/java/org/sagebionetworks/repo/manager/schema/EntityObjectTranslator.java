package org.sagebionetworks.repo.manager.schema;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Translate and Entity from a JSONObject representation.
 *
 */
public class EntityObjectTranslator implements ObjectTranslator {

	@Override
	public String getObjectId(JSONObject object) {
		ValidateArgument.required(object, "object");
		return object.getString("id");
	}

	@Override
	public ObjectType getObjectType(JSONObject object) {
		return ObjectType.entity;
	}

	@Override
	public String getObjectEtag(JSONObject object) {
		ValidateArgument.required(object, "object");
		return object.getString("etag");
	}

}
