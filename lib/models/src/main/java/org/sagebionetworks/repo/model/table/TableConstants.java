package org.sagebionetworks.repo.model.table;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TableConstants {

	/**
	 * The reserved column name for row id.
	 */
	public static final String ROW_ID = "ROW_ID";
	/**
	 * The reserved column name for row version.
	 */
	public static final String ROW_VERSION = "ROW_VERSION";

	/**
	 * The set of reserved column names includes things like ROW_ID and
	 * ROW_VERSION
	 */
	private static final Set<String> RESERVED_COLUMNS_NAMES = new HashSet<String>(
			Arrays.asList(ROW_ID, ROW_VERSION));

	/**
	 * The column name prefix for extra doubles column.
	 */
	public static final String DOUBLE_PREFIX = "_DBL";

	/**
	 * The set of key words that are defined in SQL.
	 */
	private static final Set<String> KEY_WORDS = new HashSet<String>(
			Arrays.asList("ALL", "AND", "ASC", "AS", "AVG", "BETWEEN", "DATE",
					"COUNT", "DESC", "CURRENT_DATE", "CURRENT_TIME", "DEFAULT",
					"DISTINCT", "ESCAPE", "FALSE", "FROM", "GROUP BY",
					"INTERVAL", "IN", "IS", "LIKE", "LIMIT", "MAX", "MIN",
					"NOT", "NULL", "OFFSET", "OR", "ORDER BY", "SELECT", "SUM",
					"TIME", "TIMESTAMP", "TRUE", "WHERE", "UNKNOWN"));

	/**
	 * Is the passed column name a reserved column name like ROW_ID or
	 * ROW_VERSION?
	 * 
	 * @return
	 */
	public static boolean isReservedColumnName(String columnName) {
		if (columnName == null)
			return false;
		// Make it case insensitive.
		return RESERVED_COLUMNS_NAMES.contains(columnName.toUpperCase().trim());
	}

	/**
	 * Is the passed word a SQL key word?
	 * 
	 * @param word
	 * @return
	 */
	public static boolean isKeyWord(String word) {
		if (word == null)
			return false;
		// Make it case insensitive.
		return KEY_WORDS.contains(word.toUpperCase().trim());
	}
}
