package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * DoubleQuoteDelimitedIdentifier ::= " identifier "
 *
 */
public class DoubleQuoteDelimitedIdentifier extends SQLElement {
	
	private static final String DOUBLE_DOUBLE_QUOTES = "\"\"";
	private static final String DOUBLE_QUOTES = "\"";
	
	String identifer;
	
	public DoubleQuoteDelimitedIdentifier(String identifer) {
		super();
		this.identifer = identifer;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(parameters.includeQuotes()){
			// Delimited identifiers must be within double quotes.
			// And double quote characters must be escaped with another double quote.
			builder.append(DOUBLE_QUOTES);
			builder.append(identifer.replaceAll(DOUBLE_QUOTES, DOUBLE_DOUBLE_QUOTES));
			builder.append(DOUBLE_QUOTES);
		}else{
			builder.append(identifer);
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
