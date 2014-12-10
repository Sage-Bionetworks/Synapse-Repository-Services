package org.sagebionetworks.table.query.model;

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

	public void visit(Visitor visitor) {
		if (nameLHS != null) {
			visit(this.nameLHS, visitor);
		}
		visit(this.nameRHS, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		if (nameLHS != null) {
			visit(this.nameLHS, visitor);
			visitor.append(".");
		}
		visit(this.nameRHS, visitor);
	}

	public void visit(ToTranslatedSqlVisitor visitor) {
		visitor.convertColumn(this);
	}
}
