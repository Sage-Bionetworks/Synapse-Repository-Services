package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.Visitor;

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

	public void visit(Visitor visitor) {
		visit(actualIdentifier, visitor);
	}
	
}
