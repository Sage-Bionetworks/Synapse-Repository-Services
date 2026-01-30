package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.query.SelectItem;
import org.sagebionetworks.repo.model.grid.query.result.SelectColumn;

public class SelectSelectionElement implements SelectItemElement {

	public SelectSelectionElement(SelectItem item) { }
	
	public SelectSelectionElement() { }

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		StringJoiner joiner = new StringJoiner(",");
		
		context.getSelectedColumnIndices().forEach( colIndex -> 
			joiner.add(String.format("VALS->'$[%d]'", colIndex))
		);
		
		sqlBuilder.append(joiner.toString());
	}

	@Override
	public boolean isAggregate() {
		return false;
	}

	@Override
	public void setSelect(Context context, Long index, List<SelectColumn> selectColumns) {
		context.getSelectedColumnIndices().forEach(colIndex -> 
			selectColumns.add(new SelectColumn().setColumnName(context.getHeader().getOrderedColumns().get(colIndex).getName()))
		);
	}

}
