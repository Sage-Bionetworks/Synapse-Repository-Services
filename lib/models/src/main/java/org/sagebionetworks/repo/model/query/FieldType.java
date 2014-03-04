package org.sagebionetworks.repo.model.query;

/**
 * Each field requested in a query will belong to one of these types.
 * The type will determine how the query gets written.
 * 
 * @author jmhill
 *
 */
public enum FieldType {
	PRIMARY_FIELD,
	STRING_ATTRIBUTE,
	DATE_ATTRIBUTE,
	DOUBLE_ATTRIBUTE,
	LONG_ATTRIBUTE,
	BLOB_ATTRIBUTE,
	DOES_NOT_EXIST
}