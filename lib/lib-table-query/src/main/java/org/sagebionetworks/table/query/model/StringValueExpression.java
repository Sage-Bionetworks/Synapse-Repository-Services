package org.sagebionetworks.table.query.model;

/**
 * This matches &ltstring value expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class StringValueExpression {

	CharacterValueExpression characterValueExpression;

	public StringValueExpression(
			CharacterValueExpression characterValueExpression) {
		super();
		this.characterValueExpression = characterValueExpression;
	}

	public CharacterValueExpression getCharacterValueExpression() {
		return characterValueExpression;
	}
	
}
