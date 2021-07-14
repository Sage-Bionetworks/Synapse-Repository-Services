package org.sagebionetworks.table.query.model;

public class OrderByClause extends SQLElement {
	
	SortSpecificationList sortSpecificationList;

	public OrderByClause(SortSpecificationList sortSpecificationList) {
		super();
		this.sortSpecificationList = sortSpecificationList;
	}

	public SortSpecificationList getSortSpecificationList() {
		return sortSpecificationList;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("ORDER BY ");
		sortSpecificationList.toSql(builder, parameters);
	}

	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(sortSpecificationList);
	}
}
