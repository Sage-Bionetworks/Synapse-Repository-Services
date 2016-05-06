package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltidentifier&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class Identifier extends SQLElement implements HasUnquotedValue {


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

	@Override
	public void toSql(StringBuilder builder) {
		actualIdentifier.toSql(builder);		
	}

	@Override
	public String getUnquotedValue() {
		return actualIdentifier.getUnquotedValue();
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, actualIdentifier);
	}
	
}
