package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &lttable expression&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		fromClause.toSql(builder, parameters);
		if(whereClause != null){
			builder.append(" ");
			whereClause.toSql(builder, parameters);
		}
		if(groupByClause != null){
			builder.append(" ");
			groupByClause.toSql(builder, parameters);
		}
		if(orderByClause != null){
			builder.append(" ");
			orderByClause.toSql(builder, parameters);
		}
		if(pagination != null){
			builder.append(" ");
			pagination.toSql(builder, parameters);
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
	public Iterable<Element> children() {
		return SQLElement.buildChildren(fromClause, whereClause, groupByClause, orderByClause, pagination);
	}

	@Override
	public boolean isElementAggregate() {
		return groupByClause != null;
	}

	/**
	 * Replace the existing pagination with the passed pagination.
	 * @param pagination
	 */
	public void replacePagination(Pagination pagination) {
		this.pagination = pagination;
	}

	/**
	 * Replace the existing group by with the passed group by.
	 * @param groupBy
	 */
	public void replaceGroupBy(GroupByClause groupBy) {
		this.groupByClause = groupBy;
	}
	
	/**
	 * Replace the existing group by with the passed group by.
	 * @param orderBy
	 */
	public void replaceOrderBy(OrderByClause orderBy){
		this.orderByClause = orderBy;
	}

	/**
	 * Replace the existing where clause.
	 * @param where
	 */
	public void replaceWhere(WhereClause where) {
		this.whereClause = where;
	}
}
