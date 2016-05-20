package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltcharacter value expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class CharacterValueExpression extends SQLElement {

	CharacterFactor characterFactor;

	public CharacterValueExpression(CharacterFactor characterFactor) {
		this.characterFactor = characterFactor;
	}

	public CharacterFactor getCharacterFactor() {
		return characterFactor;
	}

	@Override
	public void toSql(StringBuilder builder) {
		characterFactor.toSql(builder);		
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, characterFactor);
	}
	
}
