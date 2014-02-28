package org.sagebionetworks.table.query.model;

/**
 * This matches &ltvalue expression primary&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ValueExpressionPrimary {

	UnsignedValueSpecification unsignedValueSpecification;
	ColumnReference columnReference;
	SetFunctionSpecification setFunctionSpecification;
	
	public ValueExpressionPrimary(
			UnsignedValueSpecification unsignedValueSpecification) {
		super();
		this.unsignedValueSpecification = unsignedValueSpecification;
	}
	
	public ValueExpressionPrimary(ColumnReference columnReference) {
		super();
		this.columnReference = columnReference;
	}

	public ValueExpressionPrimary(
			SetFunctionSpecification setFunctionSpecification) {
		super();
		this.setFunctionSpecification = setFunctionSpecification;
	}

	public UnsignedValueSpecification getUnsignedValueSpecification() {
		return unsignedValueSpecification;
	}
	public ColumnReference getColumnReference() {
		return columnReference;
	}
	public SetFunctionSpecification getSetFunctionSpecification() {
		return setFunctionSpecification;
	}
	
	
}
