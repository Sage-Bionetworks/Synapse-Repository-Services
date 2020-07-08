package org.sagebionetworks.repo.manager.schema;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.schema.ObjectType;

/**
 * Abstraction for a JSON subject to be validated against a JSON schema.
 *
 */
public interface JsonSubject {

	/**
	 * The ID of the subject.
	 * 
	 * @return
	 */
	String getObjectId();

	/**
	 * The type of the subject.
	 * 
	 * @return
	 */
	ObjectType getObjectType();

	/**
	 * The etag of the subject.
	 * 
	 * @return
	 */
	String getObjectEtag();

	/**
	 * Convert the subject to a JSONObject for validation.
	 * 
	 * @return
	 */
	JSONObject toJson();
}
