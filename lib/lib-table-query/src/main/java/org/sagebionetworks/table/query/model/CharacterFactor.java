package org.sagebionetworks.table.query.model;

import java.util.List;

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
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, characterPrimary);
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(characterPrimary);
	}
}
