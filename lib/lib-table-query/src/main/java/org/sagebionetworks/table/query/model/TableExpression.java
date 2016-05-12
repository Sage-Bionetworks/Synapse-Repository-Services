package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;

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
}
