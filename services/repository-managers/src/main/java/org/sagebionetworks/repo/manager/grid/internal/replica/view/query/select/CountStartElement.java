package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.query.SelectItem;
import org.sagebionetworks.repo.model.grid.query.result.SelectColumn;

public class CountStartElement implements SelectItemElement {

	public CountStartElement(SelectItem item) {
	}

	public CountStartElement() {
	}

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		sqlBuilder.append(" COUNT(*) AS C");
	}

	@Override
	public boolean isAggregate() {
		return true;
	}

	@Override
	public void setSelect(GridHeader header, Long index, List<SelectColumn> selectColumns) {
		selectColumns.add(new SelectColumn().setColumnIndex(index).setColumnName("count"));
	}

}
