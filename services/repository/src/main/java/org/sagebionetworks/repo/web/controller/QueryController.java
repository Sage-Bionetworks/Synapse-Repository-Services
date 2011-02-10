package org.sagebionetworks.repo.web.controller;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.AnnotatableEntitiesAccessorImpl;
import org.sagebionetworks.repo.web.EntitiesAccessor;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.QueryStatement;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author deflaux
 * 
 */
@Controller
public class QueryController extends BaseController {

	// TODO @Autowired, no GAE references allowed in this class
	private static final DAOFactory DAO_FACTORY = new GAEJDODAOFactoryImpl();
	private DatasetDAO datasetDao = DAO_FACTORY.getDatasetDAO();
	private EntitiesAccessor<Dataset> datasetAccessor = new AnnotatableEntitiesAccessorImpl<Dataset>(
			datasetDao);

	/**
	 * @param query
	 * @param modelMap
	 * @return modelMap
	 * @throws DatastoreException
	 * @throws ParseException
	 * @throws NotFoundException
	 */
	@SuppressWarnings("unchecked")
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.QUERY, method = RequestMethod.GET)
	public String query(
			@RequestParam(value = ServiceConstants.QUERY_PARAM, required = true) String query,
			ModelMap modelMap) throws DatastoreException, ParseException,
			NotFoundException {

		QueryStatement stmt = new QueryStatement(query);

		if (null == stmt.getTableName()
				|| !stmt.getTableName().equals("dataset")) {
			throw new ParseException(
					"Queries are only supported for datasets at this time");
		}

		List<Dataset> datasets;

		// TODO talk to Bruce to see if he would prefer that this stuff is
		// transformed in to a query string that JDO understands

		if (null != stmt.getWhereField() && null != stmt.getWhereValue()) {
			// TODO only == is supported for InRangeHaving
			datasets = datasetAccessor.getInRangeHaving(stmt.getOffset(), stmt
					.getLimit(), stmt.getWhereField(), stmt.getWhereValue());
		} else if (null != stmt.getSortField()) {
			datasets = datasetAccessor.getInRangeSortedBy(stmt.getOffset(),
					stmt.getLimit(), stmt.getSortField(), stmt
							.getSortAcending());
		} else {
			datasets = datasetAccessor.getInRange(stmt.getOffset(), stmt
					.getLimit());
		}

		Integer i = 0;
		for (Dataset dataset : datasets) {
			ObjectMapper m = new ObjectMapper();
			Map<String, Object> props = m.convertValue(dataset, Map.class);
			Annotations annotations = datasetDao
					.getAnnotations(dataset.getId());
			props.putAll(annotations.getStringAnnotations());
			props.putAll(annotations.getFloatAnnotations());
			props.putAll(annotations.getDateAnnotations());
			modelMap.put(i.toString(), props);
			i++;

			// TODO filter out un-requested fields when we support more than
			// SELECT *
		}

		return "";

	}
}
