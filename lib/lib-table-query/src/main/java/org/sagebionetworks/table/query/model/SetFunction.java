package org.sagebionetworks.table.query.model;

public class SetFunction {
	
	SetFunctionType setFunctionType;
	SetQuantifier setQuantifier;
	ValueExpression valueExpressionPrimary;
	
	public SetFunction(SetFunctionType setFunctionType,
			SetQuantifier setQuantifier,
			ValueExpression valueExpressionPrimary) {
		super();
		this.setFunctionType = setFunctionType;
		this.setQuantifier = setQuantifier;
		this.valueExpressionPrimary = valueExpressionPrimary;
	}
	
	
}
