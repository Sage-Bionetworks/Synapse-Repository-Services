package org.sagebionetworks.repo.manager.grid.internal.replica.view.query;

import java.util.Map;

import org.sagebionetworks.repo.model.grid.query.OrderByDirection;
import org.sagebionetworks.repo.model.grid.query.OrderByItem;
import org.sagebionetworks.util.ValidateArgument;

public class OrderByItemElement implements Element {
	
    private String columnName;
    private OrderByDirection direction;

	public OrderByItemElement(OrderByItem orderByItem) {
		ValidateArgument.required(orderByItem, "orderByItem");
		ValidateArgument.required(orderByItem.getColumnName(), "orderByItem.columnName");
		this.columnName = orderByItem.getColumnName();
		this.direction = orderByItem.getDirection();
	}

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		Integer columnIndex = context.getColumnIndexForName(columnName);
		
	}

}
