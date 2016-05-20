package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltcolumn name&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ColumnName extends SQLElement {

	Identifier identifier;

	public ColumnName(Identifier identifier) {
		this.identifier = identifier;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	@Override
	public void toSql(StringBuilder builder) {
		identifier.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, identifier);
	}
	
}
