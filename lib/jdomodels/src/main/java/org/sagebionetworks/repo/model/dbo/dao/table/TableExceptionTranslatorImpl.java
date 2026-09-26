package org.sagebionetworks.repo.model.dbo.dao.table;

import org.sagebionetworks.repo.model.dao.table.ColumnNameProvider;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TableExceptionTranslatorImpl implements TableExceptionTranslator {

	private static Pattern PATTERN_TABLE_NAME = Pattern.compile(SQLUtils.TABLE_PREFIX + "[0-9]+");
	private static Pattern PATTERN_COLUMM_ID = Pattern
			.compile(SQLUtils.COLUMN_PREFIX + "[0-9]+" + SQLUtils.COLUMN_POSTFIX);
	private static String UNKNOWN_COLUMN_MESSAGE = "Unknown column";
	
	private static final String CHECK_CONSTRAINT_SUFFIX = "' is violated.";
	private static final String CHECK_CONSTRAINT_PREFIX = "Check constraint '";

	static final String REDACTED_VALUE = "[value redacted]";

	/**
	 * MySQL data-value error messages quote the offending cell value (e.g. "Incorrect integer value:
	 * 'Alabama' for column '_C456_' at row 1"). Because a table/view build reads rows across many
	 * benefactors, that value can belong to a source row the querying caller is not authorized to see,
	 * so the value is redacted before it is ever persisted to the table status or surfaced. Each pattern
	 * captures the leading clause (group 1) and matches the quoted data literal that follows it; the
	 * trailing column/key name (schema, not data) is left intact.
	 */
	private static final List<Pattern> VALUE_BEARING_PATTERNS = List.of(
			// 1366 (Incorrect ... value) and 1292 (Truncated incorrect ... value)
			Pattern.compile("(?i)(incorrect\\s+\\w+\\s+value:\\s*)'[^']*'"),
			// 1062 Duplicate entry '<value>' for key '<key>'
			Pattern.compile("(?i)(duplicate entry\\s+)'[^']*'"));

	private final ColumnNameProvider columnNameProvider;
	private final ConnectionFactory connectionFactory;

	public TableExceptionTranslatorImpl(ColumnNameProvider columnNameProvider, ConnectionFactory connectionFactory) {
		this.columnNameProvider = columnNameProvider;
		this.connectionFactory = connectionFactory;
	}

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
			// found a SQLException so we can translate it. Redact any embedded data value first, on the
			// raw message, so a leaked value can never be reinterpreted as a column/table token below.
			String newMessage = redactDataValues(sqlException.getMessage());
			newMessage = replaceConstraintNameWithConstraintClause(newMessage);
			newMessage = replaceColumnIdsAndTableNames(newMessage);
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
	 * Replace any quoted data literal in a known value-bearing MySQL error message with a redaction
	 * placeholder, leaving the surrounding schema references (column/key names) intact. Returns the
	 * input unchanged when it matches no value-bearing pattern.
	 * <p>
	 * Known limitation: a data value that itself contains a single quote is only partially redacted,
	 * since the quoted-literal match stops at the first inner quote.
	 */
	static String redactDataValues(String message) {
		if (message == null) {
			return null;
		}
		String result = message;
		for (Pattern pattern : VALUE_BEARING_PATTERNS) {
			result = pattern.matcher(result).replaceAll("$1'" + Matcher.quoteReplacement(REDACTED_VALUE) + "'");
		}
		return result;
	}

	String replaceConstraintNameWithConstraintClause(String message) {
		Optional<String> constraintName = getConstraintViolationName(message);
		if(constraintName.isPresent()) {
			Optional<String> clause = connectionFactory.getFirstConnection().getConstraintClause(constraintName.get());
			if(clause.isPresent()) {
				return message.replaceAll(constraintName.get(), clause.get());
			}
		}
		return message;
	}

	/**
	 * Attempt to find a SQLException in the given exception stack.
	 * 
	 * @param exception
	 * @return Null if no SQLException is found in the stack.
	 */
	public static SQLException findSQLException(Throwable exception) {
		// ignore exceptions that are already translated.
		if (exception != null && exception.getMessage() != null
				&& exception.getMessage().startsWith(SQLUtils.THE_SIZE_OF_THE_COLUMN)) {
			return null;
		}
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
	
	/**
	 * Extract the constraint name from a raw error message
	 * @param rawMessage
	 * @return
	 */
	public static Optional<String> getConstraintViolationName(String rawMessage){
		int start = rawMessage.indexOf(CHECK_CONSTRAINT_PREFIX);
		if(start > -1) {
			start += CHECK_CONSTRAINT_PREFIX.length();
			int end = rawMessage.indexOf(CHECK_CONSTRAINT_SUFFIX);
			if(end > 0) {
				return Optional.of(rawMessage.substring(start, end));
			}
		}
		return Optional.empty();
	}
}
