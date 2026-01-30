package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.query.SelectByName;
import org.sagebionetworks.repo.model.grid.query.SelectItem;
import org.sagebionetworks.repo.model.grid.query.result.SelectColumn;

public class SelectByNameElement implements SelectItemElement {
	
	private String columnName;

	public SelectByNameElement(SelectItem item) {
		this.columnName = ((SelectByName) item).getColumnName();
	}

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		Integer columnIndex = context.getColumnIndexForName(columnName);
		sqlBuilder.append(String.format("VALS->'$[%d]'", columnIndex));
	}

	@Override
	public boolean isAggregate() {
		return false;
	}

	@Override
	public void setSelect(Context context, Long index, List<SelectColumn> selectColumns) {
		selectColumns.add(new SelectColumn().setColumnName(columnName));
	}

}
