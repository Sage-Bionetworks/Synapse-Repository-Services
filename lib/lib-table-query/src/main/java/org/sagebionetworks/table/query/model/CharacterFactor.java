package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltcharacter factor&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class CharacterFactor extends SQLElement {

	CharacterPrimary characterPrimary;

	public CharacterFactor(CharacterPrimary characterPrimary) {
		this.characterPrimary = characterPrimary;
	}

	public CharacterPrimary getCharacterPrimary() {
		return characterPrimary;
	}

	public void visit(Visitor visitor) {
		visit(this.characterPrimary, visitor);
	}
	
}
