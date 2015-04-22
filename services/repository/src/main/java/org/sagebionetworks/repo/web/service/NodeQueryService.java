package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeQueryService {

	QueryResults query(Long userId, String query, HttpServletRequest request)
			throws DatastoreException, ParseException, NotFoundException, UnauthorizedException;

	/**
	 * Executes a query and includes the annotations for each entity.
	 */
	QueryResults executeQueryWithAnnotations(Long userId, BasicQuery query, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException, ParseException;
	
	/**
	 * Structured query.
	 * @param userId
	 * @param query
	 * @return
	 * @throws NotFoundException 
	 */
	EntityQueryResults structuredQuery(Long userId, EntityQuery query) throws NotFoundException;
}
