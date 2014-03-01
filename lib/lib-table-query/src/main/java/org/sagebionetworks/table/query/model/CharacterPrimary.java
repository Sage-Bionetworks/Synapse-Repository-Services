package org.sagebionetworks.table.query.model;

/**
 * This matches &ltcharacter primary&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class CharacterPrimary {

	ValueExpressionPrimary valueExpressionPrimary;

	public CharacterPrimary(ValueExpressionPrimary valueExpressionPrimary) {
		super();
		this.valueExpressionPrimary = valueExpressionPrimary;
	}

	public ValueExpressionPrimary getValueExpressionPrimary() {
		return valueExpressionPrimary;
	}
	
}
