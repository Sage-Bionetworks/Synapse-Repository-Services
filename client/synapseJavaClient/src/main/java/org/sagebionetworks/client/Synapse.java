package org.sagebionetworks.client;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * Low-level Java Client API for Synapse REST APIs
 */
public class Synapse {

	private static final Logger log = Logger.getLogger(Synapse.class.getName());

	private static final int JSON_INDENT = 2;

	private String serviceEndpoint;

	private static final String QUERY_URI = "/query?query=";
	private static final Map<String, String> defaultGETDELETEHeaders;
	private static final Map<String, String> defaultPOSTPUTHeaders;

	static {
		Map<String, String> readOnlyHeaders = new HashMap<String, String>();
		readOnlyHeaders.put("Accept", "application/json");
		defaultGETDELETEHeaders = Collections.unmodifiableMap(readOnlyHeaders);
		Map<String, String> readWriteHeaders = new HashMap<String, String>();
		readWriteHeaders.putAll(readOnlyHeaders);
		readWriteHeaders.put("Content-Type", "application/json");
		defaultPOSTPUTHeaders = Collections.unmodifiableMap(readWriteHeaders);
	}

	/**
	 * @param serviceEndpoint
	 */
	public Synapse(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
	}

	/**
	 * Create a new dataset, layer, etc ...
	 * 
	 * @param uri
	 * @param entity
	 * @return the newly created entity
	 * @throws Exception
	 */
	public JSONObject createEntity(String uri, JSONObject entity)
			throws Exception {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
		URL requestUrl = new URL(serviceEndpoint + uri);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);
		return dispatchRequest(requestUrl, "POST", entity.toString(),
				requestHeaders);
	}

	/**
	 * Get a dataset, layer, preview, annotations, etc...
	 * 
	 * @param uri
	 * @return the retrieved entity
	 * @throws Exception
	 */
	public JSONObject getEntity(String uri) throws Exception {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}

		URL requestUrl = new URL(serviceEndpoint + uri);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);

		return dispatchRequest(requestUrl, "GET", null, requestHeaders);
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * This convenience method first grabs a copy of the currently stored
	 * entity, then overwrites fields from the entity passed in on top of the
	 * stored entity we retrieved and then PUTs the entity. This essentially
	 * does a partial update from the point of view of the user of this API.
	 * 
	 * Note that users of this API may want to inspect what they are overwriting
	 * before they do so. Another approach would be to do a GET, display the
	 * field to the user, allow them to edit the fields, and then do a PUT.
	 * 
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public JSONObject updateEntity(String uri, JSONObject entity)
			throws Exception {

		JSONObject storedEntity = getEntity(uri);

		Iterator<String> keyIter = entity.keys();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			storedEntity.put(key, entity.get(key));
		}

		return putEntity(uri, storedEntity);

	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws Exception
	 */
	public JSONObject putEntity(String uri, JSONObject entity) throws Exception {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}

		URL requestUrl = new URL(serviceEndpoint + uri);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);
		requestHeaders.put("ETag", entity.getString("etag"));

		return dispatchRequest(requestUrl, "PUT", entity.toString(),
				requestHeaders);
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param uri
	 * @throws Exception
	 */
	public void deleteEntity(String uri) throws Exception {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}

		URL requestUrl = new URL(serviceEndpoint + uri);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);

		HttpClientHelper.performRequest(requestUrl, "DELETE", null,
				requestHeaders);
		return;
	}

	/**
	 * Perform a query
	 * 
	 * @param query
	 *            the query to perform
	 * @return the query result
	 * @throws Exception
	 */
	public JSONObject query(String query) throws Exception {
		if (null == query) {
			throw new IllegalArgumentException("must provide a query");
		}

		URL requestUrl = new URL(serviceEndpoint + QUERY_URI
				+ URLEncoder.encode(query, "UTF-8"));
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);

		return dispatchRequest(requestUrl, "GET", null, requestHeaders);
	}

	/**
	 * Convert exceptions emanating from the service to
	 * Synapse[User|Service]Exception but let all other types of exceptions
	 * bubble up as usual
	 * 
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return
	 * @throws Exception
	 */
	private JSONObject dispatchRequest(URL requestUrl, String requestMethod,
			String requestContent, Map<String, String> requestHeaders)
			throws Exception {

		JSONObject results = null;
		try {
			String response = HttpClientHelper.performRequest(requestUrl,
					requestMethod, requestContent, requestHeaders);
			if (null != response) {
				results = new JSONObject(response);
				if (log.isDebugEnabled()) {
					log.debug(requestMethod + " " + requestUrl + " : "
							+ results.toString(JSON_INDENT));
				}
			}
		} catch (HttpClientHelperException e) {
			// Well-handled server side exceptions come back as JSON, attempt to
			// deserialize and convert the error
			int statusCode = 500; // assume a service exception
			try {
				statusCode = e.getMethod().getStatusCode();
				String response = e.getMethod().getResponseBodyAsString();
				results = new JSONObject(response);
				if (log.isDebugEnabled()) {
					log.debug("Retrieved " + requestUrl + " : "
							+ results.toString(JSON_INDENT));
				}
			} catch (Exception conversionException) {
				// We could not convert it, just re-throw the original exception
				throw e;
			}

			if ((400 <= statusCode) && (500 > statusCode)) {
				throw new SynapseUserException("User Error(" + statusCode
						+ "): " + results.getString("reason"));
			}
			throw new SynapseServiceException("Service Error(" + statusCode
					+ "): " + results.getString("reason"));
		} // end catch

		return results;
	}
}
