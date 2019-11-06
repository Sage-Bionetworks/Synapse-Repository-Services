package org.sagebionetworks.table.query.model;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * 
 * Possible SQL function return types.
 *
 */
public enum FunctionReturnType {

	STRING(ColumnType.STRING),
	LONG(ColumnType.INTEGER),
	DOUBLE(ColumnType.DOUBLE),
	// Cases where the return type matches the parameter type.
	MATCHES_PARAMETER(null);
	
	private ColumnType columnType;
	
	FunctionReturnType(ColumnType type){
		this.columnType = type;
	}
	
	/**
	 * Get the ColumnType that matches the function return type.
	 * @param parameterType The type of the parameter passed to the function.
	 * For some functions, the return type matches the parameter type.
	 * @return
	 */
	public ColumnType getColumnType(ColumnType parameterType) {
		if(this.columnType != null) {
			return columnType;
		}else {
			return parameterType;
		}
	}
}
