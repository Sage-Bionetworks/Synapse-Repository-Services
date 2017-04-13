package org.sagebionetworks.repo.model.query.jdo;

import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.query.BasicQuery;

public class NodeQueryDaoV2Impl implements NodeQueryDaoV2 {

	@Override
	public NodeQueryResults executeQuery(BasicQuery query,
			Set<Long> benefactorIds) throws DatastoreException {
		// this method uses the converted from of the query.
		query = BasicQueryUtils.convertFromToExpressions(query);
		return null;
	}


}
