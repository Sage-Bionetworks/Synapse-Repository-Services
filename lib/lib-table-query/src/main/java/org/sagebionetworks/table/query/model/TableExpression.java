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

	public void visit(Visitor visitor) {
		visit(fromClause, visitor);
		if (whereClause != null) {
			visit(whereClause, visitor);
		}
		if (groupByClause != null) {
			visit(groupByClause, visitor);
		}
		if (orderByClause != null) {
			visit(orderByClause, visitor);
		}
		if (pagination != null) {
			visit(pagination, visitor);
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visit(fromClause, visitor);
		if(whereClause != null){
			visitor.append(" ");
			visit(whereClause, visitor);
		}
		if(groupByClause != null){
			visitor.append(" ");
			visit(groupByClause, visitor);
		}
		if(orderByClause != null){
			visitor.append(" ");
			visit(orderByClause, visitor);
		}
		if(pagination != null){
			visitor.append(" ");
			visit(pagination, visitor);
		}
	}
}
