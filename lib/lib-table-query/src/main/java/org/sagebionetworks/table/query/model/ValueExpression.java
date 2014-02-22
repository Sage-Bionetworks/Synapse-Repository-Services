package org.sagebionetworks.table.query.model;

public class ValueExpression {

	SetFunction setFunction;
	ColumnReference columnReference;
	public ValueExpression(SetFunction setFunction,
			ColumnReference columnReference) {
		super();
		this.setFunction = setFunction;
		this.columnReference = columnReference;
	}	
	
}
