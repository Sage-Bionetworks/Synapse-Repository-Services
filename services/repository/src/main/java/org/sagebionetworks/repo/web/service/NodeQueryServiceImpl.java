package org.sagebionetworks.repo.web.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.util.QueryTranslator;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.query.QueryStatement;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeQueryServiceImpl implements NodeQueryService {

	private static final String[] EXCLUDED_DATASET_PROPERTIES = {
			"uri", "etag", "annotations", "layer"
		};

	private static final String[] EXCLUDED_LAYER_PROPERTIES = {
			"uri", "etag", "annotations", "preview", "locations"
		};

	private static final Map<String, Set<String>> EXCLUDED_PROPERTIES;	
	static {
		Set<String> datasetProperties = new HashSet<String>();
		datasetProperties.addAll(Arrays.asList(EXCLUDED_DATASET_PROPERTIES));
		Set<String> layerProperties = new HashSet<String>();
		layerProperties.addAll(Arrays.asList(EXCLUDED_LAYER_PROPERTIES));
		Map<String, Set<String>> excludedProperties = new HashMap<String, Set<String>>();
		excludedProperties.put("dataset", datasetProperties);
		excludedProperties.put("layer", layerProperties);
		EXCLUDED_PROPERTIES = Collections.unmodifiableMap(excludedProperties);
	}

	@Autowired
	private NodeQueryDao nodeQueryDao;
	@Autowired
	private UserManager userManager;

	@Override
	public QueryResults query(String userId, String query, HttpServletRequest request)
			throws DatastoreException, ParseException, NotFoundException, UnauthorizedException {

		// Parse and validate the query
		QueryStatement stmt = new QueryStatement(query);
		// Convert from a query statement to a basic query
		BasicQuery basic = QueryTranslator.createBasicQuery(stmt);
		QueryResults results = executeQueryWithAnnotations(userId, basic, request);
		results.setResults(formulateResult(stmt, results.getResults()));
		return results;
	}

	@Override
	public QueryResults executeQueryWithAnnotations(String userId, BasicQuery query, HttpServletRequest request)
			throws DatastoreException, NotFoundException, UnauthorizedException, ParseException {
		if (query == null) {
			throw new IllegalArgumentException("Query cannot be null");
		}
		UserInfo userInfo = userManager.getUserInfo(userId);
		NodeQueryResults results = nodeQueryDao.executeQuery(query, userInfo);
		return new QueryResults(results.getAllSelectedData(), results.getTotalNumberOfResults());
	}

	private List<Map<String, Object>> formulateResult(QueryStatement stmt, List<Map<String, Object>> rows) {
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> row : rows) {
			results.add(formulateResult(stmt, row));
		}
		return results;
	}

	private Map<String, Object> formulateResult(QueryStatement stmt,
			Map<String, Object> fields) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String field : fields.keySet()) {
			if (!EXCLUDED_PROPERTIES.get("dataset").contains(field)) {
				result.put(stmt.getTableName() + "." + field, fields.get(field));
			}
		}
		return result;
	}
}
