package org.sagebionetworks.repo.model.dbo.dao.table;

/**
 * An abstraction for translating low-level exception into 'user-friendly' exceptions.
 * @author John
 *
 */
public interface TableExceptionTranslator {

	/**
	 * Replace all ColumnIds with column names, and replace all tables
	 * names with the syn123 name.
	 * For example, given input = "The column '_C123_' does not exist in 'T456'" where
	 * the name of Column 123='foo', the resulting string would become:
	 * "The column 'foo' does not exist in 'syn456'"
	 * @param input
	 * @return
	 */
	String replaceColumnIdsAndTableNames(String input);

	/**
	 * Attempt to translate the given Exception into a 'user-friendly'
	 * exception.
	 * 
	 * @param exception
	 * @return The translated exception with a 'user-friendly' message.
	 */
	RuntimeException translateException(Throwable exception);

}
