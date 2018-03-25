package org.sagebionetworks.table.query.model;

import static org.sagebionetworks.table.query.model.FunctionReturnType.*;
/**
 * This matches &ltset function type&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public enum SetFunctionType {
	AVG(DOUBLE),
	COUNT(LONG),
	MIN(MATCHES_PARAMETER),
	MAX(MATCHES_PARAMETER),
	SUM(MATCHES_PARAMETER),
	BIT_AND(LONG),
	BIT_OR(LONG),
	BIT_XOR(LONG),
	STD(DOUBLE),
	STDDEV(DOUBLE),
	STDDEV_POP(DOUBLE),
	STDDEV_SAMP(DOUBLE),
	VAR_POP(DOUBLE),
	VAR_SAMP(DOUBLE),
	VARIANCE(DOUBLE),
	;
	
	FunctionReturnType returnType;
	
	SetFunctionType(FunctionReturnType returnType){
		this.returnType = returnType;
	}
	
	/**
	 * The return type for a given fuction.
	 * @return
	 */
	public FunctionReturnType getFunctionReturnType() {
		return this.returnType;
	}
}
