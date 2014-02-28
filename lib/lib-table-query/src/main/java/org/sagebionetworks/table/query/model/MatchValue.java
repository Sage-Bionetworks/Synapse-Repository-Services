package org.sagebionetworks.table.query.model;

/**
 * This matches &ltmatch value&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class MatchValue {
	
	CharacterValueExpression characterValueExpression;

	
	public MatchValue(CharacterValueExpression characterValueExpression) {
		super();
		this.characterValueExpression = characterValueExpression;
	}

	public CharacterValueExpression getCharacterValueExpression() {
		return characterValueExpression;
	}

}