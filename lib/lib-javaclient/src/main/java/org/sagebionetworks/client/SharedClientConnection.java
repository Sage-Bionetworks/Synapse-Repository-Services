package org.sagebionetworks.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

import com.google.common.collect.Maps;

/**
 * Low-level Java Client API for Synapse REST APIs
 */
public class SharedClientConnection {

	public static final String USER_AGENT = "User-Agent";

	private static final Logger log = LogManager.getLogger(SharedClientConnection.class.getName());

	private static final int JSON_INDENT = 2;
	protected static final String DEFAULT_AUTH_ENDPOINT = "https://repo-prod.prod.sagebase.org/auth/v1";
	private static final String SESSION_TOKEN_HEADER = "sessionToken";
	private static final String REQUEST_PROFILE_DATA = "profile_request";
	private static final String PROFILE_RESPONSE_OBJECT_HEADER = "profile_response_object";

	protected String authEndpoint;

	protected Map<String, String> defaultGETDELETEHeaders;
	protected Map<String, String> defaultPOSTPUTHeaders;

	private JSONObject profileData;
	private boolean requestProfile;
	private HttpClientProvider clientProvider;

	private String userName;
	private String apiKey;

	/**
	 * Default constructor uses the default repository and auth services
	 * endpoints.
	 */
	public SharedClientConnection() {
		// Use the default implementations
		this(new HttpClientProviderImpl());
	}

	/**
	 * Will use the provided client provider and data uploader.
	 * 
	 * @param clientProvider 
	 * @param dataUploader 
	 */
	public SharedClientConnection(HttpClientProvider clientProvider) {
		if (clientProvider == null)
			throw new IllegalArgumentException("HttpClientProvider cannot be null");

		setAuthEndpoint(DEFAULT_AUTH_ENDPOINT);

		defaultGETDELETEHeaders = new HashMap<String, String>();
		defaultGETDELETEHeaders.put("Accept", "application/json");

		defaultPOSTPUTHeaders = new HashMap<String, String>();
		defaultPOSTPUTHeaders.putAll(defaultGETDELETEHeaders);
		defaultPOSTPUTHeaders.put("Content-Type", "application/json");
		
		this.clientProvider = clientProvider;
		clientProvider.setGlobalConnectionTimeout(ServiceConstants.DEFAULT_CONNECT_TIMEOUT_MSEC);
		clientProvider.setGlobalSocketTimeout(ServiceConstants.DEFAULT_SOCKET_TIMEOUT_MSEC);
		
		requestProfile = false;
	}
	
	/**
	 * Use this method to override the default implementation of {@link HttpClientProvider}
	 * @param clientProvider
	 */
	public void setHttpClientProvider(HttpClientProvider clientProvider) {
		this.clientProvider = clientProvider;
	}
	
	/**
	 * @param authEndpoint
	 *            the authEndpoint to set
	 */
	public void setAuthEndpoint(String authEndpoint) {
		this.authEndpoint = authEndpoint;
	}

	/**
	 * @param request
	 */
	public void setRequestProfile(boolean request) {
		this.requestProfile = request;
	}

	/**
	 * @return JSONObject
	 */
	public JSONObject getProfileData() {
		return this.profileData;
	}
	

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the apiKey
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * @param apiKey the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Log into Synapse
	 * 
	 * @return A session token
	 */
	public Session login(String username, String password, String userAgent) 
			throws SynapseException {
		return login(username, password, userAgent, null);
	}
	
	public Session login(String username, String password, String userAgent, Map<String,String> parameters) throws SynapseException {
		LoginCredentials loginRequest = new LoginCredentials();
		loginRequest.setEmail(username);
		loginRequest.setPassword(password);
		
		Session session;
		try {
			JSONObject obj = createAuthEntity("/session", EntityFactory.createJSONObjectForEntity(loginRequest), userAgent, parameters);
			session = EntityFactory.createEntityFromJSONObject(obj, Session.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
		
		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, session.getSessionToken());
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, session.getSessionToken());
		
		return session;		
	}

	public void logout(String userAgent) throws SynapseException {
		deleteUri(authEndpoint, "/session", userAgent);
		defaultGETDELETEHeaders.remove(SESSION_TOKEN_HEADER);
		defaultPOSTPUTHeaders.remove(SESSION_TOKEN_HEADER);
	}
	
	public boolean revalidateSession(String userAgent, Map<String,String> parameters) throws SynapseException {
		Session session = new Session();
		session.setSessionToken(getCurrentSessionToken());
		try {
			putAuthEntity("/session", EntityFactory.createJSONObjectForEntity(session), userAgent);
		} catch (SynapseForbiddenException e) {
			throw new SynapseTermsOfUseException(e.getMessage());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		}
		return true;
	}
	
	/**
	 * Authenticate the synapse client with an existing session token
	 * 
	 * @param sessionToken
	 */
	public void setSessionToken(String sessionToken) {
		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
	}

	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	public String getCurrentSessionToken() {
		return defaultPOSTPUTHeaders.get(SESSION_TOKEN_HEADER);
	}

	public void invalidateApiKey(String userAgent) throws SynapseException {
		deleteUri(authEndpoint, "/secretKey", userAgent);
		this.apiKey = null;
	}

	/**
	 * Put the contents of the passed file to the passed URL.
	 * 
	 * @param url
	 * @param file
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public String putFileToURL(URL url, File file, String contentType) throws SynapseException {
		try {
			if (url == null)
				throw new IllegalArgumentException("URL cannot be null");
			if (file == null)
				throw new IllegalArgumentException("File cannot be null");
			HttpPut httppost = new HttpPut(url.toString());
			// There must not be any headers added or Amazon will return a 403.
			// Therefore, we must clear the content type.
			@SuppressWarnings("deprecation")
			org.apache.http.entity.FileEntity fe = new org.apache.http.entity.FileEntity(file, contentType);
			httppost.setEntity(fe);
			HttpResponse response = clientProvider.execute(httppost);
			int code = response.getStatusLine().getStatusCode();
			if (code < 200 || code > 299) {
				throw new SynapseException("Response code: " + code + " " + response.getStatusLine().getReasonPhrase()
						+ " for " + url + " File: " + file.getName());
			}
			return EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		}

	}
	
	/**
	 * Put the contents of the passed byte array to the passed URL, associating the given content type
	 * 
	 * @param url
	 * @param content the byte array to upload
	 * @throws SynapseException
	 */
	public String putBytesToURL(URL url, byte[] content, String contentType) throws SynapseException {
		try {
			if (url == null)
				throw new IllegalArgumentException("URL cannot be null");
			if (content == null)
				throw new IllegalArgumentException("content cannot be null");
			HttpPut httppost = new HttpPut(url.toString());
			ByteArrayEntity se = new ByteArrayEntity(content);
			httppost.setHeader("content-type", contentType);
			httppost.setEntity(se);
			HttpResponse response = clientProvider.execute(httppost);
			int code = response.getStatusLine().getStatusCode();
			if (code < 200 || code > 299) {
				throw new SynapseException("Response code: " + code + " " + response.getStatusLine().getReasonPhrase()
						+ " for " + url);
			}
			return EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		}

	}
	
	/**
	 * Asymmetrical post where the request and response are not of the same type.
	 * 
	 * @param url
	 * @param userAgent
	 * @param reqeust
	 * @param calls
	 * @throws SynapseException
	 */
	public String asymmetricalPost(String url, String requestBody, String userAgent)
			throws SynapseException {
		HttpPost post;
		try {
			post = createPost(url, requestBody, userAgent);
			HttpResponse response = clientProvider.execute(post);
			int code = response.getStatusLine().getStatusCode();
			String responseBody = (null != response.getEntity()) ? EntityUtils.toString(response.getEntity()) : null;
			if(code < 200 || code > 299){
				throw new SynapseException("Response code: "+code+" "+response.getStatusLine().getReasonPhrase()+" for "+url+" body: "+requestBody);
			}
			return responseBody;
		} catch (UnsupportedEncodingException e) {
			throw new SynapseException(e);
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		}
	}
	
	/**
	 * Helper to create a post from an object.
	 * 
	 * @param url
	 * @param body
	 * @param userAgent
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private HttpPost createPost(String url, String body, String userAgent) throws SynapseException {
		try {
			HttpPost post = new HttpPost(url);
			setHeaders(post, defaultPOSTPUTHeaders, userAgent);
			StringEntity stringEntity = new StringEntity(body);
			post.setEntity(stringEntity);
			return post;
		} catch (UnsupportedEncodingException e) {
			throw new SynapseException(e);
		}
	}

	/**
	 * Download the file at the given URL.
	 * 
	 * @param uri
	 * @param userAgent
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws SynapseException 
	 */
	public String downloadZippedFileString(String endpoint, String uri, String userAgent) throws ClientProtocolException, IOException,
			FileNotFoundException, SynapseException {
		HttpGet get = new HttpGet(endpoint+uri);
		setHeaders(get, defaultGETDELETEHeaders, userAgent);
		// Add the header that sets the content type and the boundary
		HttpResponse response = clientProvider.execute(get);
		HttpEntity entity = response.getEntity();
		int statusCode = response.getStatusLine().getStatusCode();
		if(statusCode != 200) {
			String message = EntityUtils.toString(entity);
			throw new SynapseException("Status code: " + statusCode + ", " + message);
		}
		return FileUtils.readCompressedStreamAsString(entity.getContent());
	}

	@Deprecated
	public File downloadFromSynapse(String path, String md5,
				File destinationFile) throws SynapseException {
		try {
			clientProvider.downloadFile(path, destinationFile.getAbsolutePath());
			// Check that the md5s match, if applicable
			if (null != md5) {
				String localMd5 = MD5ChecksumHelper
						.getMD5Checksum(destinationFile.getAbsolutePath());
				if (!localMd5.equals(md5)) {
					throw new SynapseUserException(
							"md5 of downloaded file does not match the one in Synapse"
									+ destinationFile);
				}
			}

			return destinationFile;
		} catch (ClientProtocolException e) {
			throw new SynapseException(e);
		} catch (IOException e) {
			throw new SynapseException(e);
		} catch (HttpClientHelperException e) {
			throw new SynapseException(e);
		}
	}

	/******************** Mid Level Authorization Service APIs ********************/

	/**
	 * Create a new login, etc ...
	 * @param userAgent 
	 * 
	 * @return the newly created entity
	 */
	private JSONObject createAuthEntity(String uri, JSONObject entity, String userAgent, Map<String,String> parameters) throws SynapseException {
		return postJson(authEndpoint, uri, entity.toString(), userAgent, parameters);
	}

	private JSONObject putAuthEntity(String uri, JSONObject entity, String userAgent)
			throws SynapseException {
		if (null == authEndpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}

		return putJson(authEndpoint, uri, entity.toString(), userAgent);
	}

	public String getDataDirect(String endpoint, String uri) throws SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		try {
			HttpResponse response = clientProvider.performRequest(endpoint + uri, "GET", null, null);
			return EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			throw new SynapseException(e);
		} catch (HttpClientHelperException e) {
			throw new SynapseException(e);
		}
	}

	public String postStringDirect(String endpoint, String uri, String data, String userAgent) throws SynapseException {
		try {
			HttpPost post = new HttpPost(endpoint + uri);
			setHeaders(post, defaultPOSTPUTHeaders, userAgent);
			StringEntity stringEntity = new StringEntity(data);
			post.setEntity(stringEntity);
			HttpResponse response = clientProvider.execute(post);
			return (null != response.getEntity()) ? EntityUtils.toString(response.getEntity()) : null;
		} catch (IOException e) {
			throw new SynapseException(e);
		}
	}

	/******************** Low Level APIs ********************/

	/**
	 * Create any JSONEntity
	 * @param endpoint
	 * @param uri
	 * @param userAgent 
	 * @param entity
	 * @return
	 * @throws SynapseException 
	 */
	public JSONObject postJson(String endpoint, String uri, String jsonString, String userAgent) throws SynapseException {
		return postJson(endpoint, uri, jsonString, userAgent, null);
	}
	
	/**
	 * Create any JSONEntity
	 * @param endpoint
	 * @param uri
	 * @param userAgent 
	 * @param entity
	 * @param originClient
	 * @return
	 * @throws SynapseException 
	 */
	public JSONObject postJson(String endpoint, String uri, String jsonString, String userAgent, Map<String,String> parameters) throws SynapseException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == parameters) {
			parameters = Collections.emptyMap();
		}
		JSONObject jsonObject = signAndDispatchSynapseRequest(endpoint, uri, "POST", jsonString, defaultPOSTPUTHeaders,
				userAgent, parameters);
		return jsonObject;
	}

	/**
	 * Update any JSONEntity
	 * @param endpoint
	 * @param uri
	 * @param userAgent 
	 * @param entity
	 * @return
	 * @throws SynapseException
	 */
	public JSONObject putJson(String endpoint, String uri, String jsonToPut, String userAgent) throws SynapseException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		JSONObject jsonObject = signAndDispatchSynapseRequest(endpoint, uri, "PUT", jsonToPut, defaultPOSTPUTHeaders, userAgent);
		return jsonObject;
	}
	
	/**
	 * Get a JSONEntity
	 * @param userAgent 
	 */
	protected JSONObject getJson(String endpoint, String uri, String userAgent) throws SynapseException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		JSONObject jsonObject = signAndDispatchSynapseRequest(endpoint, uri, "GET", null, defaultGETDELETEHeaders, userAgent);
		return jsonObject;
	}

	/**
	 * Call Create on any URI
	 * @param userAgent 
	 */
	public JSONObject postUri(String endpoint, String uri, String userAgent) throws SynapseException {
		if (null == uri) throw new IllegalArgumentException("must provide uri");		
		return signAndDispatchSynapseRequest(endpoint, uri, "POST", null, defaultPOSTPUTHeaders, userAgent);
	}

	/**
	 * Call Delete on any URI
	 * @param userAgent 
	 */
	public void deleteUri(String endpoint, String uri, String userAgent) throws SynapseException {
		if (null == uri) throw new IllegalArgumentException("must provide uri");		
		signAndDispatchSynapseRequest(endpoint, uri, "DELETE", null, defaultGETDELETEHeaders, userAgent);
	}

	protected JSONObject signAndDispatchSynapseRequest(String endpoint, String uri, String requestMethod,
			String requestContent, Map<String, String> requestHeaders, String userAgent) throws SynapseException {
		Map<String, String> parameters = Maps.newHashMap();
		parameters.put(AuthorizationConstants.DOMAIN_PARAM, DomainType.SYNAPSE.toString());
		return signAndDispatchSynapseRequest(endpoint, uri, requestMethod, requestContent, requestHeaders, userAgent,
				parameters);
	}
	
	protected JSONObject signAndDispatchSynapseRequest(String endpoint, String uri, String requestMethod,
			String requestContent, Map<String, String> requestHeaders, String userAgent, Map<String,String> parameters)
			throws SynapseException {
		Map<String, String> modHeaders = new HashMap<String, String>(requestHeaders);
		modHeaders.put(USER_AGENT, userAgent);
		
		if (apiKey!=null) {
			String timeStamp = (new DateTime()).toString();
			String uriRawPath = null; 
			try {
				uriRawPath = (new URI(endpoint+uri)).getRawPath(); // chop off the query, if any
			} catch (URISyntaxException e) {
				throw new SynapseException(e);
			}
		    String signature = HMACUtils.generateHMACSHA1Signature(userName, uriRawPath, timeStamp, apiKey);
		    modHeaders.put(AuthorizationConstants.USER_ID_HEADER, userName);
		    modHeaders.put(AuthorizationConstants.SIGNATURE_TIMESTAMP, timeStamp);
		    modHeaders.put(AuthorizationConstants.SIGNATURE, signature);
		    return dispatchSynapseRequest(endpoint, uri, requestMethod, requestContent, modHeaders, parameters);
		} 
		return dispatchSynapseRequest(endpoint, uri, requestMethod, requestContent, modHeaders, parameters);
	}

	protected String createRequestUrl(String endpoint, String uri, Map<String,String> parameters) throws SynapseServiceException {
		// At least one test calls the dispatch method directly, so verify again that the origin client has been set
		URL requestUrl = null;
		URIBuilder builder = new URIBuilder();
		try {
			URL parsedEndpoint = new URL(endpoint);
			String endpointPrefix = parsedEndpoint.getPath();
			String endpointLocation = endpoint.substring(0, endpoint.length() - endpointPrefix.length());

			requestUrl = (uri.startsWith(endpointPrefix)) ? new URL(endpointLocation + uri) : new URL(endpoint + uri);
			
			builder = new URIBuilder(requestUrl.toURI());
			for (Map.Entry<String,String> entry : parameters.entrySet()) {
				builder.addParameter(entry.getKey(), entry.getValue());
			}
		} catch(MalformedURLException mue) {
			throw new SynapseServiceException("Invalid URI: <<"+builder.toString()+">>", mue);
		} catch(URISyntaxException use) {
			throw new SynapseServiceException("Invalid URI: <<"+builder.toString()+">>", use);
		}
		return builder.toString();
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
	 */
	protected JSONObject dispatchSynapseRequest(String endpoint, String uri, String requestMethod,
			String requestContent, Map<String, String> requestHeaders, Map<String,String> parameters)
			throws SynapseException {
		if (parameters == null) {
			parameters = Collections.emptyMap();
		}
		if (requestProfile && !requestMethod.equals("DELETE")) {
			requestHeaders.put(REQUEST_PROFILE_DATA, "true");
		} else {
			if (requestHeaders.containsKey(REQUEST_PROFILE_DATA))
				requestHeaders.remove(REQUEST_PROFILE_DATA);
		}
		
		// remove session token if it is null
		if(requestHeaders.containsKey(SESSION_TOKEN_HEADER) && requestHeaders.get(SESSION_TOKEN_HEADER) == null) {
			requestHeaders.remove(SESSION_TOKEN_HEADER);
		}
		
		JSONObject results = null;
		String requestUrl = null;
		try {
			requestUrl = createRequestUrl(endpoint, uri, parameters);
			
			HttpResponse response = clientProvider.performRequest(requestUrl, requestMethod, requestContent,
					requestHeaders);

			if (requestProfile && !requestMethod.equals("DELETE")) {
				Header header = response
						.getFirstHeader(PROFILE_RESPONSE_OBJECT_HEADER);
				String encoded = header.getValue();
				String decoded = new String(Base64.decodeBase64(encoded
						.getBytes("UTF-8")), "UTF-8");
				profileData = new JSONObject(decoded);
			} else {
				profileData = null;
			}

			String responseBody = (null != response.getEntity()) ? EntityUtils
					.toString(response.getEntity()) : null;
			if (null != responseBody && responseBody.length()>0) {
				try {
					results = new JSONObject(responseBody);
				} catch (JSONException jsone) {
					throw new SynapseServiceException("responseBody: <<"+responseBody+">>", jsone);
				}
				if (log.isDebugEnabled()) {
					if(authEndpoint.equals(endpoint)) {
						log.debug(requestMethod + " " + requestUrl + " : (not logging auth request details)");
					}
					else {
						log.debug(requestMethod + " " + requestUrl + " : "
								+ results.toString(JSON_INDENT));
					}
				}
			}

		} catch (HttpClientHelperException e) {
			// Well-handled server side exceptions come back as JSON, attempt to
			// deserialize and convert the error
			int statusCode = 500; // assume a service exception
			statusCode = e.getHttpStatus();
			String response = "";
			String resultsStr = "";
			try {
				response = e.getResponse();
				if (null != response && response.length()>0) {
					try {
						results = new JSONObject(response);
					} catch (JSONException jsone) {
						throw new SynapseServiceException("Failed to parse: "+response, jsone);
					}
					if (log.isDebugEnabled()) {
						log.debug("Retrieved " + requestUrl + " : "
								+ results.toString(JSON_INDENT));
					}
					if (results != null)
						resultsStr = results.getString("reason");
				}
				String exceptionContent = "Service Error(" + statusCode + "): "
						+ resultsStr + " " + e.getMessage();

				if (statusCode == 401) {
					throw new SynapseUnauthorizedException(exceptionContent);
				} else if (statusCode == 403) {
					throw new SynapseForbiddenException(exceptionContent);
				} else if (statusCode == 404) {
					throw new SynapseNotFoundException(exceptionContent);
				} else if (statusCode == 400) {
					throw new SynapseBadRequestException(exceptionContent);
				} else if (statusCode >= 400 && statusCode < 500) {
					throw new SynapseUserException(exceptionContent);
				} else {
					throw new SynapseServiceException("request content: "+requestContent+" exception content: "+exceptionContent+" status code: "+statusCode);
				}
			} catch (JSONException jsonEx) {
				// swallow the JSONException since its not the real problem and
				// return the response as-is since it is not JSON
				throw new SynapseServiceException(jsonEx);
			} catch (ParseException parseEx) {
				throw new SynapseServiceException(parseEx);
			}
		} // end catch
		catch (MalformedURLException e) {
			throw new SynapseServiceException(e);
		} catch (ClientProtocolException e) {
			throw new SynapseServiceException(e);
		} catch (IOException e) {
			throw new SynapseServiceException(e);
		} catch (JSONException e) {
			throw new SynapseServiceException(e);
		}

		return results;
	}

	public String getDirect(String endpoint, String uri, String userAgent) throws ClientProtocolException, IOException {
		HttpGet get = new HttpGet(endpoint + uri);
		setHeaders(get, defaultGETDELETEHeaders, userAgent);
		HttpResponse response = clientProvider.execute(get);
		String responseBody = (null != response.getEntity()) ? EntityUtils.toString(response.getEntity()) : null;
		return responseBody;
	}

	private void setHeaders(HttpRequestBase request, Map<String, String> headers, String userAgent) {
		for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
			request.setHeader(headerEntry.getKey(), headerEntry.getValue());
		}
		request.setHeader(USER_AGENT, userAgent);
	}
}

