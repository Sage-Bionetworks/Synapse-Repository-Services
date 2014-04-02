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
	 * The set of reserved column names includes things like ROW_ID and ROW_VERSION
	 */
	private static final Set<String> RESERVED_COLUMNS_NAMES = new HashSet<String>(Arrays.asList(ROW_ID, ROW_VERSION));
	
	/**
	 * Is the passed column name a reserved column name like ROW_ID or ROW_VERSION?
	 * @return
	 */
	public static boolean isReservedColumnName(String columnName){
		if(columnName == null) return false;
		// Make it case insensitive.
		return RESERVED_COLUMNS_NAMES.contains(columnName.toUpperCase());
	}
}
