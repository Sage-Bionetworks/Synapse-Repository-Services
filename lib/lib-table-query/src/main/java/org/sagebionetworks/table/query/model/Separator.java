package org.sagebionetworks.table.query.model;

/**
 * SQLElement representing: 'SEPARATOR &ltcharacter_string_literal&gt'
 *
 */
public class Separator extends SQLElement {
	
	CharacterStringLiteral separatorValue;
	
	public Separator(CharacterStringLiteral separatorValue) {
		super();
		this.separatorValue = separatorValue;
	}
	
	public CharacterStringLiteral getSeparatorValue() {
		return separatorValue;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("SEPARATOR ");
		separatorValue.toSql(builder, parameters);
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(separatorValue);
	}
}
