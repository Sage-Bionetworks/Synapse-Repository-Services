package org.sagebionetworks.table.query.model;


/**
 * This matches &ltcharacter primary&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class CharacterPrimary extends SQLElement {

	ValueExpressionPrimary valueExpressionPrimary;

	public CharacterPrimary(ValueExpressionPrimary valueExpressionPrimary) {
		super();
		this.valueExpressionPrimary = valueExpressionPrimary;
	}

	public boolean isAggregate() {
		return valueExpressionPrimary.isAggregate();
	}

	public ValueExpressionPrimary getValueExpressionPrimary() {
		return valueExpressionPrimary;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		this.valueExpressionPrimary.toSQL(builder, columnConvertor);
	}
	
}
