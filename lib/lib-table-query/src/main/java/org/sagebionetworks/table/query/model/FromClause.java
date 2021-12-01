package org.sagebionetworks.table.query.model;

import java.util.Optional;

/**
 * This matches &ltfrom clause&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class FromClause extends SQLElement implements HasSingleTableName, HasReplaceableChildren {

	private TableReference tableReference;

	public FromClause(TableReference tableReference) {
		super();
		this.tableReference = tableReference;
	}

	public TableReference getTableReference() {
		return tableReference;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("FROM ");
		tableReference.toSql(builder, parameters);
	}

	public void setTableReference(TableReference tableReference) {
		this.tableReference = tableReference;
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(tableReference);
	}

	@Override
	public Optional<String> getSingleTableName() {
		if(tableReference == null) {
			return Optional.empty();
		}
		return tableReference.getSingleTableName();
	}

	@Override
	public void replaceChildren(Element replacment) {
		this.tableReference = (TableReference) replacment;
	}

}
