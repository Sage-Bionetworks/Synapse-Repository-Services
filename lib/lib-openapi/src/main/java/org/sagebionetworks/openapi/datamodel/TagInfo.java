package org.sagebionetworks.openapi.datamodel;

import java.util.Objects;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Provides information regarding the Controllers in the API.
 * @author lli
 *
 */
public class TagInfo implements JSONEntity {
	private String name;
	private String description;
	
	public String getName() {
		return name;
	}
	
	public TagInfo withName(String name) {
		this.name = name;
		return this;
	}
	
	public String getDescription() {
		return description;
	}
	
	public TagInfo withDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TagInfo other = (TagInfo) obj;
		return Objects.equals(description, other.description) && Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "TagInfo [name=" + name + ", description=" + description + "]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (name == null) {
			throw new IllegalArgumentException("The 'name' field is required.");
		}
		writeTo.put("name", name);
		if (description != null) {
			writeTo.put("description", description);
		}
		return writeTo;
	}
}
