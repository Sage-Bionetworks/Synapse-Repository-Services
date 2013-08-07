package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeQueryService {

	QueryResults query(String userId, String query, HttpServletRequest request)
			throws DatastoreException, ParseException, NotFoundException, UnauthorizedException;

	/**
	 * Executes a query and includes the annotations for each entity.
	 */
	QueryResults executeQueryWithAnnotations(String userId, BasicQuery query, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException, ParseException;
}
