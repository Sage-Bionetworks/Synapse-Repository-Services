package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor.SQLClause;
import org.sagebionetworks.table.query.model.visitors.Visitor;

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

	@Override
	public void toSql(StringBuilder builder) {
		builder.append("ORDER BY ");
		sortSpecificationList.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, sortSpecificationList);
	}
}
