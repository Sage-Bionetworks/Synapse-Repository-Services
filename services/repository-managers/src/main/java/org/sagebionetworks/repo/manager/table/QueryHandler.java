package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.table.cluster.QueryTranslator;

public class QueryHandler {
	
	private final QueryTranslator query;
	private final RowHandler handler;

	public QueryHandler(QueryTranslator query, RowHandler handler) {
		this.query = query;
		this.handler = handler;
	}

	public QueryTranslator getQuery() {
		return query;
	}

	public RowHandler getHandler() {
		return handler;
	}
}