package org.sagebionetworks.table.query.model;

/**
 * This matches &ltsort specification&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class SortSpecification extends SQLElement {
	
    SortKey sortKey;
    OrderingSpecification orderingSpecification;
	public SortSpecification(SortKey sortKey,
			OrderingSpecification orderingSpecification) {
		this.sortKey = sortKey;
		this.orderingSpecification = orderingSpecification;
	}
	public SortKey getSortKey() {
		return sortKey;
	}
	public OrderingSpecification getOrderingSpecification() {
		return orderingSpecification;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		sortKey.toSql(builder, parameters);
		if(orderingSpecification != null){
			builder.append(" ");
			builder.append(orderingSpecification.name());
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(sortKey);
	}
}
