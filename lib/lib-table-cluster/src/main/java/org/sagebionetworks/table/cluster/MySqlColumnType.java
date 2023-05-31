package org.sagebionetworks.table.cluster;

/**
 * Supported MySQL column types for tables.
 *
 */
public enum MySqlColumnType {
	BIGINT(false, true, MySqlCastType.SIGNED),
	VARCHAR(true, true, MySqlCastType.CHAR),
	DOUBLE(false, true, MySqlCastType.DOUBLE),
	BOOLEAN(false, true, MySqlCastType.UNSIGNED),
	MEDIUMTEXT(false, false, MySqlCastType.CHAR),
	TEXT(false, true, MySqlCastType.CHAR),
	TINYINT(false, true, MySqlCastType.UNSIGNED),
	ENUM(false, true, MySqlCastType.CHAR),
	JSON(false, false, MySqlCastType.JSON);

	
	boolean hasSize;
	boolean isCreateIndex;
	MySqlCastType castType;
	
	private MySqlColumnType(boolean hasSize, boolean isCreateIndex, MySqlCastType castType){
		this.hasSize = hasSize;
		this.isCreateIndex = isCreateIndex;
		this.castType = castType;
	}
	
	/**
	 * Can type type have a size?
	 * @return
	 */
	public boolean hasSize(){
		return hasSize;
	}
	
	/**
	 * @return Should we attempt to add an index for this type of column?
	 */
	public boolean isCreateIndex() {
		return isCreateIndex;
	}
	
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
	
	public MySqlCastType getMySqlCastType() {
		return this.castType;
	}
}
