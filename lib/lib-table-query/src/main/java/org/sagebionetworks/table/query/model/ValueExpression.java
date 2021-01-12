package org.sagebionetworks.table.query.model;

/**
 * ValueExpression ::= {@link NumericValueExpression}
 */
public class ValueExpression extends SimpleBranch {

	public ValueExpression(NumericValueExpression numericValueExpression) {
		super(numericValueExpression);
	}

	public String getDisplayName(){
		NumericPrimary numericPrimary = this.getFirstElementOfType(NumericPrimary.class);
		SQLElement numericPrimaryChild = numericPrimary.getChild();
		if (numericPrimaryChild instanceof  ValueExpressionPrimary){
			ValueExpressionPrimary valueExpressionPrimary = (ValueExpressionPrimary) numericPrimaryChild;
			if(valueExpressionPrimary.getChild() instanceof ColumnReference || valueExpressionPrimary.getChild() instanceof UnsignedValueSpecification) {
				return this.toSqlWithoutQuotes();
			}
		}

		return this.toSql();
	}

}
