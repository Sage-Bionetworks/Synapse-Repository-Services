package org.sagebionetworks.client;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.utils.HttpClientHelper;

/**
 *
 */
public class Synapse {

	private static final Logger log = Logger.getLogger(Synapse.class.getName());

	private static final int JSON_INDENT = 2;

	private String serviceEndpoint;

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
		String response = HttpClientHelper.performRequest(requestUrl, "POST",
				entity.toString(), requestHeaders);
		JSONObject results = new JSONObject(response);
		if (log.isDebugEnabled()) {
			log.debug("Created " + uri + " : " + results.toString(JSON_INDENT));
		}
		return results;
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

		String response = HttpClientHelper.performRequest(requestUrl, "GET",
				null, requestHeaders);
		JSONObject results = new JSONObject(response);
		if (log.isDebugEnabled()) {
			log.debug("Retrieved " + uri + " : "
					+ results.toString(JSON_INDENT));
		}
		return results;
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

		String response = HttpClientHelper.performRequest(requestUrl, "PUT",
				entity.toString(), requestHeaders);
		JSONObject results = new JSONObject(response);
		if (log.isDebugEnabled()) {
			log.debug("Updated " + uri + " : " + results.toString(JSON_INDENT));
		}
		return results;
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

}
