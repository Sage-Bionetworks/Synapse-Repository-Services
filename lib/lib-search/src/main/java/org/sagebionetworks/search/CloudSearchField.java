package org.sagebionetworks.search;

public interface CloudSearchField {
	String getFieldName();
	boolean isSearchable();
	boolean isFaceted();
	boolean isReturned();
}
