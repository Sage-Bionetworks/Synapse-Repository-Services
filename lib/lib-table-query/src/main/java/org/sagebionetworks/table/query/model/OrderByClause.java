package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.SQLElement.ColumnConvertor.SQLClause;

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
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		builder.append("ORDER BY ");
		if (columnConvertor != null) {
			columnConvertor.setCurrentClause(SQLClause.ORDER_BY);
		}
		sortSpecificationList.toSQL(builder, columnConvertor);
		if (columnConvertor != null) {
			columnConvertor.setCurrentClause(null);
		}
	}
	

}
