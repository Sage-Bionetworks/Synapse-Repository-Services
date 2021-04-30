package org.sagebionetworks;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class FakeJsonSchema extends JsonSchema {
	private static final String NOT_PART_OF_SPECIFICATION = "notPartOfSpecification";
	private String notPartOfSpecification;
	
	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		JSONObjectAdapter superAdapter = super.initializeFromJSONObject(adapter);
		if (superAdapter.has(NOT_PART_OF_SPECIFICATION)) {
			notPartOfSpecification = superAdapter.getString(NOT_PART_OF_SPECIFICATION);
		} else {
			notPartOfSpecification = null;
		}
		return superAdapter;
	}
	
	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		JSONObjectAdapter superAdapter = super.writeToJSONObject(adapter);
		if (notPartOfSpecification != null) {
            superAdapter.put(NOT_PART_OF_SPECIFICATION, notPartOfSpecification);
        }
		return superAdapter;
	}

	public String getNotPartOfSpecification() {
		return notPartOfSpecification;
	}

	public void setNotPartOfSpecification(String notPartOfSpecification) {
		this.notPartOfSpecification = notPartOfSpecification;
	}
	
	
}
