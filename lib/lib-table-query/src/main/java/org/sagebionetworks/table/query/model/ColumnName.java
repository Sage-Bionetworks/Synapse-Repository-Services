package org.sagebionetworks.table.query.model;


/**
 * This matches &ltcolumn name&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ColumnName extends SQLElement {

	Identifier identifier;

	public ColumnName(Identifier identifier) {
		super();
		this.identifier = identifier;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		identifier.toSQL(builder, columnConvertor);
	}
	
}
