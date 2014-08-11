package org.sagebionetworks.table.query.model;

/**
 * This matches &ltvalue expression primary&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ValueExpressionPrimary implements SQLElement {

	SignedValueSpecification signedValueSpecification;
	ColumnReference columnReference;
	SetFunctionSpecification setFunctionSpecification;
	
	public ValueExpressionPrimary(
SignedValueSpecification signedValueSpecification) {
		super();
		this.signedValueSpecification = signedValueSpecification;
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

	public SignedValueSpecification getSignedValueSpecification() {
		return signedValueSpecification;
	}
	public ColumnReference getColumnReference() {
		return columnReference;
	}
	public SetFunctionSpecification getSetFunctionSpecification() {
		return setFunctionSpecification;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		// only one element at a time will be no null
		if (signedValueSpecification != null) {
			signedValueSpecification.toSQL(builder);
		}else if(columnReference != null){
			columnReference.toSQL(builder);
		}else{
			setFunctionSpecification.toSQL(builder);
		}
	}
	
	
}
