package org.sagebionetworks.table.query.model;

import java.util.List;

public class GeneralLiteral extends SQLElement implements HasQuoteValue {
	
	String generalLiteral;

	public GeneralLiteral(String generalLiteral) {
		super();
		this.generalLiteral = generalLiteral;
	}

	@Override
	public void toSql(StringBuilder builder) {
		// General literals have single quotes
		builder.append("'");
		builder.append(this.generalLiteral.replaceAll("'", "''"));
		builder.append("'");
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// no sub-elements
	}

	@Override
	public String getValueWithoutQuotes() {
		return generalLiteral;
	}

	@Override
	public boolean isSurrounedeWithQuotes() {
		return true;
	}

	@Override
	public void replaceUnquoted(String newValue) {
		generalLiteral = newValue;
	}

}
