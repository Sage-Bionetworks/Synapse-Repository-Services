package org.sagebionetworks.repo.manager.schema;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.schema.JsonSchema;

public interface AnnotationsTranslator {

	/**
	 * Create a new JSONObject containing all of the data from the given Entity and
	 * its annotations.
	 * 
	 * @param entity
	 * @param annotations
	 * @param schema
	 * @return
	 */
	JSONObject writeToJsonObject(Entity entity, Annotations annotations, JsonSchema schema);

	/**
	 * Given a JSONObject containing all data from an Entity, extract only the Annotations.
	 * @param entityClass
	 * @param jsonObject
	 * @return
	 */
	Annotations readFromJsonObject(Class<? extends Entity> entityClass, JSONObject jsonObject);

}
