package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class TableExpression implements SQLElement {

	FromClause fromClause;
	WhereClause whereClause;

	public TableExpression(FromClause fromClause, WhereClause whereClause) {
		super();
		this.fromClause = fromClause;
		this.whereClause = whereClause;
	}

	public WhereClause getWhereClause() {
		return whereClause;
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		fromClause.toSQL(builder);
		if(whereClause != null){
			builder.append(" ");
			whereClause.toSQL(builder);
		}
	}

}
