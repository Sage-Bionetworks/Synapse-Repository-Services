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

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		if (columnConvertor != null) {
			columnConvertor.convertColumn(this, builder);
		} else {
			if (nameLHS != null) {
				this.nameLHS.toSQL(builder, columnConvertor);
				builder.append(".");
			}
			this.nameRHS.toSQL(builder, columnConvertor);
		}
	}
}
