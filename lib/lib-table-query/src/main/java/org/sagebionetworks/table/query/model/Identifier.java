package org.sagebionetworks.table.query.model;


/**
 * This matches &ltidentifier&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class Identifier extends SQLElement {

	ActualIdentifier actualIdentifier;

	public Identifier(ActualIdentifier actualIdentifier) {
		super();
		this.actualIdentifier = actualIdentifier;
	}

	public ActualIdentifier getActualIdentifier() {
		return actualIdentifier;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		actualIdentifier.toSQL(builder, columnConvertor);
	}
	
}
