package org.sagebionetworks.repo.web.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayerDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.util.SchemaHelper;
import org.sagebionetworks.repo.web.AnnotatableEntitiesAccessorImpl;
import org.sagebionetworks.repo.web.EntitiesAccessor;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.QueryStatement;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
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

	private DatasetDAO dao;

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

	private void checkAuthorization(String userId) {
		BaseDAO<Dataset> dao = getDaoFactory().getDatasetDAO(userId);
		setDao(dao);
	}

	/**
	 * @param dao
	 */
	public void setDao(BaseDAO<Dataset> dao) {
		this.dao = (DatasetDAO) dao;
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
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.QUERY_PARAM, required = true) String query,
			HttpServletRequest request) throws DatastoreException,
			ParseException, NotFoundException, UnauthorizedException {

		checkAuthorization(userId);

		/**
		 * Parse and validate the query
		 */
		QueryStatement stmt = null;
		try {
			stmt = new QueryStatement(URLDecoder.decode(query, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE,
					"Something is really messed up if we don't support UTF-8",
					e);
		}

		if (stmt.getTableName().equals("dataset")) {
			return performDatasetQuery(stmt);
		} else if (stmt.getTableName().equals("layer")) {
			if (null == stmt.getWhereTable()
					|| null == stmt.getWhereField()
					|| !(stmt.getWhereTable().equals("dataset") && stmt
							.getWhereField().equals("id"))) {
				throw new ParseException(
						"Layer queries must include a 'WHERE dataset.id == <the id>' clause");
			}
			return performLayerQuery(stmt);
		} else {
			throw new ParseException(
					"Queries are only supported for datasets and layers at this time");
		}
	}

	/**
	 * @return the schema
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.QUERY + UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getQuerySchema() throws DatastoreException {
		return SchemaHelper.getSchema(QueryResults.class);
	}

	@SuppressWarnings("unchecked")
	private QueryResults performDatasetQuery(QueryStatement stmt)
			throws DatastoreException, NotFoundException {

		EntitiesAccessor<Dataset> accessor = new AnnotatableEntitiesAccessorImpl<Dataset>();
		accessor.setDao(dao);

		/**
		 * Perform the query
		 * 
		 * TODO talk to Bruce to see if he would prefer that this stuff is
		 * transformed in to a query string that JDO understands
		 */
		List<Dataset> entities;
		if (null != stmt.getWhereField() && null != stmt.getWhereValue()) {
			// TODO only == is supported for InRangeHaving
			entities = accessor.getInRangeHaving(stmt.getOffset(), stmt
					.getLimit(), stmt.getWhereField(), stmt.getWhereValue());
		} else if (null != stmt.getSortField()) {
			entities = accessor.getInRangeSortedBy(stmt.getOffset(), stmt
					.getLimit(), stmt.getSortField(), stmt.getSortAcending());
		} else {
			entities = accessor.getInRange(stmt.getOffset(), stmt.getLimit());
		}

		/**
		 * Get the total number of results for this query
		 * 
		 * TODO we don't have this for queries with a WHERE clause
		 */
		Integer totalNumberOfResults = dao.getCount();

		/**
		 * Format the query result
		 */
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for (Dataset dataset : entities) {

			Map<String, Object> result = OBJECT_MAPPER.convertValue(dataset,
					Map.class);
			Annotations annotations = dao.getAnnotations(dataset.getId());
			result.putAll(annotations.getStringAnnotations());
			result.putAll(annotations.getDoubleAnnotations());
			result.putAll(annotations.getLongAnnotations());
			result.putAll(annotations.getDateAnnotations());

			results.add(formulateResult(stmt, result));
		}

		return new QueryResults(results, totalNumberOfResults);
	}

	@SuppressWarnings("unchecked")
	private QueryResults performLayerQuery(QueryStatement stmt)
			throws DatastoreException, NotFoundException {

		InputDataLayerDAO layerDao = dao.getInputDataLayerDAO(stmt
				.getWhereValue().toString());
		EntitiesAccessor<InputDataLayer> accessor = new AnnotatableEntitiesAccessorImpl<InputDataLayer>();
		accessor.setDao(layerDao);

		/**
		 * Perform the query
		 * 
		 * TODO talk to Bruce to see if he would prefer that this stuff is
		 * transformed in to a query string that JDO understands
		 * 
		 * TODO support WHERE datasetId == 123 AND foo == var
		 */
		List<InputDataLayer> entities;
		if (null != stmt.getSortField()) {
			entities = accessor.getInRangeSortedBy(stmt.getOffset(), stmt
					.getLimit(), stmt.getSortField(), stmt.getSortAcending());
		} else {
			entities = accessor.getInRange(stmt.getOffset(), stmt.getLimit());
		}

		/**
		 * Get the total number of results for this query
		 * 
		 * TODO we don't have this for queries with a WHERE clause
		 */
		Integer totalNumberOfResults = layerDao.getCount();

		/**
		 * Format the query result
		 */
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for (InputDataLayer layer : entities) {

			Map<String, Object> result = OBJECT_MAPPER.convertValue(layer,
					Map.class);

			Annotations annotations = layerDao.getAnnotations(layer.getId());
			result.putAll(annotations.getStringAnnotations());
			result.putAll(annotations.getDoubleAnnotations());
			result.putAll(annotations.getLongAnnotations());
			result.putAll(annotations.getDateAnnotations());

			results.add(formulateResult(stmt, result));
		}

		return new QueryResults(results, totalNumberOfResults);
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
