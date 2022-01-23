package org.sagebionetworks.table.cluster.columntranslation;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;

/**
 *
 * Metadata about columns used to for SQL query translation
 */
public interface ColumnTranslationReference {
	/**
	 * The type of the column being translated
	 * @return type of the column being translated. never null
	 */
	ColumnType getColumnType();

	/**
	 * The column name as referenced in a user's sql. (e.g. foo, bar, baz)
	 * @return column name as referenced in user sql. never null
	 */
	String getUserQueryColumnName();

	/**
	 * The translated column name that will be used to query the table (e.g. _C123_, _C456_)
	 * @return translated column name that will be used to query the table. never null.
	 */
	String getTranslatedColumnName();
	
	/**
	 * Get the maximum size of the referenced column.
	 * @return
	 */
	Long getMaximumSize();
	
	/**
	 * For list types, get the max list length.
	 * @return Null for non-list
	 */
	Long getMaximumListLength();
	
	FacetType getFacetType();
	
	String getDefaultValues();
}
