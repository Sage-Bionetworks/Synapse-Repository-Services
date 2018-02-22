package org.sagebionetworks.table.query.model;

/**
 * Information about the supported MySql functions.
 *
 */
public enum MySqlFunctionName {

	// date-time
	CURRENT_TIMESTAMP(FunctionReturnType.STRING),
	CURRENT_DATE(FunctionReturnType.STRING),
	CURRENT_TIME(FunctionReturnType.STRING),
	NOW(FunctionReturnType.STRING),
	UNIX_TIMESTAMP(FunctionReturnType.LONG),
	FROM_UNIXTIME(FunctionReturnType.STRING),
	// string
	CONCAT(FunctionReturnType.STRING),
	REPLACE(FunctionReturnType.STRING),
	UPPER(FunctionReturnType.STRING),
	LOWER(FunctionReturnType.STRING),
	TRIM(FunctionReturnType.STRING),
	// aggregate
	AVG(FunctionReturnType.DOUBLE),
	COUNT(FunctionReturnType.LONG),
	MIN(FunctionReturnType.LONG),
	MAX(FunctionReturnType.LONG),
	SUM(FunctionReturnType.DOUBLE),
	BIT_AND(FunctionReturnType.LONG),
	BIT_OR(FunctionReturnType.LONG),
	BIT_XOR(FunctionReturnType.LONG),
	GROUP_CONCAT(FunctionReturnType.STRING),
	STD(FunctionReturnType.DOUBLE),
	STDDEV(FunctionReturnType.DOUBLE),
	STDDEV_POP(FunctionReturnType.DOUBLE),
	STDDEV_SAMP(FunctionReturnType.DOUBLE),
	VAR_POP(FunctionReturnType.DOUBLE),
	VAR_SAMP(FunctionReturnType.DOUBLE),
	VARIANCE(FunctionReturnType.DOUBLE),
	JSON_ARRAYAGG(FunctionReturnType.STRING),
	JSON_OBJECTAGG(FunctionReturnType.STRING),
	;
	
	FunctionReturnType returnType;
	
	MySqlFunctionName(FunctionReturnType returnType){
		this.returnType = returnType;
	}
	
	/**
	 * The return type of this function.
	 * @return
	 */
	public FunctionReturnType getFunctionReturnType(){
		return this.returnType;
	}
}
