package org.sagebionetworks.table.query.model;

/**
 * This matches &ltcharacter factor&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class CharacterFactor extends SQLElement {

	CharacterPrimary characterPrimary;

	public CharacterFactor(CharacterPrimary characterPrimary) {
		this.characterPrimary = characterPrimary;
	}

	public CharacterPrimary getCharacterPrimary() {
		return characterPrimary;
	}
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		characterPrimary.toSql(builder, parameters);		
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(characterPrimary);
	}
}
