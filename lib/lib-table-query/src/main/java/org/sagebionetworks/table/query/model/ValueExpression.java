package org.sagebionetworks.table.query.model;

/**
 * This matches &ltvalue expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ValueExpression {

	StringValueExpression stringValueExpression;
	
	public ValueExpression(StringValueExpression stringValueExpression) {
		super();
		this.stringValueExpression = stringValueExpression;
	}
	public StringValueExpression getStringValueExpression() {
		return stringValueExpression;
	}
	
}
