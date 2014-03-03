package org.sagebionetworks.table.query.model;

/**
 * This matches &ltderived column&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class DerivedColumn {

	ValueExpression valueExpression;
	String asClause;
	
	public DerivedColumn(ValueExpression valueExpression, String asClause) {
		super();
		this.valueExpression = valueExpression;
		this.asClause = asClause;
	}

	public ValueExpression getValueExpression() {
		return valueExpression;
	}

	public String getAsClause() {
		return asClause;
	}
	
}
