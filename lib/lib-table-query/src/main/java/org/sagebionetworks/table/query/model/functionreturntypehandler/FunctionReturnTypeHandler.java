package org.sagebionetworks.table.query.model.functionreturntypehandler;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Determines the return type of a function
 */
public interface FunctionReturnTypeHandler {

	/**
	 * Determines the return type of the function
	 * @param parameterColumnType ColumnType of the function's input parameter
	 *                            that may or may not be used to determine the function's return type
	 * @return return type of the function
	 */
	ColumnType returnValueColumnType(ColumnType parameterColumnType);
}
