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
	YEARWEEK(FunctionReturnType.LONG),
	DATE(FunctionReturnType.STRING),
	DAYNAME(FunctionReturnType.STRING),
	DAYOFWEEK(FunctionReturnType.LONG),
	DAYOFMONTH(FunctionReturnType.LONG),
	DAYOFYEAR(FunctionReturnType.LONG),
	WEEKOFYEAR(FunctionReturnType.LONG),
	MONTHNAME(FunctionReturnType.STRING),
	// date-time-fields
	MICROSECOND(FunctionReturnType.LONG),
	SECOND(FunctionReturnType.LONG),
	MINUTE(FunctionReturnType.LONG),
	HOUR(FunctionReturnType.LONG),
	DAY(FunctionReturnType.LONG),
	WEEK(FunctionReturnType.LONG),
	MONTH(FunctionReturnType.LONG),
	QUARTER(FunctionReturnType.LONG),
	YEAR(FunctionReturnType.LONG),
	// string
	CONCAT(FunctionReturnType.STRING),
	REPLACE(FunctionReturnType.STRING),
	UPPER(FunctionReturnType.STRING),
	LOWER(FunctionReturnType.STRING),
	TRIM(FunctionReturnType.STRING),
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
