package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltvalue expression primary&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ValueExpressionPrimary extends SQLElement {

	SignedValueSpecification signedValueSpecification;
	ColumnReference columnReference;
	SetFunctionSpecification setFunctionSpecification;
	
	public ValueExpressionPrimary(SignedValueSpecification signedValueSpecification) {
		this.signedValueSpecification = signedValueSpecification;
	}
	
	public ValueExpressionPrimary(ColumnReference columnReference) {
		this.columnReference = columnReference;
	}

	public ValueExpressionPrimary(SetFunctionSpecification setFunctionSpecification) {
		this.setFunctionSpecification = setFunctionSpecification;
	}

	public SignedValueSpecification getSignedValueSpecification() {
		return signedValueSpecification;
	}
	public ColumnReference getColumnReference() {
		return columnReference;
	}
	public SetFunctionSpecification getSetFunctionSpecification() {
		return setFunctionSpecification;
	}

	public void visit(Visitor visitor) {
		// only one element at a time will be no null
		if (signedValueSpecification != null) {
			visit(signedValueSpecification, visitor);
		} else if (columnReference != null) {
			visit(columnReference, visitor);
		} else {
			visit(setFunctionSpecification, visitor);
		}
	}
	
	
}
