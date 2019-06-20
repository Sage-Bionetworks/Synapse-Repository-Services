package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;
import org.sagebionetworks.repo.web.DeprecatedServiceException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * This service is deprecated and will be removed.
 */
@Deprecated
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class QueryController {
	
	/**
	 * Post a structured <a href="${org.sagebionetworks.repo.model.entity.query.EntityQuery}">EntityQuery</a> and get a structured
	 * <a href="${org.sagebionetworks.repo.model.entity.query.EntityQueryResults}">EntityQueryResults</a>.
	 * @param userId
	 * @param query
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws ParseException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.QUERY, method = RequestMethod.POST)
	public @ResponseBody EntityQueryResults structuredQuery(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody EntityQuery query,
			HttpServletRequest request)
			throws DeprecatedServiceException {
		throw new DeprecatedServiceException();
	}

	/**
	 * Provides a 'SQL' like query language and produces a dynamic map of results. See:
	 * <a href="https://sagebionetworks.jira.com/wiki/display/PLFM/Repository+Service+API#RepositoryServiceAPI-QueryAPI">Query API</a>
	 * @param userId
	 * @param query
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws ParseException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.QUERY, method = RequestMethod.GET)
	public @ResponseBody QueryResults query(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = ServiceConstants.QUERY_PARAM, required = true) String query,
			HttpServletRequest request)
			throws DeprecatedServiceException {
		throw new DeprecatedServiceException();
	}
}
