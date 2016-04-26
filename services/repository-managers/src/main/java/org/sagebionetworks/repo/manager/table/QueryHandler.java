package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.table.cluster.SqlQuery;

public class QueryHandler {
	
	private final SqlQuery query;
	private final RowAndHeaderHandler handler;

	public QueryHandler(SqlQuery query, RowAndHeaderHandler handler) {
		this.query = query;
		this.handler = handler;
	}

	public SqlQuery getQuery() {
		return query;
	}

	public RowAndHeaderHandler getHandler() {
		return handler;
	}
}