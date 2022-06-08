package org.sagebionetworks.repo.manager.schema;

import org.everit.json.schema.Schema;
import org.json.JSONObject;

public interface ValidationListenerProvider {

	/**
	 * Create a new stateful DerivedAnnotationVistor.
	 * 
	 * @param schema
	 * @param subjectJson
	 * @return
	 */
	DerivedAnnotationVisitor createNewVisitor(Schema schema, JSONObject subjectJson);
}
