package org.sagebionetworks.table.query.model.functionreturntypehandler;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Always matchs the same type as the passed in parameter
 */
public class MatchParameterFunctionReturnTypeHandler implements FunctionReturnTypeHandler {
	@Override
	public ColumnType returnValueColumnType(ColumnType parameterColumnType) {
		return parameterColumnType;
	}
}
