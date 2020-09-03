package org.sagebionetworks.table.query.model;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.model.functionreturntypehandler.FunctionReturnTypeHandler;
import org.sagebionetworks.table.query.model.functionreturntypehandler.MatchParameterFunctionReturnTypeHandler;
import org.sagebionetworks.table.query.model.functionreturntypehandler.ConstantFunctionReturnTypeHandler;
import org.sagebionetworks.table.query.model.functionreturntypehandler.UnnestParameterFunctionReturnTypeHandler;

/**
 * 
 * Possible SQL function return types.
 *
 */
public enum FunctionReturnType {

	STRING(ColumnType.STRING),
	LONG(ColumnType.INTEGER),
	DOUBLE(ColumnType.DOUBLE),
	USERID(ColumnType.USERID),
	// Cases where the return type matches the parameter type.
	MATCHES_PARAMETER(new MatchParameterFunctionReturnTypeHandler()),
	// Cases where the parameter type is a list and the return type is the non-list version of that parameter type
	UNNEST_PARAMETER(new UnnestParameterFunctionReturnTypeHandler());

	private FunctionReturnTypeHandler handler;

	FunctionReturnType(ColumnType type){
		this.handler = new ConstantFunctionReturnTypeHandler(type);
	}

	FunctionReturnType(FunctionReturnTypeHandler handler){
		this.handler = handler;
	}
	
	/**
	 * Get the ColumnType that matches the function return type.
	 * @param parameterType The type of the parameter passed to the function.
	 * For some functions, the return type matches the parameter type.
	 * @return
	 */
	public ColumnType getColumnType(ColumnType parameterType) {
		return this.handler.returnValueColumnType(parameterType);
	}
}
