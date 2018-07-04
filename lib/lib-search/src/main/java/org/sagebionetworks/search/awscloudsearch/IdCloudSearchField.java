package org.sagebionetworks.search.awscloudsearch;

/**
 * This is a special case of CloudSearch index field. The unique id of an entry in CloudSearch must exist and can be referenced by "_id"
 */
class IdCloudSearchField implements CloudSearchField{
	public static final String ID_FIELD_NAME = "_id";

	@Override
	public String getFieldName() {
		return ID_FIELD_NAME;
	}

	@Override
	public boolean isSearchable() {
		return true;
	}

	@Override
	public boolean isFaceted() {
		return false;
	}

	@Override
	public boolean isReturned() {
		return true;
	}
}
