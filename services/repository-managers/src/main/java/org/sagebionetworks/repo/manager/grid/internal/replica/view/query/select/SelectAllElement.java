package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

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
		StringJoiner joiner = new StringJoiner(",");
		for (int i=0; i<context.getHeader().getOrderedColumns().size(); i++) {
			joiner.add(String.format("VALS->'$[%d]'", i));
		}
		sqlBuilder.append(joiner.toString());
	}

	@Override
	public boolean isAggregate() {
		return false;
	}

	@Override
	public void setSelect(Context context, Long index, List<SelectColumn> selectColumns) {
		selectColumns.addAll(context.getHeader().getOrderedColumns().stream()
			.map(oc -> new SelectColumn().setColumnName(oc.getName()))
			.collect(Collectors.toList())
		);
	}
	
	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		return getClass() == obj.getClass();
	}

	@Override
	public String toString() {
		return "SelectAllElement []";
	}

}
