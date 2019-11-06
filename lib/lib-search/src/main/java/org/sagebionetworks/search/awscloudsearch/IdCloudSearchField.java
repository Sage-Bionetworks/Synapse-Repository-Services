package org.sagebionetworks.search.awscloudsearch;

import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;

/**
 * This is a special case of CloudSearch index field.
 * The unique id of an entry in CloudSearch must always exist so there is no need to create an IndexField for it.
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

	@Override
	public IndexFieldType getType() {
		return IndexFieldType.Literal;
	}
}
