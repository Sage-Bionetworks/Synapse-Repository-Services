package org.sagebionetworks.table.query.model;

import java.util.List;

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
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, sortSpecificationList);
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(sortSpecificationList);
	}
}
