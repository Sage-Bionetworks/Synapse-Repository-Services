package org.sagebionetworks.table.query.model;

import java.util.List;

public class DelimitedIdentifier extends SQLElement implements HasQuoteValue {

	String overrideSql;
	String delimitedIdentifier;
	
	public DelimitedIdentifier(String delimitedIdentifier) {
		super();
		this.delimitedIdentifier = delimitedIdentifier;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(overrideSql != null){
			builder.append(overrideSql);
			return;
		}
		// Delimited identifiers must be within double quotes.
		// And double quote characters must be escaped with another double quote.
		builder.append("\"");
		builder.append(delimitedIdentifier.replaceAll("\"", "\"\""));
		builder.append("\"");
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this is a leaf element.
	}

	@Override
	public String getValueWithoutQuotes() {
		return delimitedIdentifier;
	}

	@Override
	public boolean isSurrounedeWithQuotes() {
		return true;
	}

	@Override
	public void overrideSql(String overrideSql) {
		this.overrideSql = overrideSql;
	}

}
