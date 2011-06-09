package org.sagebionetworks.repo.web;

import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;

/**
 * Utility for creating various types of queries.
 * @author jmhill
 *
 */
public class QueryUtils {
	
	/**
	 * Create a query to find the paginated children of a the given parent that are of 
	 * the given type.
	 * @param parentId
	 * @param paging
	 * @param type
	 * @return
	 */
	public static BasicQuery createChildrenOfTypePaginated(String parentId,
			PaginatedParameters paging, ObjectType type) {
		BasicQuery query = new BasicQuery();
		// We want all children
		query.setLimit(paging.getLimit());
		query.setOffset(paging.getOffset()-1);
		query.setSort(paging.getSortBy());
		query.setAscending(paging.getAscending());
		query.setFrom(type);
		query.addExpression(new Expression(new CompoundId(null, NodeConstants.COL_PARENT_ID), Compartor.EQUALS, Long.parseLong(parentId)));
		return query;
	}
	
	/**
	 * Query to find all of the entities of a given type, in a paginated result.
	 * @param paging
	 * @param type
	 * @return
	 */
	public static BasicQuery createFindPaginagedOfType(PaginatedParameters paging, ObjectType type) {
		BasicQuery query = new BasicQuery();
		query.setFrom(type);
		query.setLimit(paging.getLimit());
		query.setOffset(paging.getOffset()-1);
		query.setAscending(paging.getAscending());
		query.setSort(paging.getSortBy());
		return query;
	}

}
