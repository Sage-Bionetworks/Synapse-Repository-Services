package org.sagebionetworks.repo.model.table;

import java.util.List;

/**
 * Interface for an object field that can be translated into a
 * {@link ColumnModel} which can be used to build a default column model for
 * views
 * 
 * @author Marco Marasca
 *
 */
public interface DefaultField {

	/**
	 * @return The column name
	 */
	String getColumnName();

	/**
	 * @return The column type
	 */
	ColumnType getColumnType();

	/**
	 * @return The max size
	 */
	Long getMaximumSize();

	/**
	 * @return The facet type
	 */
	FacetType getFacetType();

	/**
	 * @return The enum values
	 */
	List<String> getEnumValues();

}
