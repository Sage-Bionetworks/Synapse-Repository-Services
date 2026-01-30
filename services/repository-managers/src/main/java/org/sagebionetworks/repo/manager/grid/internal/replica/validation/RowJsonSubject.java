package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import static org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManagerImpl.gridRowToJsonObject;

import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.schema.ObjectType;

/**
 * Renders the cells of Row as a JSONObject that can be passed to a JSON Schema
 * validator.
 */
public class RowJsonSubject implements JsonSubject {

	private final JSONObject json;

	public RowJsonSubject(RowView rowView) {
		json = rowView.getRowObject().getData().getRowJsonDocument();
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
		RowJsonSubject other = (RowJsonSubject) obj;
		return Objects.equals(jsonToString(), other.jsonToString());
	}

}
