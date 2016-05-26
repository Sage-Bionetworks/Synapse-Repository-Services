package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &lttable expression&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class TableExpression extends SQLElement implements HasAggregate {

	FromClause fromClause;
	WhereClause whereClause;
	GroupByClause groupByClause;
	OrderByClause orderByClause;
	Pagination pagination;

	public TableExpression(FromClause fromClause, WhereClause whereClause, GroupByClause groupByClause, OrderByClause orderByClause, Pagination pagination) {
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
	public void toSql(StringBuilder builder) {
		fromClause.toSql(builder);
		if(whereClause != null){
			builder.append(" ");
			whereClause.toSql(builder);
		}
		if(groupByClause != null){
			builder.append(" ");
			groupByClause.toSql(builder);
		}
		if(orderByClause != null){
			builder.append(" ");
			orderByClause.toSql(builder);
		}
		if(pagination != null){
			builder.append(" ");
			pagination.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, fromClause);
		checkElement(elements, type, whereClause);
		checkElement(elements, type, groupByClause);
		checkElement(elements, type, orderByClause);
		checkElement(elements, type, pagination);
	}

	@Override
	public boolean isElementAggregate() {
		return groupByClause != null;
	}

	/**
	 * Replace the existing pagination with the passed pagination.
	 * @param pagination
	 */
	public void replace(Pagination pagination) {
		this.pagination = pagination;
	}
}
