package org.sagebionetworks.repo.model;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;

/**
 * 
 * @author brucehoff
 *
 */
public interface InstanceGenerator<T extends JSONEntity> {
	
	/**
	 * Given a schema, determine the corresponding Java Class
	 * and return an empty instance into which a JSON object
	 * may be deserialized
	 * 
	 * @param schema
	 * @return
	 */
	public T newInstanceForSchema(JSONObjectAdapter schema);
	
	public Class<T> getClazz();

}
