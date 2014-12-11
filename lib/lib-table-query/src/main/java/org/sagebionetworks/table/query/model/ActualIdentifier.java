package org.sagebionetworks.table.query.model;


/**
 * This matches &ltactual identifier&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ActualIdentifier extends SQLElement {
	
	String regularIdentifier;
	String delimitedIdentifier;
	public ActualIdentifier(String regularIdentifier, String delimitedIdentifier) {
		if(regularIdentifier != null && delimitedIdentifier != null) throw new IllegalArgumentException("An actual identifier must be either a regular-identifier or a delimited-identifier but not both"); 
		this.regularIdentifier = regularIdentifier;
		this.delimitedIdentifier = delimitedIdentifier;
	}
	public String getRegularIdentifier() {
		return regularIdentifier;
	}
	public String getDelimitedIdentifier() {
		return delimitedIdentifier;
	}
	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		// We do not
		if(regularIdentifier != null){
			// Regular identifiers can be written without modification.
			builder.append(regularIdentifier);
		}else{
			// Delimited identifiers must be within double quotes.
			// And double quote characters must be escaped with another double quote.
			builder.append("\"");
			builder.append(delimitedIdentifier.replaceAll("\"", "\"\""));
			builder.append("\"");
		}
	}
	
}
