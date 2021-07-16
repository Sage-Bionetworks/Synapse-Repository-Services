package org.sagebionetworks.table.query.model;

/**
 * BacktickDelimitedIdentifier ::= ` identifier `
 *
 */
public class BacktickDelimitedIdentifier extends LeafElement {
	
	private static final String DOUBLE_BACKTICK = "``";
	private static final String BACKTICK = "`";
	
	String identifier;

	public BacktickDelimitedIdentifier(String identifier) {
		super();
		this.identifier = identifier;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(parameters.includeQuotes()){
			// Delimited identifiers must be within quotes.
			// And quote characters must be escaped with another quote.
			builder.append(BACKTICK);
			builder.append(identifier.replaceAll(BACKTICK, DOUBLE_BACKTICK));
			builder.append(BACKTICK);
		}else{
			builder.append(identifier);
		}
	}
	
	@Override
	public boolean hasQuotes(){
		return true;
	}

}
