package org.sagebionetworks.repo.model.table;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class TableConstants {

	/**
	 * The reserved column name for row id.
	 */
	public static final String ROW_ID = "ROW_ID";
	/**
	 * The reserved column id for row id.
	 */
	public static final Long ROW_ID_ID = -1L;
	/**
	 * The reserved column name for row version.
	 */
	public static final String ROW_VERSION = "ROW_VERSION";
	/**
	 * The reserved column name for row version.
	 */
	public static final Long ROW_VERSION_ID = -2L;

	/**
	 * The set of reserved column names includes things like ROW_ID and
	 * ROW_VERSION
	 */
	private static final Set<String> RESERVED_COLUMNS_NAMES = new HashSet<String>(
			Arrays.asList(ROW_ID, ROW_VERSION));

	/**
	 * The Map of reserved column names like ROW_ID and
	 * ROW_VERSION to pseudo ids
	 */
	private static final Map<String, Long> RESERVED_COLUMNS_IDS = ImmutableMap.<String, Long> builder().put(ROW_ID, ROW_ID_ID)
			.put(ROW_VERSION, ROW_VERSION_ID).build();
	
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
	 * Return the pseudo id if the passed column name is a reserved column name like ROW_ID or
	 * ROW_VERSION?
	 * 
	 * @return
	 */
	public static Long getReservedColumnId(String columnName) {
		if (columnName == null)
			return null;
		// Make it case insensitive.s
		return RESERVED_COLUMNS_IDS.get(columnName.toUpperCase().trim());
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
