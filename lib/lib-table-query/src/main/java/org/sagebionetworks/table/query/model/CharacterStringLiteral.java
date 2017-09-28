package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * 
 *  A CharacterStringLiteral is a string surrounded with single quotes.
 *
 */
public class CharacterStringLiteral extends SQLElement {

	String characterStringLiteral;

	public CharacterStringLiteral(String characterStringLiteral) {
		super();
		this.characterStringLiteral = characterStringLiteral;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(parameters.includeQuotes()){
			// character literals have single quotes
			builder.append("'");
			// single quotes within the string must be replaced.
			builder.append(this.characterStringLiteral.replaceAll("'", "''"));
			builder.append("'");
		}else{
			builder.append(this.characterStringLiteral);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this is a leaf element
	}

	@Override
	public boolean hasQuotes() {
		return true;
	}

}
