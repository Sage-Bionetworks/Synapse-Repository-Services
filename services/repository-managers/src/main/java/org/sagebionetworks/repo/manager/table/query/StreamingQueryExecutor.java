package org.sagebionetworks.repo.manager.table.query;

import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TableIndexDAO;

/**
 * Query executor that won't include the list of results in memory but will just use the given row handler to process row by row
 */
public class StreamingQueryExecutor implements QueryExecutor {
	
	private RowHandler rowHandler;

	public StreamingQueryExecutor(RowHandler rowHandler) {
		this.rowHandler = rowHandler;
	}

	@Override
	public RowSet executeQuery(TableIndexDAO indexDao, QueryTranslator query) {
		indexDao.queryAsStream(query, rowHandler);
		
		return new RowSet()
			.setTableId(query.getSingleTableIdOptional().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT))	
			.setHeaders(query.getSelectColumns());
	}

}
