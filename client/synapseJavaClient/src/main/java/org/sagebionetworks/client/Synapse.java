package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

/**
 * Low-level Java Client API for Synapse REST APIs
 */
public class Synapse {

	private static final Logger log = Logger.getLogger(Synapse.class.getName());
	
	private static final int DEFAULT_TIMEOUT_MSEC = 5000;

	private static final int JSON_INDENT = 2;
	private static final String ANNOTATION_URI_SUFFIX = "annotations";
	private static final String DEFAULT_REPO_ENDPOINT = "https://staging-reposervice.elasticbeanstalk.com/repo/v1";
	private static final String DEFAULT_AUTH_ENDPOINT = "https://staging-reposervice.elasticbeanstalk.com/auth/v1";
	private static final String SESSION_TOKEN_HEADER = "sessionToken";
        private static final String REQUEST_PROFILE_DATA = "profile_request";
        private static final String PROFILE_RESPONSE_OBJECT_HEADER = "profile_response_object";
	private static final String QUERY_URI = "/query?query=";

	private String repoEndpoint;
	private String authEndpoint;

	private Map<String, String> defaultGETDELETEHeaders;
	private Map<String, String> defaultPOSTPUTHeaders;
        
        private JSONObject profileData;
        private boolean requestProfile;

	/**
	 * Default constructor uses the default repository and auth services
	 * endpoints.
	 */
	public Synapse() {
                
		setRepositoryEndpoint(DEFAULT_REPO_ENDPOINT);
		setAuthEndpoint(DEFAULT_AUTH_ENDPOINT);

		defaultGETDELETEHeaders = new HashMap<String, String>();
		defaultGETDELETEHeaders.put("Accept", "application/json");

		defaultPOSTPUTHeaders = new HashMap<String, String>();
		defaultPOSTPUTHeaders.putAll(defaultGETDELETEHeaders);
		defaultPOSTPUTHeaders.put("Content-Type", "application/json");
		
		HttpClientHelper.setConnectionTimeout(DEFAULT_TIMEOUT_MSEC);
		HttpClientHelper.setSocketTimeout(DEFAULT_TIMEOUT_MSEC);
                
                requestProfile = false;
                		
	}

	/**
	 * @param repoEndpoint
	 *            the repoEndpoint to set
	 */
	public void setRepositoryEndpoint(String repoEndpoint) {
		this.repoEndpoint = repoEndpoint;
	}

	/**
	 * @param authEndpoint
	 *            the authEndpoint to set
	 */
	public void setAuthEndpoint(String authEndpoint) {
		this.authEndpoint = authEndpoint;
	}
        
        public void setRequestprofile(boolean request) {
            this.requestProfile = request;
        }
        
        public JSONObject getProfileData() {
            return this.profileData;
        }

	/**
	 * Log into Synapse
	 * 
	 * @param username
	 * @param password
	 * @throws JSONException
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws IOException
	 * @throws HttpException
	 */
	public void login(String username, String password) throws JSONException,
			HttpException, IOException, SynapseUserException,
			SynapseServiceException {
		JSONObject loginRequest = new JSONObject();
		loginRequest.put("email", username);
		loginRequest.put("password", password);
                
                boolean reqPr = requestProfile;
                requestProfile = false;

		JSONObject credentials = createAuthEntity("/session", loginRequest);

		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, credentials
				.getString(SESSION_TOKEN_HEADER));
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, credentials
				.getString(SESSION_TOKEN_HEADER));
                
                requestProfile = reqPr;
	}
	
	/**
	 * Get the current session token used by this client.
	 * @return
	 */
	public String getCurrentSessionToken(){
		return defaultPOSTPUTHeaders.get(SESSION_TOKEN_HEADER);
	}

	/******************** Mid Level Repository Service APIs ********************/

	/**
	 * Create a new dataset, layer, etc ...
	 * 
	 * @param uri
	 * @param entity
	 * @return the newly created entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public JSONObject createEntity(String uri, JSONObject entity)
			throws HttpException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		return createSynapseEntity(repoEndpoint, uri, entity);
	}

	/**
	 * Get a dataset, layer, preview, annotations, etc...
	 * 
	 * @param uri
	 * @return the retrieved entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public JSONObject getEntity(String uri) throws HttpException, IOException,
			JSONException, SynapseUserException, SynapseServiceException {
		return getSynapseEntity(repoEndpoint, uri);
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
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	@SuppressWarnings("unchecked")
	public JSONObject updateEntity(String uri, JSONObject entity)
			throws HttpException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		return updateSynapseEntity(repoEndpoint, uri, entity);
	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public JSONObject putEntity(String uri, JSONObject entity)
			throws HttpException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		return putSynapseEntity(repoEndpoint, uri, entity);
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param uri
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public void deleteEntity(String uri) throws HttpException, IOException,
			JSONException, SynapseUserException, SynapseServiceException {
		deleteSynapseEntity(repoEndpoint, uri);
		return;
	}

	/**
	 * Perform a query
	 * 
	 * @param query
	 *            the query to perform
	 * @return the query result
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public JSONObject query(String query) throws HttpException, IOException,
			JSONException, SynapseUserException, SynapseServiceException {
		return querySynapse(repoEndpoint, query);
	}

	/**
	 * TODO I don't think this API is quite what we want, look at the R client
	 * API for examples as to how to make this more convenient
	 * 
	 * @throws IOException
	 * @throws JSONException 
	 * @throws SynapseServiceException 
	 * @throws SynapseUserException 
	 * @throws DecoderException 
	 * @throws HttpClientHelperException 
	 * @return 
	 */
	public JSONObject uploadLayerToSynapse(JSONObject layer, File dataFile)
			throws IOException, JSONException, SynapseUserException, SynapseServiceException, DecoderException, HttpClientHelperException {

		String s3Path = dataFile.getName();

		String md5 = MD5ChecksumHelper.getMD5Checksum(dataFile
				.getAbsolutePath());
		JSONObject s3LocationRequest = new JSONObject();
		s3LocationRequest.put("path", "/" + s3Path);
		s3LocationRequest.put("md5sum", md5);
		s3LocationRequest.put("parentId", layer.getString("id"));
		s3LocationRequest.put("type", "awss3");
		JSONObject s3Location = createEntity("/location", s3LocationRequest);

		// TODO find a more direct way to go from hex to base64
		byte[] encoded = Base64.encodeBase64(Hex.decodeHex(md5.toCharArray()));
		String base64Md5 = new String(encoded, "ASCII");

		Map<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("x-amz-acl", "bucket-owner-full-control");
		headerMap.put("Content-MD5", base64Md5);
		headerMap.put("Content-Type", s3Location.getString("contentType"));

		HttpClientHelper.uploadFile(s3Location.getString("path"), dataFile
				.getAbsolutePath(), headerMap);
		
		return getEntity(s3Location.getString("uri"));
	}

	/******************** Mid Level Authorization Service APIs ********************/

	/**
	 * Create a new login, etc ...
	 * 
	 * @param uri
	 * @param entity
	 * @return the newly created entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public JSONObject createAuthEntity(String uri, JSONObject entity)
			throws HttpException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		return createSynapseEntity(authEndpoint, uri, entity);
	}

	/******************** Low Level APIs ********************/

	/**
	 * Create a new dataset, layer, etc ...
	 * 
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return the newly created entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public JSONObject createSynapseEntity(String endpoint, String uri,
			JSONObject entity) throws HttpException, IOException,
			JSONException, SynapseUserException, SynapseServiceException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}
                
		return dispatchSynapseRequest(endpoint, uri, "POST", entity.toString(),
				defaultPOSTPUTHeaders);
	}

	/**
	 * Get a dataset, layer, preview, annotations, etc...
	 * 
	 * @param endpoint
	 * @param uri
	 * @return the retrieved entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public JSONObject getSynapseEntity(String endpoint, String uri)
			throws HttpException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}

		return dispatchSynapseRequest(endpoint, uri, "GET", null,
				defaultGETDELETEHeaders);
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
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	@SuppressWarnings("unchecked")
	public JSONObject updateSynapseEntity(String endpoint, String uri,
			JSONObject entity) throws HttpException, IOException,
			JSONException, SynapseUserException, SynapseServiceException {

		JSONObject storedEntity = getSynapseEntity(endpoint, uri);

		boolean isAnnotation = uri.endsWith(ANNOTATION_URI_SUFFIX);

		Iterator<String> keyIter = entity.keys();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			if (isAnnotation) {
				// Annotations need to go one level deeper
				JSONObject storedAnnotations = storedEntity.getJSONObject(key);
				JSONObject entityAnnotations = entity.getJSONObject(key);
				Iterator<String> annotationIter = entity.getJSONObject(key)
						.keys();
				while (annotationIter.hasNext()) {
					String annotationKey = annotationIter.next();
					storedAnnotations.put(annotationKey, entityAnnotations
							.get(annotationKey));
				}
			} else {
				storedEntity.put(key, entity.get(key));
			}
		}

		return putSynapseEntity(endpoint, uri, storedEntity);

	}

	/**
	 * Update a dataset, layer, preview, annotations, etc...
	 * 
	 * @param endpoint
	 * @param uri
	 * @param entity
	 * @return the updated entity
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public JSONObject putSynapseEntity(String endpoint, String uri,
			JSONObject entity) throws HttpException, IOException,
			JSONException, SynapseUserException, SynapseServiceException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);
		requestHeaders.put("ETag", entity.getString("etag"));

		return dispatchSynapseRequest(endpoint, uri, "PUT", entity.toString(),
				requestHeaders);
	}

	/**
	 * Delete a dataset, layer, etc..
	 * 
	 * @param endpoint
	 * @param uri
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public void deleteSynapseEntity(String endpoint, String uri)
			throws HttpException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}

		dispatchSynapseRequest(endpoint, uri, "DELETE", null,
				defaultGETDELETEHeaders);
		return;
	}

	/**
	 * Perform a query
	 * 
	 * @param endpoint
	 * @param query
	 *            the query to perform
	 * @return the query result
	 * @throws SynapseServiceException
	 * @throws SynapseUserException
	 * @throws JSONException
	 * @throws IOException
	 * @throws HttpException
	 */
	public JSONObject querySynapse(String endpoint, String query)
			throws HttpException, IOException, JSONException,
			SynapseUserException, SynapseServiceException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == query) {
			throw new IllegalArgumentException("must provide a query");
		}

		String queryUri = QUERY_URI + URLEncoder.encode(query, "UTF-8");

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);

		return dispatchSynapseRequest(endpoint, queryUri, "GET", null,
				requestHeaders);
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
	 * @throws IOException
	 * @throws HttpException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	private JSONObject dispatchSynapseRequest(String endpoint,
			String uri, String requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws HttpException,
			IOException, JSONException, SynapseUserException,
			SynapseServiceException {
                
                if (requestProfile && ! requestMethod.equals("DELETE")) {
                    requestHeaders.put(REQUEST_PROFILE_DATA, "true");
                } else {
                    if (requestHeaders.containsKey(REQUEST_PROFILE_DATA))
                        requestHeaders.remove(REQUEST_PROFILE_DATA);
                }

		URL parsedEndpoint = new URL(endpoint);
		String endpointPrefix = parsedEndpoint.getPath();
		String endpointLocation = endpoint.substring(0, endpoint.length()
				- endpointPrefix.length());

		URL requestUrl = (uri.startsWith(endpointPrefix)) ? new URL(
				endpointLocation + uri) : new URL(endpoint + uri);

		JSONObject results = null;
		try {
                        HttpMethodBase method = HttpClientHelper.performRequest(requestUrl,
					requestMethod, requestContent, requestHeaders);
                        // TODO: Change hardcoded value from HttpClientHelper
                        String response = method.getResponseBodyAsString(1024*1024);
                        
                        if (requestProfile && ! requestMethod.equals("DELETE")) {
                            Header header = method.getResponseHeader(PROFILE_RESPONSE_OBJECT_HEADER);
                            String encoded = header.getValue();
                            String decoded = new String(Base64.decodeBase64(encoded.getBytes("UTF-8")), "UTF-8");
                            profileData = new JSONObject(decoded);
                        } else {
                            profileData = null;
                        }
                        
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
			statusCode = e.getMethod().getStatusCode();
			String response = e.getMethod().getResponseBodyAsString();
			try {
				results = new JSONObject(response);
				if (log.isDebugEnabled()) {
					log.debug("Retrieved " + requestUrl + " : "
							+ results.toString(JSON_INDENT));
				}

				if ((400 <= statusCode) && (500 > statusCode)) {
					throw new SynapseUserException("User Error(" + statusCode
							+ "): " + results.getString("reason"));
				}
				throw new SynapseServiceException("Service Error(" + statusCode
						+ "): " + results.getString("reason"));
			} catch (JSONException jsonEx) {
				// swallow the JSONException since its not the real problem and
				// return the response as-is since it is not JSON
				throw new SynapseServiceException("Service Error(" + statusCode
						+ "): " + response);
			}
		} // end catch

		return results;
	}
}
