package org.sagebionetworks.repo.manager.schema;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.schema.ObjectType;

/**
 * Basic Entity implementation of JsonSubject.
 *
 */
public class EntityJsonSubject implements JsonSubject {

	String entityId;
	String entityEtag;
	JSONObject json;

	public EntityJsonSubject(Entity entity, JSONObject json) {
		super();
		this.entityId = entity.getId();
		this.entityEtag = entity.getEtag();
		this.json = json;
	}

	@Override
	public String getObjectId() {
		return this.entityId;
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.entity;
	}

	@Override
	public String getObjectEtag() {
		return this.entityEtag;
	}

	@Override
	public JSONObject toJson() {
		return this.json;
	}

}
