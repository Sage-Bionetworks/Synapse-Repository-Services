package org.sagebionetworks.table.query.model;


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

	public boolean isAggregate() {
		if (setFunctionSpecification != null) {
			return setFunctionSpecification.isAggregate();
		} else {
			return false;
		}
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
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		// only one element at a time will be no null
		if (signedValueSpecification != null) {
			signedValueSpecification.toSQL(builder, columnConvertor);
		}else if(columnReference != null){
			columnReference.toSQL(builder, columnConvertor);
		}else{
			setFunctionSpecification.toSQL(builder, columnConvertor);
		}
	}
}
