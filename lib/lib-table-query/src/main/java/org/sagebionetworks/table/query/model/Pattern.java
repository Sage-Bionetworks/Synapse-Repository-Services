package org.sagebionetworks.table.query.model;


/**
 * This matches &ltpattern&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class Pattern extends SQLElement {
	
	CharacterValueExpression characterValueExpression;

	public Pattern(CharacterValueExpression characterValueExpression) {
		super();
		this.characterValueExpression = characterValueExpression;
	}

	public CharacterValueExpression getCharacterValueExpression() {
		return characterValueExpression;
	}
	
	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		characterValueExpression.toSQL(builder, columnConvertor);
	}
}
