package org.sagebionetworks.web.server.servlet;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.NotImplementedException;
import org.sagebionetworks.web.client.DatasetService;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.PaginatedDatasets;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * The server-side implementation of the DatasetService. This serverlet will
 * communicate with the platform API via REST.
 * 
 * @author jmhill
 * 
 */
@SuppressWarnings("serial")
public class DatasetServiceImpl extends RemoteServiceServlet implements
		DatasetService {

	private static Logger logger = Logger.getLogger(DatasetServiceImpl.class
			.getName());

	public static final String KEY_OFFSET = "offestKey";
	public static final String KEY_LIMIT = "limitKey";
	public static final String KEY_SORT = "sortKey";
	public static final String KEY_ASCND = "ascendingKey";
	public static final String PATH_ALL_DATASETS = "repo/v1/dataset?offset={"
			+ KEY_OFFSET + "}&limit={" + KEY_LIMIT + "}";
	
	public static final String PATH_ALL_DATASETS_SORTING = "repo/v1/dataset?offset={"
		+ KEY_OFFSET + "}&limit={" + KEY_LIMIT + "}&sort={" + KEY_SORT + "}&ascending={" + KEY_ASCND + "}";
	
	public static final String KEY_DATASET_ID = "idKey";
	public static final String PATH_DATASET = "repo/v1/dataset/{"+KEY_DATASET_ID+"}";

	private RestTemplateProvider templateProvider = null;
	private String rootUrl = null;

	/**
	 * The rest template will be injected via Guice.
	 * 
	 * @param template
	 */
	@Inject
	public void setRestTemplate(RestTemplateProvider template) {
		this.templateProvider = template;
	}

	/**
	 * Injected via Guice from the ServerConstants.properties file.
	 * 
	 * @param url
	 */
	@Inject
	public void setRootUrl(
			@Named("org.sagebionetworks.rest.api.root.url") String url) {
		this.rootUrl = url;
		logger.info("rootUrl:" + this.rootUrl);

	}

	/**
	 * Validate that the service is ready to go. If any of the injected data is
	 * missing then it cannot run. Public for tests.
	 */
	public void validateService() {
		if (templateProvider == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.web.server.RestTemplateProvider was not injected into this service");
		if (templateProvider.getTemplate() == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.web.server.RestTemplateProvider returned a null template");
		if (rootUrl == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.rest.api.root.url was not set");
	}

	@Override
	public PaginatedDatasets getAllDatasets(int offset, int length, String sort, boolean ascending) {
		// First make sure the service is ready to go.
		validateService();
		// Build up the path
		StringBuilder builder = new StringBuilder();
		Map<String, String> map = new HashMap<String, String>();
		builder.append(rootUrl);
		if(sort != null){
			builder.append(PATH_ALL_DATASETS_SORTING);
			map.put(KEY_SORT, sort);
			map.put(KEY_ASCND, Boolean.valueOf(ascending).toString());
		}else{
			builder.append(PATH_ALL_DATASETS);
		}

		// the values to the keys
		map.put(KEY_OFFSET, Integer.toString(offset));
		map.put(KEY_LIMIT, Integer.toString(length));
		String url = builder.toString();
		logger.info("GET: " + url);
		PaginatedDatasets paginated = null;

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		
		// Get the string back
		ResponseEntity<String> testReponse = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, String.class, map);
		logger.info("Response Status: "+testReponse.getStatusCode());
		logger.info(testReponse.getBody());
		
		// Make the actual call.
		ResponseEntity<PaginatedDatasets> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, PaginatedDatasets.class, map);

		// ResponseEntity<PaginatedDatasets> response =
		// templateProvider.getTemplate().getForEntity(url,
		// PaginatedDatasets.class, map);
		if (response.getStatusCode() == HttpStatus.OK) {
			paginated = response.getBody();
			return paginated;
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}

	@Override
	public Dataset getDataset(String id) {
		// First make sure the service is ready to go.
		validateService();
		// Build up the path
		StringBuilder builder = new StringBuilder();
		builder.append(rootUrl);
		builder.append(PATH_DATASET);
		// the values to the keys
		Map<String, String> map = new HashMap<String, String>();
		map.put(KEY_DATASET_ID, id);
		String url = builder.toString();
		logger.info("GET: " + url);
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		// Make the actual call.
		ResponseEntity<Dataset> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, Dataset.class, map);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}

	@Override
	public Layer getLayer(String datasetId, String layerId) {
		throw new NotImplementedException();		
	}

}
