package org.sagebionetworks.table.query.model;

public enum ArrayFunctionType {

	/* returns individual rows for each element of the lists in a
	LIST type column.
	For example:
	given a column named STR_LIST with 2 rows:
		["a","b","c"]
		["d","e"]

	UNNEST(STR_LIST) will return 5 rows:
		a
		b
		c
		d
		e
	 */
	UNNEST(FunctionReturnType.UNNEST_PARAMETER);

	FunctionReturnType returnType;

	ArrayFunctionType(FunctionReturnType returnType){
		this.returnType = returnType;
	}

	/**
	 * The return type for a given function.
	 * @return
	 */
	public FunctionReturnType getFunctionReturnType() {
		return this.returnType;
	}
}
