package org.sagebionetworks.table.query.model;

import java.util.Optional;

/**
 * This matches &lttable expression&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class TableExpression extends SQLElement implements HasAggregate, HasSingleTableName {

	FromClause fromClause;
	WhereClause whereClause;
	GroupByClause groupByClause;
	OrderByClause orderByClause;
	Pagination pagination;
	DefiningClause definingClause;

	public TableExpression(FromClause fromClause, WhereClause whereClause, GroupByClause groupByClause, OrderByClause orderByClause, Pagination pagination, DefiningClause definingClause) {
		this.fromClause = fromClause;
		this.whereClause = whereClause;
		this.groupByClause = groupByClause;
		this.orderByClause = orderByClause;
		this.pagination = pagination;
		this.definingClause = definingClause;
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

	public DefiningClause getDefiningClause() {
		return definingClause;
	}
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		fromClause.toSql(builder, parameters);
		if(definingClause != null) {
			builder.append(" ");
			definingClause.toSql(builder, parameters);
		}
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
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(fromClause, whereClause, groupByClause, orderByClause, pagination, definingClause);
	}

	@Override
	public boolean isElementAggregate() {
		return groupByClause != null;
	}

	/**
	 * Replace the existing pagination with the passed pagination.
	 * @param pagination
	 */
	public void replacePagination(Pagination replacement) {
		this.pagination = Replaceable.prepareToReplace(this.pagination, replacement, this);
	}
	
	/**
	 * Replace the existing group by with the passed group by.
	 * @param orderBy
	 */
	public void replaceOrderBy(OrderByClause replacement){
		this.orderByClause = Replaceable.prepareToReplace(this.orderByClause, replacement, this);
	}

	/**
	 * Replace the existing where clause.
	 * @param where
	 */
	public void replaceWhere(WhereClause replacement) {
		this.whereClause = Replaceable.prepareToReplace(this.whereClause, replacement, this);
	}
	
	/**
	 * Replace the existing GroupBy clause.
	 * @param replacement
	 */
	public void replaceGroupBy(GroupByClause replacement) {
		this.groupByClause = Replaceable.prepareToReplace(this.groupByClause, replacement, this);
	}
	
	/**
	 * Replace the existing DefiningClause.s
	 * @param definingClause
	 */
	public void replaceDefiningClause(DefiningClause definingClause) {
		this.definingClause = Replaceable.prepareToReplace(this.definingClause, definingClause, this);
	}

	@Override
	public Optional<String> getSingleTableName() {
		if(fromClause == null) {
			return Optional.empty();
		}
		return fromClause.getSingleTableName();
	}
}