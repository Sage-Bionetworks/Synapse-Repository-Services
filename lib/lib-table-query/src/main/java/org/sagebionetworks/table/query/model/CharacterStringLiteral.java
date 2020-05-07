package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnConstants;

/**
 * 
 *  A CharacterStringLiteral is a string surrounded with single quotes.
 *
 */
public class CharacterStringLiteral extends SQLElement {

	String characterStringLiteral;

	public CharacterStringLiteral(String characterStringLiteral) {
		super();

		if(characterStringLiteral.length() > ColumnConstants.MAX_ALLOWED_STRING_SIZE){
			throw new IllegalArgumentException("String exceeds maximum allowed size: " + ColumnConstants.MAX_ALLOWED_STRING_SIZE);
		}
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
