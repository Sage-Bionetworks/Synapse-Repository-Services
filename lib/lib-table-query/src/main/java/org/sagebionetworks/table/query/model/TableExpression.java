package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class TableExpression implements SQLElement {

	FromClause fromClause;
	WhereClause whereClause;
	GroupByClause groupByClause;

	public TableExpression(FromClause fromClause, WhereClause whereClause, GroupByClause groupByClause) {
		super();
		this.fromClause = fromClause;
		this.whereClause = whereClause;
		this.groupByClause = groupByClause;
	}

	public WhereClause getWhereClause() {
		return whereClause;
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	public GroupByClause getGroupByClause() {
		return groupByClause;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		fromClause.toSQL(builder);
		if(whereClause != null){
			builder.append(" ");
			whereClause.toSQL(builder);
		}
		if(groupByClause != null){
			builder.append(" ");
			groupByClause.toSQL(builder);
		}
	}

}
