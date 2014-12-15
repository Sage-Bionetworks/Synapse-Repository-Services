package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.GetTableNameVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.ToTranslatedSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &lttable reference&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class TableReference extends SQLElement {

	String tableName;

	public TableReference(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	public void visit(Visitor visitor) {
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append(tableName);
	}

	public void visit(ToTranslatedSqlVisitor visitor) {
		visitor.convertTableName(tableName);
	}

	public void visit(GetTableNameVisitor visitor) {
		visitor.setTableName(tableName);
	}
}
