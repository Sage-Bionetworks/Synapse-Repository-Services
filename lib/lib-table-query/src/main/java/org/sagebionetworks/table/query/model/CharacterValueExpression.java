package org.sagebionetworks.table.query.model;


/**
 * This matches &ltcharacter value expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class CharacterValueExpression extends SQLElement {

	CharacterFactor characterFactor;

	public CharacterValueExpression(CharacterFactor characterFactor) {
		super();
		this.characterFactor = characterFactor;
	}

	public CharacterFactor getCharacterFactor() {
		return characterFactor;
	}

	public void visit(Visitor visitor) {
		visit(this.characterFactor, visitor);
	}
	
}
