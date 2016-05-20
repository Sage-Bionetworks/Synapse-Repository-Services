package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltidentifier&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class Identifier extends SQLElement {


	ActualIdentifier actualIdentifier;
	
	public Identifier(ActualIdentifier actualIdentifier) {
		this.actualIdentifier = actualIdentifier;
	}

	public ActualIdentifier getActualIdentifier() {
		return actualIdentifier;
	}

	@Override
	public void toSql(StringBuilder builder) {
		actualIdentifier.toSql(builder);		
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, actualIdentifier);
	}
	
}
