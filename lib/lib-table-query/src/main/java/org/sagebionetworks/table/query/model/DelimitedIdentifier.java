package org.sagebionetworks.table.query.model;

import java.util.List;

public class DelimitedIdentifier extends SQLElement {

	String delimitedIdentifier;
	
	public DelimitedIdentifier(String delimitedIdentifier) {
		super();
		this.delimitedIdentifier = delimitedIdentifier;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(parameters.includeQuotes()){
			// Delimited identifiers must be within double quotes.
			// And double quote characters must be escaped with another double quote.
			builder.append("`");
			builder.append(delimitedIdentifier.replaceAll("`", "``"));
			builder.append("`");
		}else{
			builder.append(delimitedIdentifier);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this is a leaf element.
	}
	
	@Override
	public boolean hasQuotes(){
		return true;
	}

}
