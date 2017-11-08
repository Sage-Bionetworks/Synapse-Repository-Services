package org.sagebionetworks.table.cluster;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.springframework.jdbc.UncategorizedSQLException;

public class TableExceptionTranslator {
	
	
	/**
	 * Attempt to translate the given exception into a human readable error message.
	 * 
	 * @param exception
	 * @param tableId
	 * @param schema
	 * @return
	 */
	public static RuntimeException translateException(Exception exception, Map<Long, ColumnModel> columnIdToModelMap ) {
		if(exception instanceof UncategorizedSQLException) {
			return translateUncategorizedSQLException((UncategorizedSQLException)exception, columnIdToModelMap);
		}
		// unknown exception type.
		return new RuntimeException(exception);
	}
	
	/**
	 * Translate an UncategorizedSQLException into an IllegalArgumentException without the SQL or error codes.
	 * @param uncategorized
	 * @param columnIdToModelMap
	 * @return
	 */
	public static RuntimeException translateUncategorizedSQLException(UncategorizedSQLException uncategorized, Map<Long, ColumnModel> columnIdToModelMap ) {
		SQLException sqlException = uncategorized.getSQLException();
		String originalMessage = sqlException.getMessage();
		String newMessage = SQLUtils.replaceAllColumnReferences(originalMessage, columnIdToModelMap);
		return new IllegalArgumentException(newMessage, uncategorized);
	}
	

}
