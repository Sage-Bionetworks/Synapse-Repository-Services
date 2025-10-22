package org.sagebionetworks.repo.manager.grid.internal.replica.view.query;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;

public class Context {

	private final GridHeader header;

	public Context(GridHeader header) {
		super();
		this.header = header;
	}

	public GridHeader getHeader() {
		return header;
	}

	public Integer getColumnIndexForName(String columnName) {
		return header.getOrderedColumns().stream().filter(c -> c.getName().equals(columnName))
				.map(c -> c.getVectorIndex()).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Column name not found: " + columnName));
	}

}
