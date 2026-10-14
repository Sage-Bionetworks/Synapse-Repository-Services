package org.sagebionetworks;

import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Test-only {@link SearchQuery} that serializes an extra, unsupported top-level key. Used to verify
 * the request-body boundary guard rejects unknown keys on the OpenSearch search DSL with HTTP 400.
 */
public class FakeSearchQuery extends SearchQuery {

	private static final String NOT_PART_OF_SPECIFICATION = "notPartOfSpecification";
	private String notPartOfSpecification;

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		JSONObjectAdapter superAdapter = super.writeToJSONObject(adapter);
		if (notPartOfSpecification != null) {
			superAdapter.put(NOT_PART_OF_SPECIFICATION, notPartOfSpecification);
		}
		return superAdapter;
	}

	public void setNotPartOfSpecification(String notPartOfSpecification) {
		this.notPartOfSpecification = notPartOfSpecification;
	}
}
