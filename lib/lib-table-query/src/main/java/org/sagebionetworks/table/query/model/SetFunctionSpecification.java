package org.sagebionetworks.table.query.model;

/**
 * This matches &ltset function specification&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SetFunctionSpecification {
	
	Boolean countAsterisk;
	SetFunctionType setFunctionType;
	SetQuantifier setQuantifier;
	ValueExpression valueExpression;
	
	public SetFunctionSpecification(Boolean countAsterisk) {
		super();
		this.countAsterisk = countAsterisk;
	}
	
	public SetFunctionSpecification(SetFunctionType setFunctionType,
			SetQuantifier setQuantifier, ValueExpression valueExpression) {
		super();
		this.setFunctionType = setFunctionType;
		this.setQuantifier = setQuantifier;
		this.valueExpression = valueExpression;
	}

	public Boolean getCountAsterisk() {
		return countAsterisk;
	}

	public SetFunctionType getSetFunctionType() {
		return setFunctionType;
	}

	public SetQuantifier getSetQuantifier() {
		return setQuantifier;
	}

	public ValueExpression getValueExpression() {
		return valueExpression;
	}

}
