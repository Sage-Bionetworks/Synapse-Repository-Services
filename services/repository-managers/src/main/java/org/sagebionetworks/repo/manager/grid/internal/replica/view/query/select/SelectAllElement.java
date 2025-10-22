package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.query.SelectItem;
import org.sagebionetworks.repo.model.grid.query.result.SelectColumn;

public class SelectAllElement implements SelectItemElement {

	public SelectAllElement(SelectItem item) {
	}

	public SelectAllElement() {
	}

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		sqlBuilder.append(" *");
	}

	@Override
	public boolean isAggregate() {
		return false;
	}

	@Override
	public void setSelect(GridHeader header, Long index, List<SelectColumn> selectColumns) {
		selectColumns.addAll(header.getOrderedColumns().stream().map(
				oc -> new SelectColumn().setColumnName(oc.getName()).setColumnIndex(Long.valueOf(oc.getVectorIndex())))
				.collect(Collectors.toList()));
	}

}
