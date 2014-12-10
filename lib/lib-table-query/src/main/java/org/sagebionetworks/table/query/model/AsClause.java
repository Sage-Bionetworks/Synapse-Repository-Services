package org.sagebionetworks.table.query.model;


/**
 * This matches &ltas clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class AsClause extends SQLElement {
	
	ColumnName columnName;

	public AsClause(ColumnName columnName) {
		super();
		this.columnName = columnName;
	}

	public ColumnName getColumnName() {
		return columnName;
	}

	public void visit(Visitor visitor) {
		visit(this.columnName, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append("AS ");
		visit(this.columnName, visitor);
	}

	public void visit(ToTranslatedSqlVisitor visitor) {
		visitor.append("AS ");
		visit(this.columnName, visitor);
		visitor.addAsColumn(columnName);
	}
}
