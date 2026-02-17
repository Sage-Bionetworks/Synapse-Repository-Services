package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.schema.ObjectType;

/**
 * A basic wrapper for a JSONObject that can be passed to a JSON Schema validator. This class does not
 * track any other metadata about the subject.
 */
public class JsonObjectSubject implements JsonSubject {

	private final JSONObject json;

	public JsonObjectSubject(JSONObject jsonObject) {
		json = jsonObject;
	}

	@Override
	public String getObjectId() {
		return null;
	}

	@Override
	public ObjectType getObjectType() {
		return null;
	}

	@Override
	public String getObjectEtag() {
		return null;
	}

	@Override
	public JSONObject toJson() {
		return json;
	}

	public String jsonToString() {
		return json != null ? json.toString() : null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(jsonToString());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonObjectSubject other = (JsonObjectSubject) obj;
		return Objects.equals(jsonToString(), other.jsonToString());
	}

}
