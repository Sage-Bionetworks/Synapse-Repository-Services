package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltcharacter primary&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class CharacterPrimary extends SQLElement {

	ValueExpressionPrimary valueExpressionPrimary;

	public CharacterPrimary(ValueExpressionPrimary valueExpressionPrimary) {
		this.valueExpressionPrimary = valueExpressionPrimary;
	}

	public ValueExpressionPrimary getValueExpressionPrimary() {
		return valueExpressionPrimary;
	}

	public void visit(Visitor visitor) {
		visit(this.valueExpressionPrimary, visitor);
	}
	
}
