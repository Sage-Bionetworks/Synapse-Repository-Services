package org.sagebionetworks.table.cluster;

/**
 * Supported MySQL column types for tables.
 *
 */
public enum MySqlColumnType {
	BIGINT,
	VARCHAR,
	DOUBLE,
	BOOLEAN,
	MEDIUMTEXT,
	TINYINT,
	ENUM;
	
	
	/**
	 * Get the MySqlColumnType for a given type string.
	 * @param type
	 * @return
	 */
	public static MySqlColumnType parserType(String typeString){
		String[] split = typeString.split("\\(");
		return valueOf(split[0].toUpperCase());
	}
	
	/**
	 * Parse the size from the given type string.
	 * 
	 * @param typeString
	 * @return Returns null if the type string does not have a numeric size.
	 */
	public static Integer parseSize(String typeString){
		String[] split = typeString.split("\\(");
		if(split.length != 2){
			return null;
		}
		split = split[1].split("\\)");
		try {
			return Integer.parseInt(split[0]);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
