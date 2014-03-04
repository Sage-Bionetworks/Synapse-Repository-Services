package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class TableExpression implements SQLElement {

	FromClause fromClause;
	WhereClause whereClause;
	GroupByClause groupByClause;
	OrderByClause orderByClause;
	Pagination pagination;

	public TableExpression(FromClause fromClause, WhereClause whereClause, GroupByClause groupByClause, OrderByClause orderByClause, Pagination pagination) {
		super();
		this.fromClause = fromClause;
		this.whereClause = whereClause;
		this.groupByClause = groupByClause;
		this.orderByClause = orderByClause;
		this.pagination = pagination;
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

	public Pagination getPagination() {
		return pagination;
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
		if(orderByClause != null){
			builder.append(" ");
			orderByClause.toSQL(builder);
		}
		if(pagination != null){
			builder.append(" ");
			pagination.toSQL(builder);
		}
	}

}
