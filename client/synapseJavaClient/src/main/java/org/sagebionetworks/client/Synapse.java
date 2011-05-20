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
			log.debug("Retrieved " + uri + " : " + results.toString(JSON_INDENT));
		}
		return results;
	}

	/**
	 * @param uri
	 * @param entity
	 * @return the updated entity
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
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws Exception
	 */
	public JSONObject updateEntity(String uri, JSONObject entity) throws Exception {
		
		JSONObject storedEntity = getEntity(uri);
		
		Iterator<String> keyIter = entity.keys();
		while(keyIter.hasNext()) {
			String key = keyIter.next();
			storedEntity.put(key, entity.get(key));
		}
		
		return doPut(uri, storedEntity);

	}
	/**
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws Exception
	 */
	public JSONObject doPut(String uri, JSONObject entity) throws Exception {
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
	 * @param uri
	 * @throws Exception
	 */
	public void deleteEntity(String uri)
			throws Exception {
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
