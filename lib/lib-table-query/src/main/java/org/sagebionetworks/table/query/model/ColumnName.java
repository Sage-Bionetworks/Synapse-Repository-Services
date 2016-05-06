package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltcolumn name&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ColumnName extends SQLElement implements HasUnquotedValue {

	Identifier identifier;

	public ColumnName(Identifier identifier) {
		this.identifier = identifier;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void visit(Visitor visitor) {
		visit(identifier, visitor);
	}

	@Override
	public void toSql(StringBuilder builder) {
		identifier.toSql(builder);
	}

	@Override
	public String getUnquotedValue() {
		return identifier.getUnquotedValue();
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, identifier);
	}
	
}
