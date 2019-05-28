package org.sagebionetworks.repo.manager.table;

/**
 * At least one of the columns listed in a FacetColumnRequest is not facet-able according to the table's schema.
 */
public class InvalidTableQueryFacetColumnRequestException extends IllegalArgumentException{
	public InvalidTableQueryFacetColumnRequestException(String s) {
		super(s);
	}
}
