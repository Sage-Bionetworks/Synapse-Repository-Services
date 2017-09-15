package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * A GeneralLiteral is a string surrounded with single quotes.
 *
 */
public class GeneralLiteral extends SQLElement {
		
	String generalLiteral;

	public GeneralLiteral(String generalLiteral) {
		super();
		this.generalLiteral = generalLiteral;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(parameters.includeQuotes()){
			// General literals have single quotes
			builder.append("'");
			// single quotes within the string must be replaced.
			builder.append(this.generalLiteral.replaceAll("'", "''"));
			builder.append("'");
		}else{
			builder.append(this.generalLiteral);
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
