package org.sagebionetworks.repo.model.dbo.dao.table;

import org.sagebionetworks.repo.model.dao.table.ColumnNameProvider;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableExceptionTranslatorImpl implements TableExceptionTranslator {

	private static Pattern PATTERN_TABLE_NAME = Pattern.compile(SQLUtils.TABLE_PREFIX + "[0-9]+");
	private static Pattern PATTERN_COLUMM_ID = Pattern
			.compile(SQLUtils.COLUMN_PREFIX + "[0-9]+" + SQLUtils.COLUMN_POSTFIX);
	private static String UNKNOWN_COLUMN_MESSAGE = "Unknown column";

	@Autowired
	ColumnNameProvider columnNameProvider;

	/**
	 * Attempt to translate the given exception into a human readable error message.
	 * 
	 * @param exception
	 * @param tableId
	 * @param schema
	 * @return
	 */
	@Override
	public RuntimeException translateException(Throwable exception) {
		// attempt to find a SQLException in the stack.
		SQLException sqlException = findSQLException(exception);
		if (sqlException != null) {
			// found a SQLException so we can translate it.
			String originalMessage = sqlException.getMessage();
			String newMessage = replaceColumnIdsAndTableNames(originalMessage);
			newMessage = appendUnquotedKeyWordMessage(newMessage);
			return new IllegalArgumentException(newMessage, exception);
		} else if (exception instanceof RuntimeException) {
			// did not find a SQLException but the exception is already a RuntimeException.
			return (RuntimeException) exception;
		} else {
			// did not find a SQLException and need to wrap the the exception in a
			// RuntimeException
			return new RuntimeException(exception);
		}
	}

	/**
	 * Attempt to find a SQLException in the given exception stack.
	 * 
	 * @param exception
	 * @return Null if no SQLException is found in the stack.
	 */
	public static SQLException findSQLException(Throwable exception) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof SQLException) {
				return (SQLException) cause;
			} else {
				cause = cause.getCause();
			}
		}
		// did not find a SQLException in the stack.
		return null;
	}

	/**
	 * Replace all SQL table references (T123) in the input string with the given
	 * tableId.
	 * 
	 * @param input
	 * @param talbeId
	 * @return
	 */
	public static String replaceAllTableReferences(String input) {
		Matcher matcher = PATTERN_TABLE_NAME.matcher(input);
		// This will contain the new string
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			// The group will be a raw value like: '_C123_'
			String group = matcher.group();
			Long id = Long.parseLong(group.substring(1, group.length()));
			String tableName = KeyFactory.keyToString(id);
			// Replace the ID with the name.
			matcher.appendReplacement(sb, tableName);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * The columnIds in the given input string. For example: '_C123_' will result in
	 * 123L.
	 * 
	 * @param input
	 * @param idToColumnMap
	 * @return
	 */
	public static Set<Long> getColumnIdsFromString(String input) {
		HashSet<Long> results = new HashSet<>();
		Matcher matcher = PATTERN_COLUMM_ID.matcher(input);
		while (matcher.find()) {
			// The group will be a raw value like: '_C123_'
			String group = matcher.group();
			Long id = Long.parseLong(group.substring(2, group.length() - 1));
			results.add(id);
		}
		return results;
	}

	/**
	 * Replace all column id (_C123_) in the given input string with the name of the
	 * column.
	 * 
	 * @param input
	 * @param idToColumnMap
	 * @return
	 */
	public static String replaceAllColumnReferences(String input, Map<Long, String> idToColumnName) {
		Matcher matcher = PATTERN_COLUMM_ID.matcher(input);
		// This will contain the new string
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			// The group will be a raw value like: '_C123_'
			String group = matcher.group();
			Long id = Long.parseLong(group.substring(2, group.length() - 1));
			// match to the column
			String name = idToColumnName.get(id);
			if (name != null) {
				// Replace the ID with the name.
				matcher.appendReplacement(sb, name);
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator#
	 * replaceColumnIdsAndTableNames(java.lang.String)
	 */
	@Override
	public String replaceColumnIdsAndTableNames(String input) {
		input = replaceAllTableReferences(input);
		// Lookup the ColumnIds in the string
		Set<Long> columnIds = getColumnIdsFromString(input);
		// Lookup the name of each column
		Map<Long, String> coumnIdToNameMap = columnNameProvider.getColumnNames(columnIds);
		return replaceAllColumnReferences(input, coumnIdToNameMap);
	}

	/*
	 * PLFM-6392 Add more informative error message for key words that must be quoted
	 */
	private static String appendUnquotedKeyWordMessage(String input) {
		if (input.contains(UNKNOWN_COLUMN_MESSAGE)) {
			return input + TableExceptionTranslator.UNQUOTED_KEYWORDS_ERROR_MESSAGE;
		}
		return input;
	}
}
