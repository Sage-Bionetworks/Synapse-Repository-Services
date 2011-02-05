package org.sagebionetworks.repo.web.controller;

import java.io.StringReader;
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
import org.sagebionetworks.repo.queryparser.QueryNode;
import org.sagebionetworks.repo.queryparser.QueryParser;
import org.sagebionetworks.repo.web.AnnotatableEntitiesAccessorImpl;
import org.sagebionetworks.repo.web.EntitiesAccessor;
import org.sagebionetworks.repo.web.NotFoundException;
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

		// TODO stash this in ThreadLocal because its expensive to create and
		// not threadsafe
		QueryParser parser = new QueryParser(new StringReader(query));
		QueryNode parseTree = (QueryNode) parser.Start();

		String tableId = null;
		String whereField = null;
		Object whereValue = null;

		// TODO move this into a helper function because its going to get way
		// more complicated
		for (int i = 0; i < parseTree.jjtGetNumChildren(); i++) {
			QueryNode node = (QueryNode) parseTree.jjtGetChild(i);
			switch (node.getId()) {
			case QueryParser.JJTWHERE:
				whereField = (String) ((QueryNode) node.jjtGetChild(0))
						.jjtGetValue();
				whereValue = ((QueryNode) node.jjtGetChild(1).jjtGetChild(0))
						.jjtGetValue();
				break;
			case QueryParser.JJTTABLENAME:
				tableId = (String) node.jjtGetValue();
				break;
			}
		}

		// TODO ServiceConstants.validatePaginationParams(offset, limit);

		if (null == tableId || !tableId.equals("dataset")) {
			throw new ParseException(
					"Queries are only supported for datasets at this time");
		}

		List<Dataset> datasets;

		// TODO talk to Bruce to see if he would prefer that this stuff is
		// transformed in to a query string that JDO understands

		if (null != whereField && null != whereValue) {
			// TODO only == is supported for InRangeHaving
			datasets = datasetAccessor.getInRangeHaving(1, Integer.MAX_VALUE,
					whereField, whereValue);
		} else {
			datasets = datasetAccessor.getInRange(1, Integer.MAX_VALUE);
		}
		// TODO add ORDER BY, LIMIT, OFFSET, ASC, DESC to the query parser
		// datasets = datasetAccessor.getInRangeSortedBy(1, Integer.MAX_VALUE,
		// whereField, true);

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

		modelMap
				.put("Note to John",
						"I can change the structure of this.  Just let me know what you want.");
		return "";

	}

}
