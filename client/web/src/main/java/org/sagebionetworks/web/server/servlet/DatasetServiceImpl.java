package org.sagebionetworks.web.server.servlet;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.DatasetService;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.server.ServerConstants;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.PaginatedDatasets;
import org.sagebionetworks.web.shared.TableResults;
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
	public static final String PATH_ALL_DATASETS = "dataset?offset={"
			+ KEY_OFFSET + "}&limit={" + KEY_LIMIT + "}";
	
	public static final String PATH_ALL_DATASETS_SORTING = "dataset?offset={"
		+ KEY_OFFSET + "}&limit={" + KEY_LIMIT + "}&sort={" + KEY_SORT + "}&ascending={" + KEY_ASCND + "}";
	
	public static final String KEY_DATASET_ID = "idKey";
	public static final String PATH_DATASET = "dataset/{"+KEY_DATASET_ID+"}";
	public static final String PATH_DATASET_ANNOTATIONS = "dataset/{"+KEY_DATASET_ID+"}/annotations";

	public static final String KEY_LAYER_ID = "idLayerKey";
	public static final String PATH_LAYER = "dataset/{"+KEY_DATASET_ID+"}/layer/{"+ KEY_LAYER_ID +"}";
	public static final String PATH_LAYER_PREVIEW = "dataset/{"+KEY_DATASET_ID+"}/layer/{"+ KEY_LAYER_ID +"}/preview";
	public static final String PATH_LAYER_PREVIEW_AS_MAP = "dataset/{"+KEY_DATASET_ID+"}/layer/{"+ KEY_LAYER_ID +"}/previewAsMap";
	public static final String PATH_LAYER_DOWNLOAD_S3 = "dataset/{"+KEY_DATASET_ID+"}/layer/{"+ KEY_LAYER_ID +"}/awsS3Location";
	
	public static final String POST_DATASET = "dataset";
	
	public static final String POST_DATASET_ANNOTATOINS = "dataset/{"+KEY_DATASET_ID+"}/annotations";

	
	private RestTemplateProvider templateProvider = null;
	private ServiceUrlProvider urlProvider;

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
	 * Injected vid Gin
	 * @param provider
	 */
	@Inject
	public void setServiceUrlProvider(ServiceUrlProvider provider){
		this.urlProvider = provider;
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
		if (urlProvider == null)
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
		builder.append(urlProvider.getBaseUrl());
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
		builder.append(urlProvider.getBaseUrl());
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
	public Annotations getDatasetAnnotations(String id) {
		// First make sure the service is ready to go.
		validateService();
		// Build up the path
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl());
		builder.append(PATH_DATASET_ANNOTATIONS);
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
		ResponseEntity<Annotations> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, Annotations.class, map);

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
		// First make sure the service is ready to go.
		validateService();
		// Build up the path
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl());
		builder.append(PATH_LAYER);
		// the values to the keys
		Map<String, String> map = new HashMap<String, String>();
		map.put(KEY_DATASET_ID, datasetId);
		map.put(KEY_LAYER_ID, layerId);
		String url = builder.toString();
		logger.info("GET: " + url);
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		// Make the actual call.
		ResponseEntity<Layer> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, Layer.class, map);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}

	@Override
	public LayerPreview getLayerPreview(String datasetId, String layerId) {
		// First make sure the service is ready to go.
		validateService();
		// Build up the path
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl());
		builder.append(PATH_LAYER_PREVIEW);
		// the values to the keys
		Map<String, String> map = new HashMap<String, String>();
		map.put(KEY_DATASET_ID, datasetId);
		map.put(KEY_LAYER_ID, layerId);
		String url = builder.toString();
		logger.info("GET: " + url);
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		// Make the actual call.
		ResponseEntity<LayerPreview> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, LayerPreview.class, map);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}

	@Override
	public TableResults getLayerPreviewMap(String datasetId, String layerId) {
		// First make sure the service is ready to go.
		validateService();
		// Build up the path
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl());
		builder.append(PATH_LAYER_PREVIEW_AS_MAP);
		// the values to the keys
		Map<String, String> map = new HashMap<String, String>();
		map.put(KEY_DATASET_ID, datasetId);
		map.put(KEY_LAYER_ID, layerId);
		String url = builder.toString();
		logger.info("GET: " + url);
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		// Make the actual call.
		ResponseEntity<TableResults> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, TableResults.class, map);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}

	@Override
	public DownloadLocation getLayerDownloadLocation(String datasetId, String layerId) {
		// First make sure the service is ready to go.
		validateService();
		// Build up the path
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl());
		builder.append(PATH_LAYER_DOWNLOAD_S3);
		// the values to the keys
		Map<String, String> map = new HashMap<String, String>();
		map.put(KEY_DATASET_ID, datasetId);
		map.put(KEY_LAYER_ID, layerId);
		String url = builder.toString();
		logger.info("GET: " + url);
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		// Make the actual call.
		ResponseEntity<DownloadLocation> response = templateProvider.getTemplate().exchange(url, HttpMethod.GET, entity, DownloadLocation.class, map);

		if (response.getStatusCode() == HttpStatus.OK) {
			return response.getBody();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}

	@Override
	public String createDataset(Dataset toCreate) {
		if(toCreate == null) throw new IllegalArgumentException("Dataset cannot be null");
		String url = urlProvider.getBaseUrl() + POST_DATASET;;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Dataset> entity = new HttpEntity<Dataset>(toCreate, headers);
		// Make the actual call.
		ResponseEntity<Dataset> response = templateProvider.getTemplate().exchange(url, HttpMethod.POST, entity, Dataset.class);
		if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
			return response.getBody().getId();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}

	@Override
	public String updateDatasetAnnotations(String datasetId,
			Annotations newAnnotations) {
		if(datasetId == null) throw new IllegalArgumentException("Dataset id cannot be null");
		if(newAnnotations == null) throw new IllegalArgumentException("Annotations cannot be null");
		if(newAnnotations.getEtag() == null) throw new IllegalArgumentException("The eTag cannot be null for the annotations.  Get a valid eTab by calling getDatasetAnnotations()");
		StringBuilder builder = new StringBuilder();
		builder.append(urlProvider.getBaseUrl());
		builder.append(POST_DATASET_ANNOTATOINS);
		// the values to the keys
		Map<String, String> map = new HashMap<String, String>();
		map.put(KEY_DATASET_ID, datasetId);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("ETag", newAnnotations.getEtag());
		HttpEntity<Annotations> entity = new HttpEntity<Annotations>(newAnnotations, headers);
		// Make the actual call.
		ResponseEntity<Annotations> response = templateProvider.getTemplate().exchange(builder.toString(), HttpMethod.PUT, entity, Annotations.class, map);
		if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
			return response.getBody().getEtag();
		} else {
			// TODO: better error handling
			throw new UnknownError("Status code:"
					+ response.getStatusCode().value());
		}
	}
	
	@Override
	protected void checkPermutationStrongName() throws SecurityException {
		// No-opp here allows us to make RPC calls for integration testing.
	}

	
}
