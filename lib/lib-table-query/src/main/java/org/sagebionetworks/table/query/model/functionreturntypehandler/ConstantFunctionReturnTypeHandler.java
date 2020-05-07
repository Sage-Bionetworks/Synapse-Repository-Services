package org.sagebionetworks.table.query.model.functionreturntypehandler;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Function return type will always be what was passed in to the constructor
 */
public class ConstantFunctionReturnTypeHandler implements FunctionReturnTypeHandler {
	ColumnType returnType;

	public ConstantFunctionReturnTypeHandler(ColumnType returnType){
		this.returnType = returnType;
	}

	@Override
	public ColumnType returnValueColumnType(ColumnType ignoredParameterType) {
		return this.returnType;
	}
}
