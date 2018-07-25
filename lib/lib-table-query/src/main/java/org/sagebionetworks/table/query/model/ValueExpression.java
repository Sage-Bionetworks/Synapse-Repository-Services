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
		if (numericPrimaryChild instanceof  ValueExpressionPrimary
				&& ((ValueExpressionPrimary) numericPrimaryChild).getChild() instanceof ColumnReference){
			return this.toSqlWithoutQuotes();
		}
		return this.toSql();
	}

}
