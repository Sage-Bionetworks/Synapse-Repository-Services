package org.sagebionetworks.search.awscloudsearch;

public interface CloudSearchField {

	/**
	 *
	 * @return Name that is used in CloudSearch of the field
	 */
	String getFieldName();

	/**
	 *
	 * @return true if the field can be referenced in a search. false, otherwise.
	 */
	boolean isSearchable();

	/**
	 *
	 * @return true if the field can return facet information about its values. false, otherwise.
	 */
	boolean isFaceted();

	/**
	 *
	 * @return true if the value of the field can be returned in results. false, otherwise.
	 */
	boolean isReturned();
}
