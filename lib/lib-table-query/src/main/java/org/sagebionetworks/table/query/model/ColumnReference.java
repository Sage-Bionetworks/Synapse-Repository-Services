package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltcolumn reference&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ColumnReference extends SQLElement {

	ColumnName nameLHS;
	ColumnName nameRHS;

	public ColumnReference(ColumnName nameLHSOrRHS, ColumnName nameRHS) {
		if (nameRHS == null) {
			this.nameLHS = null;
			this.nameRHS = nameLHSOrRHS;
		} else {
			this.nameLHS = nameLHSOrRHS;
			this.nameRHS = nameRHS;
		}
	}

	public ColumnName getNameLHS() {
		return nameLHS;
	}

	public ColumnName getNameRHS() {
		return nameRHS;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if (nameLHS != null) {
			nameLHS.toSql(builder);
			builder.append(".");
		}
		nameRHS.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, nameLHS);
		checkElement(elements, type, nameRHS);
	}
}
