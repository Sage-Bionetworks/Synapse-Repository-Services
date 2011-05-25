package org.sagebionetworks.repo;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.utils.HttpClientHelper;

/**
 * WikiGenerator is used to auto-generate the wiki for the Platform Repository
 * Service.
 * 
 * All the log output goes to stdout. See generateRepositoryServiceWiki.sh for
 * how the log output is cleaned and turned into an actual wiki page. The reason
 * why this writes log output file instead of a normal output to stdout is
 * because I want to include the response headers logged by HttpClient and this
 * was a quick way to make that happen.
 * 
 * Also note that I originally wrote this against HtmlUnit so that I could
 * include a bit of testing to make sure the responses coming back were sane,
 * but HtmlUnit does not support PUT or DELETE.
 * 
 * {code} svn checkout
 * https://sagebionetworks.jira.com/svn/PLFM/trunk/tools/wikiutil cd wikiutil
 * ~/platform/trunk/tools/wikiutil>mvn clean compile
 * ~/platform/trunk/tools/wikiutil>./generateRepositoryServiceWiki.sh
 * http://localhost:8888 > wiki.txt {code}
 * 
 */
public class WikiGenerator {

	private static final Logger log = Logger.getLogger(WikiGenerator.class
			.getName());

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
	public WikiGenerator(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
	}

	/**
	 * @param uri
	 * @param wikiHeading
	 * @param wikiDetails
	 * @return the retrieved entity
	 * @throws Exception
	 */
	public JSONObject doGet(String uri, String wikiHeading, String wikiDetails)
			throws Exception {
		log.info("");
		log.info("");
		log.info(wikiHeading);
		log.info(wikiDetails);
		log.info("");
		if (null == uri) {
			log.info("TODO add an example here");
			return null;
		}

		URL requestUrl = new URL(serviceEndpoint + uri);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);

		String curl = "curl -i ";
		for (Entry<String, String> header : requestHeaders.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}
		curl += " '" + requestUrl + "'{code}";
		log.info("*Request*{code}" + curl);
		log.info("*Response*{code}");

		String response = null;
		try {
			response = HttpClientHelper.performRequest(requestUrl, "GET", null,
					requestHeaders);
			JSONObject results = new JSONObject(response);
			log.info(results.toString(JSON_INDENT) + "{code}");
			return results;
		} catch (Exception e) {
			log.info("failure: ", e);
			log.info("{code}");
		}
		return null;
	}

	/**
	 * @param uri
	 * @param entity
	 * @param wikiHeading
	 * @param wikiDetails
	 * @return the updated entity
	 * @throws Exception
	 */
	public JSONObject doPost(String uri, JSONObject entity, String wikiHeading,
			String wikiDetails) throws Exception {
		log.info("");
		log.info("");
		log.info(wikiHeading);
		log.info(wikiDetails);
		log.info("");
		if (null == uri) {
			log.info("TODO add an example here");
			return null;
		}

		URL requestUrl = new URL(serviceEndpoint + uri);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);

		String curl = "curl -i ";
		for (Entry<String, String> header : requestHeaders.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}
		curl += " -d '" + entity.toString(JSON_INDENT) + "' " + requestUrl
				+ "{code}";
		log.info("*Request*{code}" + curl);
		log.info("*Response*{code}");

		String response = null;
		try {
			response = HttpClientHelper.performRequest(requestUrl, "POST",
					entity.toString(), requestHeaders);
			JSONObject results = new JSONObject(response);
			log.info(results.toString(JSON_INDENT) + "{code}");
			return results;
		} catch (Exception e) {
			log.info("failure: ", e);
			log.info("{code}");
		}
		return null;
	}

	/**
	 * @param uri
	 * @param entity
	 * @param wikiHeading
	 * @param wikiDetails
	 * @return the updated entity
	 * @throws Exception
	 */
	public JSONObject doPut(String uri, JSONObject entity, String wikiHeading,
			String wikiDetails) throws Exception {
		log.info("");
		log.info("");
		log.info(wikiHeading);
		log.info(wikiDetails);
		log.info("");
		if (null == uri) {
			log.info("TODO add an example here");
			return null;
		}

		URL requestUrl = new URL(serviceEndpoint + uri);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);
		requestHeaders.put("ETag", entity.getString("etag"));

		String curl = "curl -i ";
		for (Entry<String, String> header : requestHeaders.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}
		curl += " -X PUT -d '" + entity.toString(JSON_INDENT) + "' "
				+ requestUrl + "{code}";
		log.info("*Request*{code}" + curl);
		log.info("*Response*{code}");

		String response = null;
		try {
			response = HttpClientHelper.performRequest(requestUrl, "PUT",
					entity.toString(), requestHeaders);
			JSONObject results = new JSONObject(response);
			log.info(results.toString(JSON_INDENT) + "{code}");
			return results;
		} catch (Exception e) {
			log.info("failure: ", e);
			log.info("{code}");
		}
		return null;
	}

	/**
	 * @param uri
	 * @param wikiHeading
	 * @param wikiDetails
	 * @throws Exception
	 */
	public void doDelete(String uri, String wikiHeading, String wikiDetails)
			throws Exception {
		log.info("");
		log.info("");
		log.info(wikiHeading);
		log.info(wikiDetails);
		log.info("");
		if (null == uri) {
			log.info("TODO add an example here");
			return;
		}
		URL requestUrl = new URL(serviceEndpoint + uri);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);
		String curl = "curl -i ";
		for (Entry<String, String> header : requestHeaders.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}
		curl += " -X DELETE " + requestUrl + "{code}";
		log.info("*Request*{code}" + curl);
		log.info("*Response*{code}");

		try {
			HttpClientHelper.performRequest(requestUrl, "DELETE", null,
					requestHeaders);
			log.info("{code}");
			return;
		} catch (Exception e) {
			log.info("failure: ", e);
			log.info("{code}");
		}
		return;
	}

}
