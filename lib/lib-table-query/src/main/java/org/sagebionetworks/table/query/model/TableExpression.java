package org.sagebionetworks.table.query.model;


/**
 * This matches &lttable expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class TableExpression extends SQLElement {

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

	public OrderByClause getOrderByClause() {
		return orderByClause;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		fromClause.toSQL(builder, columnConvertor);
		if(whereClause != null){
			builder.append(" ");
			whereClause.toSQL(builder, columnConvertor);
		}
		if(groupByClause != null){
			builder.append(" ");
			groupByClause.toSQL(builder, columnConvertor);
		}
		if(orderByClause != null){
			builder.append(" ");
			orderByClause.toSQL(builder, columnConvertor);
		}
		if(pagination != null){
			builder.append(" ");
			pagination.toSQL(builder, columnConvertor);
		}
	}

}
