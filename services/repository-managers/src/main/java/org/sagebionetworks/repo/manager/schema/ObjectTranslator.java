package org.sagebionetworks.repo.manager.schema;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.schema.ObjectType;

public interface ObjectTranslator {
	
	/**
	 * Extract the object's ID from the given JSONObject.
	 * @param object
	 * @return
	 */
	public String getObjectId(JSONObject object);
	
	/**
	 * Extract the object's type from the given JSONObject.
	 * 
	 * @param object
	 * @return
	 */
	public ObjectType getObjectType(JSONObject object);
	
	/**
	 * Extract the object's etag from the given JSONObject.
	 * @param object
	 * @return
	 */
	public String getObjectEtag(JSONObject object);

}
