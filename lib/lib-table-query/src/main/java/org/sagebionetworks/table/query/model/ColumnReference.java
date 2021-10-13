package org.sagebionetworks.table.query.model;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * This matches &ltcolumn reference&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class ColumnReference extends SQLElement {

	ColumnName nameLHS;
	ColumnName nameRHS;
	ColumnType implicitColumnType;

	public ColumnReference(ColumnName nameLHSOrRHS, ColumnName nameRHS, ColumnType implicitColumnType) {
		if (nameRHS == null) {
			this.nameLHS = null;
			this.nameRHS = nameLHSOrRHS;
		} else {
			this.nameLHS = nameLHSOrRHS;
			this.nameRHS = nameRHS;
		}
		this.implicitColumnType = implicitColumnType;
	}
	
	public ColumnReference(ColumnName nameLHSOrRHS, ColumnName nameRHS) {
		this(nameLHSOrRHS, nameRHS, null);
	}

	public ColumnName getNameLHS() {
		return nameLHS;
	}

	public ColumnName getNameRHS() {
		return nameRHS;
	}
	
	/**
	 * @return For an implicit column (e.g. not exposed to the end user) return the column type
	 */
	public ColumnType getImplicitColumnType() {
		return implicitColumnType;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if (nameLHS != null) {
			nameLHS.toSql(builder, parameters);
			builder.append(".");
		}
		nameRHS.toSql(builder, parameters);
	}
	
	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(nameLHS, nameRHS);
	}
	
}
