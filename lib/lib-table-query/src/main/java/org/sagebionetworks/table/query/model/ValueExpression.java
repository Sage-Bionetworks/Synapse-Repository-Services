package org.sagebionetworks.table.query.model;

/**
 * This matches &ltvalue expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ValueExpression {

	SetFunctionSpecification setFunction;
	ColumnReference columnReference;
	public ValueExpression(SetFunctionSpecification setFunction,
			ColumnReference columnReference) {
		super();
		this.setFunction = setFunction;
		this.columnReference = columnReference;
	}
	public SetFunctionSpecification getSetFunction() {
		return setFunction;
	}
	public ColumnReference getColumnReference() {
		return columnReference;
	}	
	
}
