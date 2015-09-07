package org.sagebionetworks.repo.web;

import java.util.ArrayList;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.jdo.NodeField;

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
	 * @throws DatastoreException 
	 */
	public static BasicQuery createChildrenOfTypePaginated(String parentId,
			PaginatedParameters paging, EntityType type) throws DatastoreException {
		BasicQuery query = new BasicQuery();
		query.setSelect(new ArrayList<String>());
		query.getSelect().add(NodeField.ID.getFieldName());
		// We want all children
		query.setLimit(paging.getLimit());
		query.setOffset(paging.getOffset()-1);
		query.setSort(paging.getSortBy());
		query.setAscending(paging.getAscending());
		query.setFrom(type.name());
		query.addExpression(new Expression(new CompoundId(null, NodeConstants.COL_PARENT_ID), Comparator.EQUALS, KeyFactory.stringToKey(parentId)));
		return query;
	}
	
	/**
	 * Query to find all of the entities of a given type, in a paginated result.
	 * @param paging
	 * @param type
	 * @return
	 */
	public static BasicQuery createFindPaginagedOfType(PaginatedParameters paging, EntityType type) {
		BasicQuery query = new BasicQuery();
		query.setSelect(new ArrayList<String>());
		query.getSelect().add(NodeField.ID.getFieldName());
		query.setFrom(type.name());
		query.setLimit(paging.getLimit());
		query.setOffset(paging.getOffset()-1);
		query.setAscending(paging.getAscending());
		query.setSort(paging.getSortBy());
		return query;
	}

}
