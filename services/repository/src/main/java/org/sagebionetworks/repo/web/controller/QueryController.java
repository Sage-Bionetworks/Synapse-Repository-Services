package org.sagebionetworks.repo.web.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.query.QueryStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author deflaux
 * 
 */
@Controller
public class QueryController extends BaseController {

	private static final Logger log = Logger.getLogger(QueryController.class
			.getName());

	@Autowired
	GenericEntityController entityController;
	// Use a static instance of this per
	// http://wiki.fasterxml.com/JacksonBestPracticesPerformance
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String excludedDatasetProperties[] = { "uri", "etag",
			"annotations", "layer" };
	private static final String excludedLayerProperties[] = { "uri", "etag",
			"annotations", "preview", "locations" };
	private static final Map<String, Set<String>> EXCLUDED_PROPERTIES;

	static {
		Set<String> datasetProperties = new HashSet<String>();
		datasetProperties.addAll(Arrays.asList(excludedDatasetProperties));
		Set<String> layerProperties = new HashSet<String>();
		layerProperties.addAll(Arrays.asList(excludedLayerProperties));
		Map<String, Set<String>> excludedProperties = new HashMap<String, Set<String>>();
		excludedProperties.put("dataset", datasetProperties);
		excludedProperties.put("layer", layerProperties);
		EXCLUDED_PROPERTIES = Collections.unmodifiableMap(excludedProperties);
	}

	/**
	 * @param userId
	 * @param query
	 * @param request
	 * @return paginated results
	 * @throws DatastoreException
	 * @throws ParseException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.QUERY, method = RequestMethod.GET)
	public @ResponseBody
	QueryResults query(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.QUERY_PARAM, required = true) String query,
			HttpServletRequest request) throws DatastoreException,
			ParseException, NotFoundException, UnauthorizedException {

//		checkAuthorization(userId);

		/**
		 * Parse and validate the query
		 */
		QueryStatement stmt = new QueryStatement(query);

		// Convert from a query statement to a basic query
		BasicQuery basic = QueryTranslator.createBasicQuery(stmt);
		
		QueryResults results = entityController.executeQueryWithAnnotations(userId, basic, request);
		results.setResults(formulateResult(stmt, results.getResults()));
		return results;
	}

	/**
	 * Process all of the results.
	 * @param stmt
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> formulateResult(QueryStatement stmt, List<Map<String, Object>> rows) {
		List<Map<String, Object>> results = new ArrayList<Map<String,Object>>();
		for(Map<String, Object> row: rows){
			results.add(formulateResult(stmt, row));
		}
		return results;
	}

	private Map<String, Object> formulateResult(QueryStatement stmt,
			Map<String, Object> fields) {
		// TODO filter out un-requested fields when we support more than
		// SELECT *
		Map<String, Object> result = new HashMap<String, Object>();
		for (String field : fields.keySet()) {
			if (EXCLUDED_PROPERTIES.get("dataset").contains(field)) {
				// skip this
			} else {
				result
						.put(stmt.getTableName() + "." + field, fields
								.get(field));
			}
		}
		return result;
	}
	
}
