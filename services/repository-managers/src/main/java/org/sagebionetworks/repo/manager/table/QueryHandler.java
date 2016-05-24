package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.table.cluster.SqlQuery;

public class QueryHandler {
	
	private final SqlQuery query;
	private final RowHandler handler;

	public QueryHandler(SqlQuery query, RowHandler handler) {
		this.query = query;
		this.handler = handler;
	}

	public SqlQuery getQuery() {
		return query;
	}

	public RowHandler getHandler() {
		return handler;
	}
}