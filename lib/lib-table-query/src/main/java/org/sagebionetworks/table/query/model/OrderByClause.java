package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.ToSimpleSqlVisitor.SQLClause;

public class OrderByClause extends SQLElement {
	
	SortSpecificationList sortSpecificationList;

	public OrderByClause(SortSpecificationList sortSpecificationList) {
		super();
		this.sortSpecificationList = sortSpecificationList;
	}

	public SortSpecificationList getSortSpecificationList() {
		return sortSpecificationList;
	}

	public void visit(Visitor visitor) {
		visit(sortSpecificationList, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append("ORDER BY ");
		visitor.pushCurrentClause(SQLClause.ORDER_BY);
		visit(sortSpecificationList, visitor);
		visitor.popCurrentClause(SQLClause.ORDER_BY);
	}
}
