package org.sagebionetworks.repo.manager.grid.internal.replica.view.query;

import java.util.Map;

public class ColumnNameElement implements Element {

	private String columnName;

	public ColumnNameElement(String columnName) {
		this.columnName = columnName;
	}


	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		// TODO Auto-generated method stub
		
	}

}
