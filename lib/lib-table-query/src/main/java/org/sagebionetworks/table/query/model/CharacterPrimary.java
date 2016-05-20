package org.sagebionetworks.table.query.model;

import java.util.List;

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

	@Override
	public void toSql(StringBuilder builder) {
		valueExpressionPrimary.toSql(builder);		
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, valueExpressionPrimary);
	}

}
