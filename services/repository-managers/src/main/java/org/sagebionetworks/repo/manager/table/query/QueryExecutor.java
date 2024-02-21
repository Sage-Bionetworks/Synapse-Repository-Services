package org.sagebionetworks.repo.manager.table.query;

import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TableIndexDAO;

public interface QueryExecutor {
	
	/**
	 * Executes the given query against the given index
	 * 
	 * @param indexDao
	 * @param query
	 * @return A row set representing the result of the query
	 */
	RowSet executeQuery(TableIndexDAO indexDao, QueryTranslator query);

}
